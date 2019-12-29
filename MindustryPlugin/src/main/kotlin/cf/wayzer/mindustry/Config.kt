package cf.wayzer.mindustry

import io.anuke.arc.collection.Array
import io.anuke.mindustry.Vars
import io.anuke.mindustry.maps.Map
import java.util.*
import java.util.concurrent.TimeUnit

object Config {
    val dataFile = Vars.dataDirectory.child("pluginData.mapdb")
    val motd = """
        |Welcome to this Server
        |[green]欢迎来到本服务器[]
        |常用指令:
        |  [yellow]/status[] 查看服务器状态
        |  [yellow]/info[] 查看个人信息
        |  [yellow]/maps[] 查看所有地图
        |  [yellow]/slots[] 查看自动存档
        |  [yellow]/sync[] 同步数据(出现奇怪现象时使用)
        |  [yellow]/vote[] 投票命令
        |    [green]/vote map x[] 投票选图
        |    [green]/vote kick xxx[] 投票踢人
        |    [green]/vote gameOver[] 投票投降
        |    [green]/vote rollback[] 投票回滚(10分钟自动保存)
        |    [green]/vote skipWave[] 投票快进跳波
        |[yellow]----===最新消息(12.29更新)===----
        |[green]优化PVP队伍机制
        |[green]新增贡献度排行榜
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
            return Vars.maps.customMaps()
        }
}

