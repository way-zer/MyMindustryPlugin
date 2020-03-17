package cf.wayzer.mindustry

import arc.util.CommandHandler
import cf.wayzer.i18n.I18nApi
import cf.wayzer.mindustry.Data.playerData
import mindustry.Vars
import mindustry.Vars.playerGroup
import mindustry.Vars.world
import mindustry.game.Gamemode
import mindustry.io.SaveIO
import java.text.SimpleDateFormat
import java.util.*

object ServerCommander {
    internal lateinit var commandHandler: CommandHandler
    fun register(handler: CommandHandler) {
        commandHandler = handler
        handler.removeCommand("maps")
        handler.removeCommand("host")
        handler.removeCommand("load")
        handler.register("maps", "[all/pvp/attack/page] [page]", "List maps", ::onMaps)
        handler.register("host", "[id] [mode]", "Change map", ::onChange)
        handler.register("load", "<id/name>", "Load save", ::onLoad)
        handler.register("addExp", "<playerId> <num>", "Add Exp to Player", ::onAddExp)
        handler.register("mInfo", "<uuid>", "Show Player info", ::onInfo)
        handler.register("mBans", "", "List bans", ::onBans)
        handler.register("mAdmin", "[uuid]", "List or Toggle admin", ::onAdmin)
        handler.register("reloadConfig", "reload plugin config") {
            Config.load()
            I18nApi.resetCache()
            Helper.logToConsole("[green]重载成功")
        }
    }

    @Suppress("DuplicatedCode")
    private fun onMaps(arg: Array<String>) {
        val mode: Gamemode? = arg.getOrNull(0).let {
            when {
                !Config.base.mapsDistinguishMode -> null
                "pvp".equals(it, true) -> Gamemode.pvp
                "attack".equals(it, true) -> Gamemode.attack
                "all".equals(it, true) -> null
                else -> Gamemode.survival
            }
        }
        val page = arg.lastOrNull()?.toIntOrNull() ?: 1
        Helper.logToConsole(Helper.listMap(page, mode).toString())
    }

    private fun onChange(arg: Array<String>) {
        if (!Vars.net.server()) Vars.netServer.openServer()
        val map = arg.getOrNull(0)?.toIntOrNull()?.let { Config.maps[it - 1] } ?: Helper.nextMap(world.map)
        val mode = arg.getOrNull(1)?.let { Gamemode.valueOf(it) } ?: Helper.bestMode(map)
        Helper.loadMap(map, mode)
        Helper.logToConsole("[green]Change to {name:${map.name()} file:${map.file} author:${map.author()}} with Mode ${mode.name}")
    }

    private fun onLoad(arg: Array<String>) {
        if (!Vars.net.server()) Vars.netServer.openServer()
        val file = arg.getOrNull(0)?.let { Vars.saveDirectory.child("$it.${Vars.saveExtension}") }
                ?: return Helper.logToConsole("[red]Error slot id. Can't find slot")
        if (!SaveIO.isSaveValid(file))
            return Helper.logToConsole("[red]Slot is invalid")
        Helper.loadSave(file)
        Helper.logToConsole("[green]Load slot Success.")
    }

    private fun onAddExp(arg: Array<String>) {
        val player = arg.getOrNull(0)?.let { id -> playerGroup.find { it.uuid == id } }
                ?: return Helper.logToConsole("[red]Error id. Can't find player")
        val num = arg.getOrNull(1)?.toIntOrNull()
                ?: return Helper.logToConsole("[red]Error num.")
        playerData[player.uuid] = playerData[player.uuid]!!.addExp(num)
        Helper.logToConsole("[green]Add Success.")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onBans(arg: Array<String>) {
        val list = Vars.netServer.admins.banned
        val sorted = list.sortedByDescending { it.lastKicked }
        val dateFormat = SimpleDateFormat("MM:dd")
        val builder = StringBuilder("Banned Player:")
        sorted.forEachIndexed { index, info ->
            if (index > 7) return //Show only 8
            val date = dateFormat.format(Date(info.lastKicked))
            builder.append("$date | ${info.id} | ${info.lastName} \n")
        }
        Helper.logToConsole(builder.toString())
    }

    private fun onInfo(arg: Array<String>) {
        val uuid = arg[0]
        val dataFormat = SimpleDateFormat("MM:dd hh:mm")
        val info = playerData[uuid] ?: return Helper.logToConsole("[red]Can't found player")
        with(info) {
            Helper.logToConsole("""
            | $lastName[]($uuid)
            | FirstJoin: ${dataFormat.format(firstJoin)} 
            | LastJoin: ${dataFormat.format(lastJoin)}($lastAddress)
            | PlayedTime(Min): ${playedTime / 60}
            | Level: $level(Exp $exp)
            """.trimMargin("|"))
        }
    }

    private fun onAdmin(arg: Array<String>) {
        val uuid = arg.getOrNull(0)
        if (uuid == null) {
            val dataFormat = SimpleDateFormat("MM:dd hh:mm")
            Helper.logToConsole("Admins: " + Data.adminList.map {
                val info = playerData[it] ?: return@map it
                return@map "${info.lastName}($it,${dataFormat.format(info.lastJoin)})"
            }.joinToString(" , "))
        } else {
            if (Data.adminList.contains(uuid)) {
                Data.adminList.remove(uuid)
                return Helper.logToConsole("[red]$uuid [green] has been removed from Admins[]")
            }
            val info = playerData[uuid] ?: return Helper.logToConsole("[red]Can't found player")
            Data.adminList.add(uuid)
            Helper.logToConsole("[red] ${info.lastName}($uuid) [green] has been added to Admins")
        }
    }
}
