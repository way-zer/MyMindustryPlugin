package cf.wayzer.mindustry

import io.anuke.arc.Core
import io.anuke.arc.files.FileHandle
import io.anuke.arc.util.ColorCodes
import io.anuke.arc.util.Log
import io.anuke.mindustry.Vars
import io.anuke.mindustry.entities.type.Player
import io.anuke.mindustry.game.Gamemode
import io.anuke.mindustry.game.Team
import io.anuke.mindustry.gen.Call
import io.anuke.mindustry.io.SaveIO
import io.anuke.mindustry.maps.Map
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.math.ceil

object Helper {
    fun loadMap(map: Map = nextMap(), mode: Gamemode = bestMode(map)) {
        resetAndLoad {
            Vars.logic.reset()
            Vars.world.loadMap(map, map.applyRules(mode))
            Vars.state.rules = Vars.world.map.applyRules(mode)
            Vars.logic.play()
        }
        Core.app.post {
            when(mode){
                Gamemode.pvp->{
                    Vars.state.rules.apply {
                        playerDamageMultiplier = 0.0000f
                        playerHealthMultiplier = 0.0001f
                    }
                    Main.timer.schedule(1000){
                        broadcast("[yellow]PVP保护时间,禁止直接偷家(持续"+TimeUnit.MILLISECONDS.toMinutes(Config.pvpProtectTime)+"分钟)")
                    }
                    Main.timer.schedule(Config.pvpProtectTime){
                        if(Vars.world.map != map)return@schedule
                        broadcast("[yellow]PVP保护时间已结束, 全力进攻吧")
                        Vars.state.rules.apply {
                            playerDamageMultiplier = 1f
                            playerHealthMultiplier = 1f
                        }
                        Vars.state.rules = map.rules(Vars.state.rules)
                    }
                }
                else->return@post
            }
        }
    }

    fun loadSave(file: FileHandle) {
        resetAndLoad {
            Vars.logic.reset()
            SaveIO.load(file)
            Vars.logic.play()
        }
    }

    fun listBackup(): String {
        val text = StringBuilder()
        text.append("[green]===[white] 自动存档 [green]===\n")
        Config.saveRange.forEach {
            val file = SaveIO.fileFor(it)
            if (file.exists()) {
                text.append("  [red]$it[]: [yellow]Save on ${Date(file.lastModified())}\n")
            }
        }
        text.append("[green]===[white] 100-105 [green]===")
        return text.toString()
    }

    fun listMap(p: Int): String {
        val maps = Config.maps
        val totalPage = ceil(maps.size / 7f).toInt()
        var page = p
        if (page < 1) page = 1
        if (page > totalPage) page = totalPage
        val text = StringBuilder()
        text.append("[green]===[white] 服务器地图 [green]===\n")
        maps.forEachIndexed { index, map ->
            if (index in 7 * (page - 1) until 7 * page) {
                text.append("  [red]${index + 1}[]:[yellow]${map.file.file().nameWithoutExtension}[] | [yellow]${map.name()}[]\n")
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
                .replace("[]", ColorCodes.RESET)
        Log.info(replaced)
    }

    fun broadcast(message: String) {
        logToConsole("[Broadcast]$message")
        Main.timer.run {
            Vars.playerGroup.all().forEach {
                it.sendMessage(message)
            }
        }
    }

    fun nextMap(map: Map? = null): Map {
        val maps = Config.maps.copy()
        maps.shuffle()
        return (if(Vars.playerGroup.size()<3) maps.filter { bestMode(it)!=Gamemode.pvp} else maps)
                .first { it != map } ?: maps[0]
    }

    fun bestMode(map: Map): Gamemode {
        return when (map.file.name()[0]) {
            'A' -> Gamemode.attack
            'P' -> Gamemode.pvp
            'S' -> Gamemode.survival
            else -> Gamemode.bestFit(map.rules())
        }
    }

    private fun resetAndLoad(callBack: () -> Unit) {
        Core.app.post {
            val players = Vars.playerGroup.all().copy()
            players.forEach { it.dead = true }
            callBack()
            Call.onWorldDataBegin()
            players.forEach {
                if (it.con == null) return@forEach
                it.reset()
                if (Vars.state.rules.pvp)
                    it.team = getTeam(it)
                Vars.netServer.sendWorldData(it)
            }
        }
    }

    fun secureLog(tag: String, text: String) {
        logToConsole("[yellow]$text")
        Config.pluginLog.writeString("[$tag][${Date()}] $text")
    }

    fun getTeam(player: Player):Team{
        return Listener.RuntimeData.teams.getOrPut(player.uuid){
            Vars.netServer.assignTeam(player,Vars.playerGroup.all())
        }
    }
}
