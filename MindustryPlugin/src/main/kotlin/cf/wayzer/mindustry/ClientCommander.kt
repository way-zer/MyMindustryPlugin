package cf.wayzer.mindustry

import io.anuke.arc.Core
import io.anuke.arc.Events
import io.anuke.arc.util.CommandHandler
import io.anuke.mindustry.Vars.logic
import io.anuke.mindustry.entities.type.Player
import io.anuke.mindustry.game.EventType
import io.anuke.mindustry.game.Team
import io.anuke.mindustry.io.SaveIO

object ClientCommander {
    fun register(handler: CommandHandler) {
        handler.removeCommand("vote")
//        handler.removeCommand("votekick")
//        handler.register<Player>("votekick", "<player>", "投票踢人(/vote kick <player>)") { arg, p ->
//            onVote(arrayOf("kick", arg[1]), p)
//        }
        handler.register("maps", "[page]", "查看服务器地图", ::onMaps)
        handler.register("vote", "<map/gameOver/kick/skipWave/rollback> [params]",
                "进行投票:换图/投降/踢人/跳波/回滚", ::onVote)
    }

    private fun onVote(arg: Array<String>, player: Player) {
        when (arg[0].toLowerCase()) {
            "map" -> {
                if (arg.size < 2)
                    return player.sendMessage("[red]请输入地图序号")
                val id = arg[1].toIntOrNull()
                if (id == null || id < 1 || id > Config.maps.size)
                    return player.sendMessage("[red]错误参数")
                if (VoteHandler.doing)
                    return player.sendMessage("[red]投票进行中")
                val map = Config.maps[id - 1]
                VoteHandler.startVote("换图") {
                    Helper.loadMap(map)
                }
                Core.app.post { }
            }
            "gameover" -> {
                if (VoteHandler.doing)
                    return player.sendMessage("[red]投票进行中")
                VoteHandler.startVote("投降") {
                    Events.fire(EventType.GameOverEvent(Team.crux))
                }
            }
            "skipwave" -> {
                if (VoteHandler.doing)
                    return player.sendMessage("[red]投票进行中")
                VoteHandler.startVote("跳波") {
                    for (i in 0..9) {
                        logic.runWave()
                    }
                }
            }
            "fallback" -> {
                if (arg.size < 2)
                    return player.sendMessage("[red]请输入存档编号")
                val id = arg[1].toIntOrNull()
                if (id == null || id !in Config.saveRange)
                    return player.sendMessage("[red]错误参数")
                val file = SaveIO.fileFor(id)
                if (!SaveIO.isSaveValid(file))
                    return player.sendMessage("[red]存档不存在或存档损坏")
                val voteFile = SaveIO.fileFor(Config.voteSaveSolt)
                if (voteFile.exists()) voteFile.delete()
                file.copyTo(voteFile)
                VoteHandler.startVote("回档") {
                    Helper.loadSave(voteFile)
                }
            }
            else -> {
                player.sendMessage("[red]开发中,请使用默认踢人功能")
            }
        }
    }

    private fun onMaps(arg: Array<String>, player: Player) {
        val page = arg.getOrNull(0)?.toIntOrNull() ?: 1
        player.sendMessage(Helper.listMap(page))
    }
}
