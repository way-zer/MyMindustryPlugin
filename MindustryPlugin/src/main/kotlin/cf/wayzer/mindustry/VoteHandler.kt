package cf.wayzer.mindustry

import cf.wayzer.mindustry.Helper.broadcast
import io.anuke.mindustry.Vars.playerGroup
import io.anuke.mindustry.entities.type.Player
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.max

typealias CallBack = () -> Unit

object VoteHandler {
    var doing = false
        private set
    private val voted = mutableListOf<String>()
    private var task: TimerTask? = null
    var otherData: Any = ""
    fun startVote(text: String,justOne: Boolean =false, callback: CallBack): Boolean {
        if (doing) {
            return false
        }
        doing = true
        broadcast("[yellow]$text 投票开始,输入y同意")
        task = Main.timer.schedule(Config.voteTime) {
            val require = max(playerGroup.size() / 2, 1)
            if (voted.size > require) {
                broadcast("[yellow]$text 投票结束,投票成功.[green]${voted.size}/${playerGroup.size()}[yellow],超过[red]$require [yellow]人")
                callback()
            } else if (justOne&&voted.size==1&&(Listener.lastJoin+Config.voteTime<System.currentTimeMillis())){
                broadcast("[yellow]$text 投票通过.")
                callback()
            }else {
                broadcast("[yellow]$text 投票结束,投票失败.[green]${voted.size}/${playerGroup.size()}[yellow],未超过[red]$require [yellow]人")
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

    fun cancelVote(){
        if (!doing) return
        task?.cancel()
        voted.clear()
        task = null
        doing = false
    }
}
