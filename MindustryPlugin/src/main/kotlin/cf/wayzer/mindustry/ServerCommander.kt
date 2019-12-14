package cf.wayzer.mindustry

import io.anuke.arc.util.CommandHandler
import io.anuke.mindustry.Vars.world
import io.anuke.mindustry.game.Gamemode
import io.anuke.mindustry.io.SaveIO

object ServerCommander {
    fun register(handler: CommandHandler) {
        handler.register("mMaps", "[page]","List maps", ::onMaps)
        handler.register("mHost", "[id] [mode]","Change map", ::onChange)
        handler.register("mLoad", "<id>","Load save", ::onLoad)
    }

    private fun onMaps(arg: Array<String>) {
        val page = arg.getOrNull(0)?.toIntOrNull() ?: 1
        Helper.logToConsole(Helper.listMap(page))
    }

    private fun onChange(arg: Array<String>) {
        val map = arg.getOrNull(0)?.toIntOrNull()?.let { Config.maps[it-1] } ?: Helper.nextMap(world.map)
        val mode = arg.getOrNull(1)?.let { Gamemode.valueOf(it) } ?: Helper.bestMode(map)
        Helper.loadMap(map, mode)
        Helper.logToConsole("[green]Change to {name:${map.name()} file:${map.file} author:${map.author()}} with Mode ${mode.name}")
    }

    private fun onLoad(arg: Array<String>) {
        val file = arg.getOrNull(0)?.toIntOrNull()?.let { SaveIO.fileFor(it) }
                ?: return Helper.logToConsole("[red]Error slot id. Can't find slot")
        if (!SaveIO.isSaveValid(file))
            return Helper.logToConsole("[red]Slot is invalid")
        Helper.loadSave(file)
        Helper.logToConsole("[green]Load slot Success.")
    }
}
