package cf.wayzer.mindustry

import io.anuke.arc.Events
import io.anuke.mindustry.content.Blocks
import io.anuke.mindustry.game.EventType
import io.anuke.mindustry.world.Block
import java.util.*

object Listener {
    fun register() {
        Events.on(EventType.PlayerJoin::class.java) { e ->
            e.player.sendMessage(Config.motd)
        }
        Events.on(EventType.PlayerChatEvent::class.java) { e ->
            if (e.message.equals("y", true))
                VoteHandler.handleVote(e.player)
        }
        Events.on(EventType.DepositEvent::class.java){e->
            if(e.tile.block() == Blocks.thoriumReactor && e.tile.block().liquidPressure <0.001){
                Helper.broadcast("[red][WARNING!][yellow]${e.player.name}正在进行危险行为(${e.tile.x},${e.tile.y})!")
                Config.pluginLog.writeString("[ThoriumReactor]${Date()}:${e.player.name}|(${e.tile.x},${e.tile.y})",true)
            }
        }
    }
}
