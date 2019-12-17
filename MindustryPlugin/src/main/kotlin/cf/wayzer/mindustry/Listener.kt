package cf.wayzer.mindustry

import cf.wayzer.mindustry.Config.Data.playerData
import io.anuke.arc.Events
import io.anuke.mindustry.content.Blocks
import io.anuke.mindustry.game.EventType
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.util.*

object Listener {
    private val runtime = DBMaker.memoryDB().make()
    private val joinTime = runtime.hashMap("joinTime", Serializer.STRING_ASCII, Serializer.LONG).expireAfterGet().createOrOpen()
    private val onlineExp = runtime.hashMap("onlineExp", Serializer.STRING_ASCII, Serializer.INTEGER).expireAfterGet().createOrOpen()
    fun register() {
        Events.on(EventType.PlayerJoin::class.java) { e ->
            e.player.sendMessage(Config.motd)
            val data = playerData[e.player.uuid]?:let { Config.Data.PlayerData(
                    e.player.uuid,"", Date(), Date(),"",0,0,0
            ) }
            playerData[e.player.uuid]= data.copy(lastName = e.player.name,lastJoin = Date(),lastAddress = e.player.con.address)
            joinTime[e.player.uuid]=System.currentTimeMillis()
        }
        Events.on(EventType.PlayerLeave::class.java){e->
            var data = playerData[e.player.uuid]!!
            data = data.addPlayedTime(((System.currentTimeMillis()- joinTime[e.player.uuid]!!)/1000).toInt())
            onlineExp[e.player.uuid]?.let { data=data.addExp(it) }
            playerData[e.player.uuid]=data
        }
        Events.on(EventType.PlayerChatEvent::class.java) { e ->
            if (e.message.equals("y", true))
                VoteHandler.handleVote(e.player)
        }
        Events.on(EventType.DepositEvent::class.java){e->
            if(e.tile.block() == Blocks.thoriumReactor && e.tile.block().liquidPressure <1){
                Helper.broadcast("[red][WARNING!][yellow]${e.player.name}正在进行危险行为(${e.tile.x},${e.tile.y})!")
                Config.pluginLog.writeString("[ThoriumReactor]${Date()}:${e.player.name}|(${e.tile.x},${e.tile.y})",true)
            }
        }
    }
}
