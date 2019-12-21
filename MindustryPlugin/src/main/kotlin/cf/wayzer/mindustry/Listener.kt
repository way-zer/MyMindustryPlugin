package cf.wayzer.mindustry

import cf.wayzer.mindustry.Config.waitingTimeRound
import cf.wayzer.mindustry.Data.playerData
import io.anuke.arc.Events
import io.anuke.mindustry.Vars
import io.anuke.mindustry.content.Blocks
import io.anuke.mindustry.game.EventType
import io.anuke.mindustry.gen.Call
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.util.*
import kotlin.concurrent.schedule

object Listener {
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
            Call.onInfoMessage("""
                | ${if (Vars.state.rules.pvp) "[YELLOW] ${e.winner.name} 队胜利![]" else "[SCARLET]游戏结束![]"}
                | 下一张地图为:[accent]${map.name()}[] By: [accent]${map.author()}[]
                | 下一场游戏将在 ${waitingTimeRound / 1000} 秒后开始
            """.trimMargin())
            Helper.logToConsole("Next map is ${map.name()}")
            Main.timer.schedule(waitingTimeRound) {
                Helper.loadMap(map)
            }
        }
        Events.on(EventType.PlayerBanEvent::class.java){e->
            e.player.con?.kick("[red]你已被服务器禁封")
        }
    }

    private fun registerAboutPlayer() {
        Events.on(EventType.PlayerJoin::class.java) { e ->
            e.player.sendMessage(Config.motd)
            val data = playerData[e.player.uuid] ?: let {
                Data.PlayerData(
                        e.player.uuid, "", Date(), Date(), "", 0, 0, 0
                )
            }
            playerData[e.player.uuid] = data.copy(lastName = e.player.name, lastJoin = Date(), lastAddress = e.player.con.address)
            joinTime[e.player.uuid] = System.currentTimeMillis()
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
            if (e.tile.block() == Blocks.thoriumReactor && e.tile.block().liquidPressure < 1) {
                Helper.broadcast("[red][WARNING!][yellow]${e.player.name}正在进行危险行为(${e.tile.x},${e.tile.y})!")
                Helper.secureLog("ThoriumReactor", "${e.player.name} uses ThoriumReactor in danger|(${e.tile.x},${e.tile.y})")
            }
        }
    }
}
