package cf.wayzer.mindustry

import cf.wayzer.mindustry.Config.waitingTimeRound
import cf.wayzer.mindustry.Data.playerData
import io.anuke.arc.Events
import io.anuke.mindustry.Vars
import io.anuke.mindustry.content.Blocks
import io.anuke.mindustry.game.EventType
import io.anuke.mindustry.game.Team
import io.anuke.mindustry.gen.Call
import io.anuke.mindustry.net.ValidateException
import io.anuke.mindustry.world.blocks.power.NuclearReactor
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.util.*
import kotlin.concurrent.schedule

object Listener {
    var lastJoin = Long.MAX_VALUE
        private set
    private val runtime = DBMaker.memoryDB().make()
    private val joinTime = runtime.hashMap("joinTime", Serializer.STRING_ASCII, Serializer.LONG).expireAfterGet().createOrOpen()
    private val onlineExp = runtime.hashMap("onlineExp", Serializer.STRING_ASCII, Serializer.INTEGER).expireAfterGet().createOrOpen()
    // Data for one play(map)
    object RuntimeData{
        var startTime=0L
        val beginTime = mutableMapOf<String,Long>()
        val gameTime = mutableMapOf<String,Int>()
        val teams = mutableMapOf<String, Team>()
        fun reset(){
            startTime = System.currentTimeMillis()
            beginTime.clear()
            gameTime.clear()
            teams.clear()
            // 设置所有在场玩家时间
            Vars.playerGroup.all().forEach {
                beginTime[it.uuid]=System.currentTimeMillis()
            }
        }
        fun calTime(){
            beginTime.forEach { (uuid, start) ->
                gameTime.merge(uuid,(System.currentTimeMillis()-start).toInt(),Int::plus)
                beginTime[uuid]=System.currentTimeMillis()
            }
        }
    }
    fun register() {
        registerGameControl()
        registerAboutPlayer()
        registerReGrief()
    }

    private fun registerGameControl() {
        Events.on(EventType.GameOverEvent::class.java) { e ->
            if (Vars.state.rules.pvp)
                Helper.logToConsole("&lcGame over! Team &ly${e.winner.name}&lc is victorious with &ly${Vars.playerGroup.size()}&lc players online on map &ly${Vars.world.map.name()}&lc.")
            else
                Helper.logToConsole("&lcGame over! Reached wave &ly${Vars.state.wave}&lc with &ly${Vars.playerGroup.size()}&lc players online on map &ly${Vars.world.map.name()}&lc.")

            val map = Helper.nextMap(Vars.world.map)
            Call.onInfoMessage("""
                | ${if (Vars.state.rules.pvp) "[YELLOW] ${e.winner.name} 队胜利![]" else "[SCARLET]游戏结束![]"}
                | 下一张地图为:[accent]${map.name()}[] By: [accent]${map.author()}[]
                | 下一场游戏将在 ${waitingTimeRound / 1000} 秒后开始
            """.trimMargin())
            Helper.logToConsole("Next map is ${map.name()}")
            Main.timer.schedule(waitingTimeRound) {
                Helper.loadMap(map)
            }
            //TODO 结算经验 And 排行榜系统
            RuntimeData.calTime()
            RuntimeData.gameTime.filter { it.value < 5000 }.forEach{
                RuntimeData.gameTime.remove(it.key) //Remove less than 5 second
            }
            if(Vars.state.rules.pvp){
                //Remove loss Team
                RuntimeData.gameTime.keys.filter { e.winner != RuntimeData.teams[it] }.forEach{
                    RuntimeData.gameTime.remove(it)
                }
            }
            val all = RuntimeData.gameTime.values.sum().toDouble()
            val builder = StringBuilder("[yellow]贡献度排名(目前根据时间): ")
            RuntimeData.gameTime.entries.sortedByDescending { it.value }.joinTo(builder) {
                val percent = String.format("%.2f",(it.value / all*100))
                "[]" + playerData[it.key]!!.lastName + "[]([red]$percent%[])"
            }
            Helper.broadcast(builder.toString())
        }
        Events.on(EventType.WorldLoadEvent::class.java){
            Main.timer.schedule(1000){
                RuntimeData.reset()
            }
        }
        Events.on(EventType.PlayerBanEvent::class.java){e->
            e.player?.con?.kick("[red]你已被服务器禁封")
        }
        Events.on(ValidateException::class.java){e->
            Call.onInfoMessage(e.player.con,"[red]检验异常,自动同步")
            Call.onWorldDataBegin(e.player.con)
            Vars.netServer.sendWorldData(e.player)
        }
    }

    private fun registerAboutPlayer() {
        Events.on(EventType.PlayerConnect::class.java){e->
            if(Vars.state.rules.pvp)
                e.player.team = Helper.getTeam(e.player)
        }
        Events.on(EventType.PlayerJoin::class.java) { e ->
            lastJoin = System.currentTimeMillis()
            if (Config.motd.lines().size>10)
                Call.onInfoMessage(e.player.con,Config.motd)
            else
                e.player.sendMessage(Config.motd)
            val data = playerData[e.player.uuid] ?: let {
                Data.PlayerData(
                        e.player.uuid, "", Date(), Date(), "", 0, 0, 0
                )
            }
            playerData[e.player.uuid] = data.copy(lastName = e.player.name, lastJoin = Date(), lastAddress = e.player.con.address)
            joinTime[e.player.uuid] = System.currentTimeMillis()
            RuntimeData.beginTime[e.player.uuid] = System.currentTimeMillis()
        }
        Events.on(EventType.PlayerLeave::class.java) { e ->
            var data = playerData[e.player.uuid]!!
            data = data.addPlayedTime(((System.currentTimeMillis() - joinTime[e.player.uuid]!!) / 1000).toInt())
            onlineExp[e.player.uuid]?.let { data = data.addExp(it) }
            playerData[e.player.uuid] = data
        }
        Events.on(EventType.PlayerChatEvent::class.java) { e ->
            if (e.message.equals("y", true))
                VoteHandler.handleVote(e.player)
        }
    }

    private fun registerReGrief() {
        Events.on(EventType.DepositEvent::class.java) { e ->
            if (e.tile.block() == Blocks.thoriumReactor && e.tile.ent<NuclearReactor.NuclearReactorEntity>().liquids.total() < 0.05) {
                Helper.broadcast("[red][WARNING!][yellow]${e.player.name}正在进行危险行为(${e.tile.x},${e.tile.y})!")
                Helper.secureLog("ThoriumReactor", "${e.player.name} uses ThoriumReactor in danger|(${e.tile.x},${e.tile.y})")
            }
        }
        Events.on(EventType.BlockBuildBeginEvent::class.java){e->
            if(e.tile.block()==Blocks.draugFactory){
                Vars.unitGroups.size
            }
        }
        Events.on(EventType.UnitCreateEvent::class.java){e->
            if(e.unit.team == Vars.waveTeam)return@on
            when(Vars.unitGroups[e.unit.team.ordinal].size()){
                in Config.unitToWarn until Config.unitToStop ->
                    Vars.playerGroup.all().forEach {
                        if(it.team == e.unit.team){
                            it.sendMessage("[yellow]警告: 建筑过多单位,可能造成服务器卡顿")
                        }
                    }
                in Config.unitToStop..10000 ->{
                    Vars.playerGroup.all().forEach {
                        if(it.team == e.unit.team){
                            it.sendMessage("[red]警告: 建筑过多单位,可能造成服务器卡顿,已禁止生成")
                        }
                    }
                    e.unit.kill()
                }
            }
        }
    }
}
