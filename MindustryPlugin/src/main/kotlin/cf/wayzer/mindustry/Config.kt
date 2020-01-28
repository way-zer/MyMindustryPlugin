package cf.wayzer.mindustry

import arc.files.Fi
import arc.struct.Array
import mindustry.Vars
import mindustry.maps.Map
import java.util.*
import java.util.concurrent.TimeUnit

object Config {
    val dataFile: Fi = Vars.dataDirectory.child("pluginData.mapdb")
    val motd = """
        |Welcome to this Server
        |[green]欢迎来到本服务器[]
        |[yellow]本服插件为原创,请使用[red]/help[yellow]查看指令帮助
        |[blue]----===最新消息(1.25更新)===----
        |[green]现在默认只有生存模式，其他模式地图请投票选择
        """.trimMargin()
    val pluginLog = Vars.dataDirectory.child("logs").child("PluginLog.log")
    val unitToWarn = 150
    val unitToStop = 220
    val voteTime = TimeUnit.SECONDS.toMillis(60)
    val skipWaveInterval = TimeUnit.SECONDS.toMillis(10)
    val saveRange = 100..105 //From 100->105
    val voteSaveSolt = 111
    val pvpProtectTime = TimeUnit.MINUTES.toMillis(10)
    val waitingTimeRound = TimeUnit.SECONDS.toMillis(10)//下一轮等待时间
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
            return Vars.maps.customMaps()!!
        }
}

