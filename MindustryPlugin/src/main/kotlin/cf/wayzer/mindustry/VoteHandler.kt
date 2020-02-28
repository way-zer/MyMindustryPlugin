package cf.wayzer.mindustry

import cf.wayzer.i18n.I18nApi.i18n
import cf.wayzer.i18n.I18nSentence
import cf.wayzer.mindustry.Helper.broadcast
import cf.wayzer.mindustry.I18nHelper.sendMessage
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
    var welcomeMessage: I18nSentence? = null
    var otherData: Any = ""
    fun startVote(type: I18nSentence, justOne: Boolean = false, callback: () -> Unit): Boolean {
        var one = justOne
        if (doing) {
            return false
        }
        doing = true
        if (playerGroup.size() != 1) one = false
        // C as default color
        broadcast("[yellow]{type}[yellow]投票开始,输入y同意".i18n("type" to type))
        welcomeMessage = "[yellow]当前正在进行{type}[yellow]投票，输入y同意".i18n("type" to type)
        task = Main.timer.schedule(Config.vote.voteTime.toMillis()) {
            val require = max(playerGroup.size() / 2, 1)
            if (voted.size > require) {
                broadcast("[yellow]{type}[yellow]投票结束,投票成功.[green]{voted}/{state.playerSize}[yellow],超过[red]{require}[yellow]人"
                        .i18n("type" to type, "voted" to voted.size, "require" to require))
                callback()
            } else if (one && voted.size == 1 && (Listener.lastJoin + Config.vote.voteTime.toMillis() < System.currentTimeMillis())) {
                broadcast("[yellow]{type}[yellow]单人投票通过.".i18n("type" to type))
                callback()
            } else {
                broadcast("[yellow]{type}[yellow]投票结束,投票失败.[green]{voted}/{state.playerSize}[yellow],未超过[red]{require}[yellow]人"
                        .i18n("type" to type, "voted" to voted.size, "require" to require))
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
            return broadcast("[red]你已经投过票了".i18n(), quite = true)
        voted.add(player.uuid ?: "UNOWNED")
        broadcast("[green]投票成功".i18n(), quite = true)
        val require = max(playerGroup.size() / 2, 1)
        if (voted.size > require) {
            task?.cancel()
            task?.run()
        }
    }

    fun handleJoin(player: Player) {
        if (!doing || welcomeMessage == null) return
        player.sendMessage(welcomeMessage!!)
    }

    fun cancelVote() {
        if (!doing) return
        task?.cancel()
        voted.clear()
        task = null
        doing = false
    }
}
