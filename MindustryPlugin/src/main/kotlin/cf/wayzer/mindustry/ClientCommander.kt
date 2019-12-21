package cf.wayzer.mindustry

import cf.wayzer.mindustry.Data.playerData
import io.anuke.arc.Core
import io.anuke.arc.Events
import io.anuke.arc.util.CommandHandler
import io.anuke.arc.util.Time
import io.anuke.mindustry.Vars
import io.anuke.mindustry.Vars.*
import io.anuke.mindustry.entities.type.Player
import io.anuke.mindustry.game.EventType
import io.anuke.mindustry.game.Team
import io.anuke.mindustry.gen.Call
import io.anuke.mindustry.io.SaveIO
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

object ClientCommander {
    fun register(handler: CommandHandler) {
        handler.removeCommand("vote")
        handler.removeCommand("votekick")
        handler.register<Player>("votekick", "<player...>", "投票踢人(/vote kick <player>)") { arg, p ->
            onVote(arrayOf("kick", arg[0]), p)
        }

        handler.register("status", "查看服务器状态", ::onStatus)
        handler.register("info", "查看个人信息", ::onInfo)
        handler.register("maps", "[page]", "查看服务器地图", ::onMaps)
        handler.register("slots", "查看自动存档") { _, p: Player -> p.sendMessage(Helper.listBackup()) }
        handler.register("vote", "<map/gameOver/kick/skipWave/rollback> [params...]",
                "进行投票:换图/投降/踢人/跳波/回滚", ::onVote)
        registerAdmin(handler)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onStatus(arg: Array<String>, player: Player) {
        player.sendMessage("""
            |[green]服务器状态[]
            |   [green]地图: [yellow]${Vars.world.map.name()}[]
            |   [green]${(60f / Time.delta()).toInt()} FPS, ${Core.app.javaHeap / 1024 / 1024} MB used[]
        """.trimMargin())
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onInfo(arg: Array<String>, player: Player) {
        val text = with(playerData[player.uuid]!!) {
            """
            | [#DEA82A] ${player.name}个人信息[]
            | [#2B60DE]=======================================[]
            | [green]用户名[]:$lastName
            | [green]UUID[]:$uuid
            | [green]Ip地址[]:$lastAddress
            | [green]总在线时间(分钟)[]:${playedTime / 60}
            | [green]当前等级[]:$level
            | [green]当前经验[]:[blue]$exp[]/${getMaxExp()}
            | [#2B60DE]=======================================[]
            | [yellow]说明: 因为性能考虑,数据不会动态刷新. 经验目前只能通过活动奖励
        """.trimMargin()
        }
        Call.onInfoMessage(player.con, text)
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
                VoteHandler.startVote("换图($id: [yellow]${map.name()}[])") {
                    Helper.loadMap(map)
                    Main.timer.schedule(TimeUnit.SECONDS.toMillis(5)) {
                        Helper.broadcast("[green]换图成功,当前地图[yellow]${map.name()}[green](id: $id)")
                    }
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
                    for (i in 0 until 10) {
                        logic.runWave()
                        if (unitGroups[waveTeam.ordinal].size() > 300) break
                    }
                }
            }
            "rollback" -> {
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
                    Main.timer.schedule(TimeUnit.SECONDS.toMillis(5)) {
                        Helper.broadcast("[green]回档成功")
                    }
                }
            }
            "kick" -> {
                if (arg.size < 2)
                    return player.sendMessage("[red]请输入玩家Id")
                val name = arg[1]
                val target = playerGroup.find { it.name == name }
                        ?: return player.sendMessage("[red]玩家未找到")
                if(Data.adminList.contains(player.uuid)){
                    return onBan(arrayOf(target.uuid),player)
                }
                val result = VoteHandler.startVote("踢人(${player.name}踢出[red]${target.name}[])") {
                    VoteHandler.otherData = ""
                    netServer.admins.banPlayer(target.uuid)
                    Helper.secureLog("Kick", "${target.name}(${target.uuid},${target.con.address}) is kicked By ${player.name}")
                }
                if (result) {
                    VoteHandler.otherData = "KICK-" + target.uuid
                } else if ("KICK-" + target.uuid == VoteHandler.otherData) {
                    VoteHandler.handleVote(player)
                }
            }
            else -> {
                player.sendMessage("[red]请检查输入是否正确")
            }
        }
    }

    private fun onMaps(arg: Array<String>, player: Player) {
        val page = arg.getOrNull(0)?.toIntOrNull() ?: 1
        player.sendMessage(Helper.listMap(page))
    }

    private fun registerAdmin(handler: CommandHandler) {
        //Admin command
        handler.register("list", "管理指令: 列出当前所有玩家信息", ::onListPlayer)
        handler.register("ban", "[3位id]", "管理指令: 列出已ban用户，ban或解ban", ::onBan)
        handler.register("reloadMaps","管理指令: 重载地图",::onReloadMaps)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onListPlayer(arg: Array<String>, p: Player) {
        if (!Data.adminList.contains(p.uuid))
            return p.sendMessage("[red]你没有权限使用该命令")
        val text = playerGroup.all().map {
            "${it.name}[]:([red]${it.uuid.subSequence(0, 3)}[])"
        }.joinToString(" , ")
        p.sendMessage(text)
    }

    private fun onBan(arg: Array<String>, p: Player) {
        if (!Data.adminList.contains(p.uuid))
            return p.sendMessage("[red]你没有权限使用该命令")
        val uuid = arg.getOrNull(0)
        if (uuid == null) {
            p.sendMessage("Bans: " + netServer.admins.banned.map {
                return@map "[white]${it.lastName}[]([red]${it.id.subSequence(0, 3)}[])"
            }.joinToString(" , "))
        } else {
            netServer.admins.banned.forEach {
                if (it.id.startsWith(uuid)) {
                    netServer.admins.unbanPlayerID(it.id)
                    Helper.secureLog("UnBan", "${p.name} unBan ${it.lastName}(${it.id})")
                    return p.sendMessage("[green]解Ban成功 ${it.lastName}")
                }
            }
            playerGroup.find { it.uuid.startsWith(uuid) }?.let {
                netServer.admins.banPlayerID(it.uuid)
                Helper.broadcast("[red] 管理员禁封了${it.name}")
                Helper.secureLog("Ban", "${p.name} Ban ${it.name}(${it.uuid})")
                return p.sendMessage("[green]Ban成功 ${it.name}")
            }
            p.sendMessage("[red]找不到改用户,请确定三位字母id输入正确! /list 或 /ban 查看")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onReloadMaps(arg: Array<String>, p: Player) {
        if (!Data.adminList.contains(p.uuid))
            return p.sendMessage("[red]你没有权限使用该命令")
        Vars.maps.reload()
        p.sendMessage("[green]地图重载成功!")
    }
}
