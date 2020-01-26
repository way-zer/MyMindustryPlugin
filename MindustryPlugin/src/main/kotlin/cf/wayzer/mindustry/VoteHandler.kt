package cf.wayzer.mindustry

import cf.wayzer.mindustry.Helper.broadcast
import mindustry.Vars.playerGroup
import mindustry.entities.type.Player
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.max

object VoteHandler {
    var doing = false
        private set
    private val voted = mutableListOf<String>()
    private var task: TimerTask? = null
    var welcomeMessage = ""
    var otherData: Any = ""
    fun startVote(text: String,justOne: Boolean =false, callback: () -> Unit): Boolean {
        var one = justOne
        if (doing) {
            return false
        }
        doing = true
        if(playerGroup.size()!=1)one =false
        broadcast("[yellow]$text [yellow]投票开始,输入y同意")
        welcomeMessage = "[yellow]当前正在进行$text [yellow]投票，输入y同意"
        task = Main.timer.schedule(Config.voteTime) {
            val require = max(playerGroup.size() / 2, 1)
            if (voted.size > require) {
                broadcast("[yellow]$text [yellow]投票结束,投票成功.[green]${voted.size}/${playerGroup.size()}[yellow],超过[red]$require [yellow]人")
                callback()
            } else if (one&&voted.size==1&&(Listener.lastJoin+Config.voteTime<System.currentTimeMillis())){
                broadcast("[yellow]$text [yellow]投票通过.")
                callback()
            }else {
                broadcast("[yellow]$text [yellow]投票结束,投票失败.[green]${voted.size}/${playerGroup.size()}[yellow],未超过[red]$require [yellow]人")
            }
            voted.clear()
            task = null
            doing = false
        }
        return true
    }

    fun handleVote(player: Player) {
        if (!doing) return
        if (voted.contains(player.uuid ?: "UNOWNED"))
            return player.sendMessage("[red]你已经投过票了")
        voted.add(player.uuid ?: "UNOWNED")
        player.sendMessage("[green]投票成功")
        val require = max(playerGroup.size() / 2, 1)
        if (voted.size > require) {
            task?.cancel()
            task?.run()
        }
    }

    fun handleJoin(player: Player){
        if(!doing)return
        player.sendMessage(welcomeMessage)
    }

    fun cancelVote(){
        if (!doing) return
        task?.cancel()
        voted.clear()
        task = null
        doing = false
    }
}
