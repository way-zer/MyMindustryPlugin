package cf.wayzer.mindustry

import arc.Core
import arc.files.Fi
import arc.util.ColorCodes
import arc.util.Log
import cf.wayzer.i18n.I18nApi.i18n
import cf.wayzer.i18n.I18nSentence
import cf.wayzer.mindustry.I18nHelper.sendMessage
import mindustry.Vars
import mindustry.core.NetServer
import mindustry.entities.type.Player
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.io.SaveIO
import mindustry.maps.Map
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.ceil
import kotlin.math.min

object Helper {
    fun loadMap(map: Map = nextMap(), mode: Gamemode = bestMode(map)) {
        resetAndLoad {
            Vars.world.loadMap(map, map.applyRules(mode))
            Vars.state.rules = Vars.world.map.applyRules(mode)
            Vars.logic.play()
        }
        Main.timer.schedule(2000L) {
            //After reset And Load
            when (mode) {
                Gamemode.pvp -> ScheduleTasks.pvpProtectTask.start()
                else -> return@schedule
            }
        }
    }

    fun loadSave(file: Fi) {
        resetAndLoad {
            SaveIO.load(file)
            Vars.logic.play()
        }
    }

    fun listBackup(): I18nSentence {
        val dataFormat = SimpleDateFormat("hh:mm")
        val list = Config.vote.savesRange.filter { SaveIO.fileFor(it).exists() }.map {
            "[red]{id}[]: [yellow]Save on {file.date}\n".i18n("id" to it,
                    "file.date" to dataFormat.format(Date(SaveIO.fileFor(it).lastModified())))
        }
        return """
            |[green]===[white] 自动存档 [green]===
            |{list}
            |[green]===[white] {range} [green]===
        """.trimMargin().i18n("range" to Config.vote.savesRange.toString(), "list" to list)
    }

    fun listMap(p: Int, mode: Gamemode? = Gamemode.survival): I18nSentence {
        val prePage = Config.base.mapsPrePage
        val maps = Config.maps.mapIndexed { index, map -> (index + 1) to map }
                .filter { mode == null || bestMode(it.second) == mode }
        val totalPage = ceil(maps.size.toDouble() / prePage).toInt()
        var page = p
        if (page < 1) page = 1
        if (page > totalPage) page = totalPage
        val list = maps.subList(prePage * (page - 1), min(maps.size, prePage * page)).map { (id, map) ->
            "[red]{id}[green]({map.width},{map.height})[]:[yellow]{map.fileName}[] | [blue]{map.name}\n".i18n("id" to "%2d".format(id), "_map" to map)
        }
        return """
            |[green]===[white] 服务器地图 [green]===
            |  [green]插件作者:[yellow]wayZer
            |{list}
            |[green]===[white] {page}/{totalPage} [green]===
        """.trimMargin().i18n("list" to list, "page" to page, "totalPage" to totalPage)
    }

    fun logToConsole(text: String) {
        val replaced = text.replace("[green]", ColorCodes.GREEN)
                .replace("[red]", ColorCodes.LIGHT_RED)
                .replace("[yellow]", ColorCodes.YELLOW)
                .replace("[white]", ColorCodes.WHITE)
                .replace("[blue]", ColorCodes.LIGHT_BLUE)
                .replace("[]", ColorCodes.RESET)
        Log.info(replaced)
    }

    fun broadcast(message: I18nSentence, type: I18nHelper.MsgType = I18nHelper.MsgType.Message, time: Float = 10f, quite: Boolean = false) {
        if (!quite) logToConsole("[Broadcast]$message")
        Vars.playerGroup.all().forEach {
            it.sendMessage(message, type, time)
        }
    }

    fun nextMap(map: Map? = null, mode: Gamemode = Gamemode.survival): Map {
        val maps = Config.maps.copy()
        maps.shuffle()
        return maps.filter { bestMode(it) == mode }.firstOrNull { it != map } ?: maps[0]
    }

    fun bestMode(map: Map): Gamemode {
        return when (map.file.name()[0]) {
            'A' -> Gamemode.attack
            'P' -> Gamemode.pvp
            'S' -> Gamemode.survival
            'C' -> Gamemode.sandbox
            'E' -> Gamemode.editor
            else -> Gamemode.bestFit(map.rules())
        }
    }

    private fun resetAndLoad(callBack: () -> Unit) {
        Core.app.post {
            val players = Vars.playerGroup.all().toList()
            players.forEach { it.dead = true }
            callBack()
            RuntimeData.reset(players)
            Call.onWorldDataBegin()
            players.forEach {
                if (it.con == null) return@forEach
                it.reset()
                if (Vars.state.rules.pvp)
                    it.team = Vars.netServer.assignTeam(it, players)
                Vars.netServer.sendWorldData(it)
            }
        }
    }

    fun secureLog(tag: String, text: String) {
        logToConsole("[yellow]$text")
        Config.pluginLog.writeString("[$tag][${Date()}] $text\n", true)
    }

    fun setTeamAssigner() {
        val old = Vars.netServer.assigner
        if (old !is MyAssigner)
            Vars.netServer.assigner = MyAssigner(old)
    }

    private class MyAssigner(private val old: NetServer.TeamAssigner) : NetServer.TeamAssigner {
        override fun assign(player: Player, p1: MutableIterable<Player>): Team {
            if (!Vars.state.rules.pvp) return Vars.state.rules.defaultTeam
            if (RuntimeData.teams[player.uuid]?.active() == false)
                RuntimeData.teams.remove(player.uuid)
            return RuntimeData.teams.getOrPut(player.uuid) {
                //not use old,because it may assign to team without core
                val teams = Vars.state.teams.active.filter { it.hasCore() }
                teams.shuffled()
                teams.minBy { p1.count { p -> p.team == it.team && player != p } }!!.team
            }
        }
    }
}
