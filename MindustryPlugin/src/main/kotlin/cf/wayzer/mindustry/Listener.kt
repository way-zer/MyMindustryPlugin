package cf.wayzer.mindustry

import arc.Core
import arc.Events
import arc.util.Time
import cf.wayzer.i18n.I18nApi.i18n
import cf.wayzer.mindustry.Data.playerData
import cf.wayzer.mindustry.I18nHelper.sendMessage
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.gen.Call
import mindustry.net.Packets
import mindustry.net.ValidateException
import mindustry.world.blocks.power.NuclearReactor
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
            val winnerMsg: Any = if (Vars.state.rules.pvp) "[YELLOW] {team.colorizeName} 队胜利![]".i18n("_team" to e.winner) else ""
            val msg = """
                | [SCARLET]游戏结束![]"
                | {winnerMsg}
                | 下一张地图为:[accent]{map.name}[] By: [accent]{map.author}[]
                | 下一场游戏将在 {waitTime} 秒后开始
            """.trimMargin().i18n("winnerMsg" to winnerMsg, "waitTime" to Config.base.waitingTime.seconds)
            Helper.broadcast(msg, I18nHelper.MsgType.InfoToast, quite = true)
            Helper.broadcast(msg, I18nHelper.MsgType.Message, quite = true)
            Helper.logToConsole("Next map is ${map.name()}")
            Main.timer.schedule(Config.base.waitingTime.toMillis()) {
                Helper.loadMap(map)
            }
            //TODO 结算经验 And 排行榜系统
            RuntimeData.calTime()
            RuntimeData.gameTime.filter { it.value < 5000 }.forEach {
                RuntimeData.gameTime.remove(it.key) //Remove less than 5 second
            }
            if (Vars.state.rules.pvp) {
                //Remove loss Team
                RuntimeData.gameTime.keys.filter { e.winner != RuntimeData.teams[it] }.forEach {
                    RuntimeData.gameTime.remove(it)
                }
            }
            val all = RuntimeData.gameTime.values.sum().toDouble()
            val list = RuntimeData.gameTime.entries.sortedByDescending { it.value }.joinToString {
                val percent = String.format("%.2f", (it.value / all * 100))
                val info = playerData[it.key]!!
                "[]{player.name}[]([red]{percent}%,{gameTime} min[]),"
                        .i18n("percent" to percent, "gameTime" to it.value / 1000 / 60,
                                "_info" to info, "_lang" to info.lang)
                        .toString()
            }
            Helper.broadcast("""
                |[yellow]总贡献时长: {allTime}分钟
                |[yellow]贡献度排名(目前根据时间): {list}
            """.trimMargin().i18n("allTime" to String.format("%.2f", all / 1000 / 60), "list" to list))
        }
        //Kick player when banned
        Events.on(EventType.PlayerBanEvent::class.java) { e ->
            e.player?.info?.lastKicked = Time.millis()
            e.player?.con?.kick(Packets.KickReason.banned)
        }
        //Quit PVP mode when no player
        Events.on(EventType.PlayerLeave::class.java) { _ ->
            if (!Vars.state.rules.pvp) return@on
            Core.app.post {
                if (!Vars.playerGroup.isEmpty) return@post
                val next = Helper.nextMap()
                //Prevent only PVP server
                if (Helper.bestMode(next) == Gamemode.pvp) return@post
                Helper.loadMap(next)
            }
        }
        Events.on(ValidateException::class.java) { e ->
            Call.onWorldDataBegin(e.player.con)
            Vars.netServer.sendWorldData(e.player)
            e.player.sendMessage("[red]检验异常,自动同步".i18n())
        }
        //PVP Protect
        var t = 0
        Events.on(EventType.Trigger.update) {
            if (!RuntimeData.pvpProtect) return@on
            t = (t + 1) % 180
            if (t != 0) return@on //per 60ticks | 3 seconds
            Vars.playerGroup.forEach {
                if (it.isShooting && Vars.state.teams.closestEnemyCore(it.pointerX, it.pointerY, it.team)?.withinDst(it, Vars.state.rules.enemyCoreBuildRadius) == true) {
                    it.sendMessage("[red]PVP保护时间,禁止在其他基地攻击".i18n())
                    it.kill()
                }
            }
        }
    }

    private fun registerAboutPlayer() {
        Events.on(EventType.PlayerJoin::class.java) { e ->
            lastJoin = System.currentTimeMillis()
            e.player.sendMessage("""
                |Welcome to this Server
                |[green]欢迎{player.name}[green]来到本服务器[]
                |[yellow]本提示请到语言文件内修改
            """.trimMargin().i18n(), I18nHelper.MsgType.InfoToast, 30f)
            VoteHandler.handleJoin(e.player)
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
            RuntimeData.calTime()
            RuntimeData.beginTime.remove(e.player.uuid)
            RuntimeData.robots.remove(e.player.uuid)
        }
        Events.on(EventType.PlayerChatEvent::class.java) { e ->
            if (e.message.equals("y", true))
                Core.app.post {
                    VoteHandler.handleVote(e.player)
                }
            if (e.message.startsWith('!') || e.message.startsWith('\\'))
                Core.app.post {
                    e.player.sendMessage("[yellow]本服插件为原创,指令均为/开头,使用[red]/help[yellow]查看指令帮助".i18n())
                }
        }
    }

    private fun registerReGrief() {
        Events.on(EventType.DepositEvent::class.java) { e ->
            if (e.tile.block() == Blocks.thoriumReactor && e.tile.ent<NuclearReactor.NuclearReactorEntity>().liquids.total() < 0.05) {
                Helper.broadcast("[red][WARNING!][yellow]{player.name}正在进行危险行为{pos}!"
                        .i18n("_player" to e.player, "pos" to "(${e.tile.x},${e.tile.y})"))
                Helper.secureLog("ThoriumReactor", "${e.player.name} uses ThoriumReactor in danger|(${e.tile.x},${e.tile.y})")
            }
        }
        Events.on(EventType.UnitCreateEvent::class.java) { e ->
            if (e.unit.team == Vars.state.rules.waveTeam) return@on
            when (val count = Vars.unitGroup.count { it.team == e.unit.team }) {
                in Config.base.unitWarnRange ->
                    if (RuntimeData.Intervals.UnitWarn())
                        Vars.playerGroup.all().forEach {
                            if (it.team == e.unit.team) {
                                it.sendMessage("[yellow]警告: 建筑过多单位,可能造成服务器卡顿,当前: {count}".i18n("count" to count), I18nHelper.MsgType.InfoToast, 6f)
                            }
                        }
                in Config.base.unitWarnRange.last..10000 -> {
                    if (RuntimeData.Intervals.UnitWarn())
                        Vars.playerGroup.all().forEach {
                            if (it.team == e.unit.team) {
                                it.sendMessage("[red]警告: 建筑过多单位,可能造成服务器卡顿,已禁止生成".i18n("count" to count), I18nHelper.MsgType.InfoToast, 6f)
                            }
                        }
                    e.unit.kill()
                }
            }
        }
    }
}
