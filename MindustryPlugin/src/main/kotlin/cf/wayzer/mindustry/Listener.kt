package cf.wayzer.mindustry

import io.anuke.arc.Events
import io.anuke.mindustry.game.EventType

object Listener {
    fun register() {
        Events.on(EventType.PlayerJoin::class.java) { e ->
            e.player.sendMessage(Config.motd)
        }
        Events.on(EventType.PlayerChatEvent::class.java) { e ->
            if (e.message.equals("y", true))
                VoteHandler.handleVote(e.player)
        }
    }
}
