package cf.wayzer.mindustry

import arc.Core
import arc.files.Fi
import arc.util.ColorCodes
import arc.util.Log
import cf.wayzer.mindustry.util.DownTime
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
import kotlin.math.ceil

object Helper {
    fun loadMap(map: Map = nextMap(), mode: Gamemode = bestMode(map)) {
        resetAndLoad {
            Vars.world.loadMap(map, map.applyRules(mode))
            Vars.state.rules = Vars.world.map.applyRules(mode)
            Vars.logic.play()
        }
        Core.app.post {//After reset And Load
            when(mode){
                Gamemode.pvp->{
                    RuntimeData.protectDownTime = DownTime(Config.base.pvpProtectTime.toMillis(), {
                        RuntimeData.pvpProtect = true
                        broadcast("[yellow]PVP保护时间,禁止在其他基地攻击(持续" + Config.base.pvpProtectTime.toMinutes() + "分钟)")
                    }, {
                        if (Vars.world.map != map) return@DownTime false
                        Call.onInfoToast("[yellow]PVP保护时间还剩 $it 分钟", 10f)
                        return@DownTime true
                    }, {
                        RuntimeData.pvpProtect = false
                        broadcast("[yellow]PVP保护时间已结束, 全力进攻吧")
                    }).apply(DownTime::start)
                }
                else->return@post
            }
        }
    }

    fun loadSave(file: Fi) {
        resetAndLoad {
            SaveIO.load(file)
            Vars.logic.play()
        }
    }

    fun listBackup(): String {
        val text = StringBuilder()
        text.append("[green]===[white] 自动存档 [green]===\n")
        val dataFormat = SimpleDateFormat("hh:mm")
        Config.vote.savesRange.forEach {
            val file = SaveIO.fileFor(it)
            if (file.exists()) {
                text.append("  [red]$it[]: [yellow]Save on ${dataFormat.format(Date(file.lastModified()))}\n")
            }
        }
        text.append("[green]===[white] 100-105 [green]===")
        return text.toString()
    }

    fun listMap(p: Int,mode: Gamemode?= Gamemode.survival): String {
        val prePage = 7
        val maps = Config.maps.mapIndexed { index, map -> (index + 1) to map }
                .filter { mode == null || bestMode(it.second) == mode }
        val totalPage = ceil(maps.size / 7f).toInt()
        var page = p
        if (page < 1) page = 1
        if (page > totalPage) page = totalPage
        val text = StringBuilder()
        text.append("[green]===[white] 服务器地图 [green]===\n")
        text.append("  [green]插件作者:[yellow]wayZer\n")
        maps.forEachIndexed { index, pair ->
            //pair: id,map
            if (index in prePage * (page - 1) until prePage * page) {
                with(pair.second) {
                    text.append("[red]%2d[green](%d,%d)[]:[yellow]%s[]\t | [blue]%s\n".format(pair.first, width, height, file.file().nameWithoutExtension, name()))
                }
            }
        }
        text.append("[green]===[white] $page/$totalPage [green]===")
        return text.toString()
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

    fun broadcast(message: String, quite: Boolean = false) {
        Main.timer.run {
            if (!quite) logToConsole("[Broadcast]$message")
            Vars.playerGroup.all().forEach {
                if (it.con != null)
                    it.sendMessage(message)
            }
        }
    }

    fun nextMap(map: Map? = null,mode: Gamemode = Gamemode.survival): Map {
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
                    it.team = Vars.netServer.assignTeam(it,players)
                Vars.netServer.sendWorldData(it)
            }
        }
    }

    fun secureLog(tag: String, text: String) {
        logToConsole("[yellow]$text")
        Config.pluginLog.writeString("[$tag][${Date()}] $text\n",true)
    }

    fun setTeamAssigner(){
        val old = Vars.netServer.assigner
        if(old !is MyAssigner)
            Vars.netServer.assigner=MyAssigner(old)
    }

    private class MyAssigner(private val old:NetServer.TeamAssigner):NetServer.TeamAssigner{
        override fun assign(player: Player, p1: MutableIterable<Player>): Team {
            if (!Vars.state.rules.pvp) return Vars.state.rules.defaultTeam
            return RuntimeData.teams.getOrPut(player.uuid) {
                //not use old,because it may assign to team without core
                val teams = Vars.state.teams.active.filter { it.hasCore() }
                teams.shuffled()
                teams.minBy { p1.count { p -> p.team == it.team && player != p } }!!.team
            }
        }
    }
}
