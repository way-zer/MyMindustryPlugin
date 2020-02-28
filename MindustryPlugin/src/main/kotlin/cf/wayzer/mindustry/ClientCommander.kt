package cf.wayzer.mindustry

import arc.Events
import arc.util.CommandHandler
import arc.util.Time
import cf.wayzer.i18n.I18nApi.i18n
import cf.wayzer.mindustry.I18nHelper.sendMessage
import cf.wayzer.mindustry.expr.UnitBuilder
import mindustry.Vars.*
import mindustry.core.NetClient
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.io.SaveIO
import mindustry.net.Packets
import java.lang.Integer.min
import java.text.SimpleDateFormat
import java.util.*
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
        handler.register("maps", "[page/pvp/attack/all] [page]", "查看服务器地图", ::onMaps)
        handler.register("slots", "查看自动存档") { _, p: Player -> p.sendMessage(Helper.listBackup()) }
        handler.register("vote", "<map/gameOver/kick/skipWave/rollback> [params...]",
                "进行投票:换图/投降/踢人/跳波/回滚", ::onVote)
        handler.register("spectate", "变为观察者", ::onSpectate)
        registerAdmin(handler)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onStatus(arg: Array<String>, player: Player) {
        player.sendMessage("""
            |[green]服务器状态[]
            |   [green]地图: [yellow]{map.name}[]
            |   [green]{fps} FPS, {heapUse} MB used[]
            |   [green]总单位数: {state.allUnit}
            |   [yellow]被禁封总数: {state.allBan}
        """.trimMargin().i18n())
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onInfo(arg: Array<String>, player: Player) {
        player.sendMessage("""
            | [#DEA82A] {player.name}个人信息[]
            | [#2B60DE]=======================================[]
            | [green]用户名[]:{player.name}
            | [green]UUID[]:{player.uuid}
            | [green]Ip地址[]:{player.ip}
            | [green]总在线时间(分钟)[]:{player.playedTime}
            | [green]当前等级[]:{player.level}
            | [green]当前经验[]:[blue]{player.exp}[]/{player.level}
            | [#2B60DE]=======================================[]
            | [yellow]说明: 因为性能考虑,数据不会动态刷新. 经验目前只能通过活动奖励
        """.trimMargin().i18n(), I18nHelper.MsgType.InfoMessage)
    }

    private fun onVote(arg: Array<String>, player: Player) {
        //Each
        Call.sendMessage("/vote " + arg.joinToString(" "), NetClient.colorizeName(player.id, player.name), player)
        if (playerGroup.size() == 1) {
            player.sendMessage("[yellow]当前服务器只有一人,若投票结束前没人加入,则一人也可通过投票(kick除外)".i18n())
        }
        when (arg[0].toLowerCase()) {
            "map" -> {
                if (arg.size < 2)
                    return player.sendMessage("[red]请输入地图序号".i18n())
                val id = arg[1].toIntOrNull()
                if (id == null || id < 1 || id > Config.maps.size)
                    return player.sendMessage("[red]错误参数".i18n())
                if (VoteHandler.doing)
                    return player.sendMessage("[red]投票进行中".i18n())
                val map = Config.maps[id - 1]
                VoteHandler.startVote("换图({map.id}: [yellow]{map.name}[yellow])".i18n("_map" to map), true) {
                    Helper.loadMap(map)
                    Main.timer.schedule(TimeUnit.SECONDS.toMillis(5)) {
                        Helper.broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".i18n())
                    }
                }
            }
            "gameover" -> {
                if (VoteHandler.doing)
                    return player.sendMessage("[red]投票进行中".i18n())
                if (state.rules.pvp) {
                    if (!state.teams.isActive(player.team) || state.teams.get(player.team)!!.cores.isEmpty)
                        return player.sendMessage("[red]队伍已输,无需投降".i18n())
                    else VoteHandler.startVote("投降({player.name}[yellow]|{team.colorizeName}[yellow]队)".i18n()) {
                        state.teams.get(player.team).cores.forEach { it.kill() }
                    }
                } else VoteHandler.startVote("投降".i18n(), true) {
                    Events.fire(EventType.GameOverEvent(Team.crux))
                }
            }
            "skipwave" -> {
                if (VoteHandler.doing)
                    return player.sendMessage("[red]投票进行中".i18n())
                VoteHandler.startVote("跳波".i18n(), true) {
                    var i = 0
                    Main.timer.schedule(0, Config.vote.skipWaveInterval.toMillis()) {
                        if (state.gameOver || state.enemies > 300 || i >= 10) cancel()
                        i++
                        logic.runWave()
                    }
                }
            }
            "rollback" -> {
                if (arg.size < 2)
                    return player.sendMessage("[red]请输入存档编号".i18n())
                val id = arg[1].toIntOrNull()
                if (id == null || id !in Config.vote.savesRange)
                    return player.sendMessage("[red]错误参数".i18n())
                val file = SaveIO.fileFor(id)
                if (!SaveIO.isSaveValid(file))
                    return player.sendMessage("[red]存档不存在或存档损坏".i18n())
                val voteFile = SaveIO.fileFor(Config.vote.tempSlot)
                if (voteFile.exists()) voteFile.delete()
                file.copyTo(voteFile)
                VoteHandler.startVote("回档".i18n(), true) {
                    Helper.loadSave(voteFile)
                    Main.timer.schedule(TimeUnit.SECONDS.toMillis(5)) {
                        Helper.broadcast("[green]回档成功".i18n())
                    }
                }
            }
            "kick" -> {
                if (arg.size < 2)
                    return player.sendMessage("[red]请输入玩家Id".i18n())
                val name = arg[1]
                val target = playerGroup.find { it.name == name }
                        ?: return player.sendMessage("[red]玩家未找到".i18n())
                if (Data.adminList.contains(player.uuid)) {
                    return onBan(arrayOf(target.uuid), player)
                }
                if (state.rules.pvp && player.team != target.team)
                    return player.sendMessage("[red]PVP模式禁止踢出其他队玩家".i18n())
                val result = VoteHandler.startVote("踢人({player.name}[yellow]踢出[red]{target.name}[yellow])".i18n("target.name" to target.name)) {
                    VoteHandler.otherData = ""
                    if (Data.adminList.contains(target.uuid)) {
                        return@startVote Helper.broadcast("[red]错误: 目标玩家为管理员, 如有问题请与服主联系".i18n())
                    }
                    if (target.info.timesKicked < 3) {
                        target.info.lastKicked = Time.millis() + (15 * 60 * 1000) //Kick for 15 Minutes
                        target.con?.kick(Packets.KickReason.vote)
                    } else
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
                player.sendMessage("[red]请检查输入是否正确".i18n())
            }
        }
    }

    @Suppress("DuplicatedCode")
    private fun onMaps(arg: Array<String>, player: Player) {
        val mode: Gamemode? = arg.getOrNull(0).let {
            when {
                !Config.base.mapsDistinguishMode -> null
                "pvp".equals(it, true) -> Gamemode.pvp
                "attack".equals(it, true) -> Gamemode.attack
                "all".equals(it, true) -> null
                else -> Gamemode.survival
            }
        }
        val page = arg.lastOrNull()?.toIntOrNull() ?: 1
        if (Config.base.mapsDistinguishMode)
            player.sendMessage("[yellow]默认只显示所有生存图,输入[green]/maps pvp[yellow]显示pvp图,[green]/maps attack[yellow]显示攻城图[green]/maps all[yellow]显示所有".i18n())
        player.sendMessage(Helper.listMap(page, mode))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onSpectate(arg: Array<String>, player: Player) {
        if (player.team == Config.spectateTeam)
            return player.sendMessage("[red]你已经是观察者了".i18n())
        Helper.broadcast("[yellow]玩家[green]{player.name}[yellow]选择成为观察者".i18n(), quite = true)
        player.team = Config.spectateTeam
        player.lastSpawner = null
        player.spawner = null
        Call.onPlayerDeath(player)
    }

    private fun registerAdmin(handler: CommandHandler) {
        //Admin command
        handler.register("list", "管理指令: 列出当前所有玩家信息", ::onListPlayer)
        handler.register("ban", "[3位id]", "管理指令: 列出已ban用户，ban或解ban", ::onBan)
        //auto reload before maps and change map
//        handler.register("reloadMaps","管理指令: 重载地图",::onReloadMaps)
        handler.register("robot", "实验性功能: 召唤专用鬼怪建筑机", ::onExperiment)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onListPlayer(arg: Array<String>, p: Player) {
        if (!Data.adminList.contains(p.uuid))
            return p.sendMessage("[red]你没有权限使用该命令".i18n())
        val text = playerGroup.all().map {
            "${it.name}[]:([red]${it.uuid.subSequence(0, 3)}[])"
        }.joinToString(" , ")
        p.sendMessage(text)
    }

    private fun onBan(arg: Array<String>, p: Player) {
        if (!Data.adminList.contains(p.uuid))
            return p.sendMessage("[red]你没有权限使用该命令".i18n())
        val uuid = arg.getOrNull(0)
        if (uuid == null) {
            val dateFormat = SimpleDateFormat("MM:dd")
            val sorted = netServer.admins.banned.sortedByDescending { it.lastKicked }
            //TODO i18n
            p.sendMessage("Bans: " + sorted.subList(0, min(8, sorted.size)).map {
                val date = dateFormat.format(Date(it.lastKicked))
                return@map "[white]${it.lastName}[]([red]${it.id.subSequence(0, 3)} [white]$date[])"
            }.joinToString(" , "))
        } else {
            netServer.admins.banned.forEach {
                if (it.id.startsWith(uuid)) {
                    netServer.admins.unbanPlayerID(it.id)
                    Helper.secureLog("UnBan", "${p.name} unBan ${it.lastName}(${it.id})")
                    return p.sendMessage("[green]解Ban成功 {player.name}".i18n("_player" to it))
                }
            }
            playerGroup.find { it.uuid.startsWith(uuid) }?.let {
                netServer.admins.banPlayerID(it.uuid)
                Helper.broadcast("[red] 管理员禁封了{player.name}".i18n("_player" to it))
                Helper.secureLog("Ban", "${p.name} Ban ${it.name}(${it.uuid})")
                return p.sendMessage("[green]Ban成功 {player.name}".i18n("_player" to it))
            }
            p.sendMessage("[red]找不到改用户,请确定三位字母id输入正确! /list 或 /ban 查看".i18n())
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onExperiment(arg: Array<String>, p: Player) {
        //TODO Expr without i18n
        if (!Data.adminList.contains(p.uuid))
            return p.sendMessage("[red]你没有权限使用该命令")
        p.sendMessage("[yellow]该功能目前正处于实验阶段，有问题请立即与WayZer联系")
        if (state.rules.pvp)
            return p.sendMessage("[red]PVP模式禁止使用")
        val now = RuntimeData.robots.getOrDefault(p.uuid, 0)
        if (now >= 2)
            return p.sendMessage("[red]目前一个玩家最多使用两个")
        RuntimeData.robots[p.uuid] = now + 1
        try {
            UnitBuilder.createForPlayer(p)
        } catch (e: Exception) {
            Helper.secureLog("EXPR_ERR", e.message ?: "")
            e.printStackTrace()
        }
    }
}
