package cf.wayzer.mindustry

import arc.Core
import arc.util.Time
import cf.wayzer.i18n.I18nApi
import cf.wayzer.i18n.I18nSentence
import cf.wayzer.i18n.PlaceHoldHandler
import mindustry.Vars
import mindustry.entities.type.Player
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.maps.Map

object I18nHelper {
    enum class MsgType {
        Message, InfoMessage, InfoToast,
    }

    private fun registerVar(name: String, f: (loadOther: (String) -> Any?) -> Any?) {
        I18nApi.registerGlobalVal(name, object : PlaceHoldHandler {
            override fun handle(getOther: (String) -> Any?): Any? = f(getOther)
        })
    }

    private fun I18nSentence.bindForPlayer(p: Player): I18nSentence {
        val info = Data.playerData[p.uuid]!!
        addVars(
                "_player" to p,
                "_player._info" to info,
                "_lang" to info.lang
        )
        return this
    }

    fun registerSystemVars() {
        registerVar("fps") { (60f / Time.delta()).toInt() }
        registerVar("heapUse") { Core.app.javaHeap / 1024 / 1024 }
    }

    fun registerGameVars() {
        registerVar("_map") { Vars.world.map }
        registerVar("map.name") { (it("_map") as? Map)?.name() }
        registerVar("map.id") {
            (it("_map") as? Map)?.let {
                Config.maps.indexOf(it, true)
            }
        }
        registerVar("map.desc") { (it("_map") as? Map)?.description() }
        registerVar("map.author") { (it("_map") as? Map)?.author() }
        registerVar("map.width") { (it("_map") as? Map)?.width }
        registerVar("map.height") { (it("_map") as? Map)?.height }
        registerVar("map.fileName") { (it("_map") as? Map)?.file?.nameWithoutExtension() }
        registerVar("state.allUnit") { Vars.unitGroup.size() }
        registerVar("state.allBan") { Vars.netServer.admins.banned.size }
        registerVar("state.playerSize") { Vars.playerGroup.size() }
    }

    fun registerPlayerVars() {
        registerVar("player.name") {
            (it("_player") as? Player)?.name ?: let {
                (it("_info") as? Data.PlayerData)?.lastName //Another way
            }
        }
        registerVar("player.uuid") { (it("_player") as? Player)?.uuid }
        registerVar("player.ip") { (it("_player") as? Player)?.con?.address }
        registerVar("player.playedTime") { (it("_player._info") as? Data.PlayerData)?.playedTime?.div(60) }
        registerVar("player.level") { (it("_player._info") as? Data.PlayerData)?.level }
        registerVar("player.exp") { (it("_player._info") as? Data.PlayerData)?.exp }
        registerVar("player.maxExp") { (it("_player._info") as? Data.PlayerData)?.getMaxExp() }
        registerVar("_team") { (it("_player") as? Player)?.team }
        registerVar("team.name") { (it("_team") as? Team)?.name }
        registerVar("team.color") { (it("_team") as? Team)?.let { "[#${it.color}]" } }
        registerVar("team.colorizeName") { it("team.color")?.toString() + it("team.name").toString() }
    }

    fun init() {
        I18nApi.init(Vars.dataDirectory.child("lang").file().toPath())
        I18nApi.setDisplayName("原生中文")
        registerSystemVars()
        registerGameVars()
        registerPlayerVars()
    }

    fun Player.sendMessage(msg: I18nSentence, type: MsgType = MsgType.Message, time: Float = 10f) {
        val text = msg.bindForPlayer(this).toString()
        when (type) {
            MsgType.Message -> sendMessage(text)
            MsgType.InfoMessage -> Call.onInfoMessage(this.con, text)
            MsgType.InfoToast -> Call.onInfoToast(this.con, text, time)
        }
    }
}
