package cf.wayzer.mindustry

import arc.files.Fi
import arc.struct.Array
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import io.github.config4k.extract
import mindustry.Vars
import mindustry.game.Team
import mindustry.maps.Map
import java.time.Duration
import java.util.*

object Config {
    val dataFile: Fi = Vars.dataDirectory.child("pluginData.mapdb")
    val spectateTeam: Team = Team.all()[255]
    val nextSaveTime: Date
        get() {//Every 10 minutes
            val t = Calendar.getInstance()
            t.set(Calendar.SECOND, 0)
            val mNow = t.get(Calendar.MINUTE)
            t.add(Calendar.MINUTE, (mNow + 10) / 10 * 10 - mNow)
            return t.time
        }
    val maps: Array<Map>
        get() {
            Vars.maps.reload()
            return if (base.enableInternMaps) Vars.maps.all() else Vars.maps.customMaps()!!
        }
    val pluginLog: Fi = Vars.dataDirectory.child("logs").child("PluginLog.log")

    data class GameConfig(
            val welcome: String = """
        |Welcome to this Server
        |[green]欢迎来到本服务器[]
        """.trimMargin(),
            //单位警告数量,超出将阻止生成
            val unitWarnRange: IntRange = 150 until 220,
            //下一轮等待时间
            val waitingTime: Duration = Duration.ofSeconds(10),
            val pvpProtectTime: Duration = Duration.ofMinutes(10),
            val enableInternMaps: Boolean = false,
            val mapsDistinguishMode: Boolean = true,
            val alertTime: Duration = Duration.ofMinutes(20),
            val alertUseToast: Boolean = false,
            val alerts: List<String> = listOf()
    )

    data class VoteConfig(
            val voteTime: Duration = Duration.ofSeconds(60),
            val skipWaveInterval: Duration = Duration.ofSeconds(10),
            val savesRange: IntRange = 100..105,
            val tempSlot: Int = 111
    )

    private val defaultConfig = ConfigFactory.parseReader(javaClass.classLoader.getResourceAsStream("defaultConf.conf")!!.reader())
    private val configFile: Fi = Vars.dataDirectory.child("pluginConf.conf")

    lateinit var base: GameConfig
    lateinit var vote: VoteConfig
    fun load() {
        val config = ConfigFactory.parseFile(configFile.file())
                .withFallback(defaultConfig)
        config.checkValid(defaultConfig)
        base = config.extract("base")
        vote = config.extract("vote")
        configFile.writeString(config.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
    }
}

