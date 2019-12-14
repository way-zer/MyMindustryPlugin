package cf.wayzer.mindustry

import io.anuke.arc.collection.Array
import io.anuke.mindustry.Vars
import io.anuke.mindustry.maps.Map
import java.util.*
import java.util.concurrent.TimeUnit

object Config {
    val motd = """
        |Welcome to this Server
        |[green]欢迎来到本服务器[]
        |常用指令:
        |  [yellow]/maps[] 查看所有地图
        |  [yellow]/vote[] 投票命令
        |    [green]/vote map x[] 投票选图
        |    [green]/vote kick xxx[] 投票踢人
        |    [green]/vote gameover[] 投票投降
        |    [green]/vote fallback[] 投票回滚(15分钟自动保存)
        |    [green]/vote skipwave[] 投票快进跳波
        """.trimMargin()
    val voteTime = TimeUnit.SECONDS.toMillis(10)
    val saveRange = 100..105 //From 100->105
    val voteSaveSolt = 111
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
