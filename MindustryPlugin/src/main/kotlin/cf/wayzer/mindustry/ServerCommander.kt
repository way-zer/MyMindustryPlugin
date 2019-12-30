package cf.wayzer.mindustry

import cf.wayzer.mindustry.Data.playerData
import arc.Core
import arc.util.CommandHandler
import mindustry.Vars
import mindustry.Vars.playerGroup
import mindustry.Vars.world
import mindustry.game.Gamemode
import mindustry.io.SaveIO

object ServerCommander {
    fun register(handler: CommandHandler) {
        handler.removeCommand("maps")
        handler.removeCommand("host")
        handler.removeCommand("load")
        handler.register("maps", "[page]", "List maps", ::onMaps)
        handler.register("host", "[id] [mode]", "Change map", ::onChange)
        handler.register("load", "<id/name>", "Load save", ::onLoad)
        handler.register("addExp", "<playerId> <num>", "Add Exp to Player", ::onAddExp)
        handler.register("mAdmin", "[uuid]", "List or Toggle admin", ::onAdmin)
    }

    private fun onMaps(arg: Array<String>) {
        val page = arg.getOrNull(0)?.toIntOrNull() ?: 1
        Helper.logToConsole(Helper.listMap(page))
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

    private fun onAdmin(arg: Array<String>) {
        val uuid = arg.getOrNull(0)
        if (uuid == null) {
            Helper.logToConsole("Admins: " + Data.adminList.map {
                val info = playerData[it] ?: return@map it
                return@map "${info.lastName}($it)"
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
