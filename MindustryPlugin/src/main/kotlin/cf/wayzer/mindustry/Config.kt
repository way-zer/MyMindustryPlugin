package cf.wayzer.mindustry

import io.anuke.arc.collection.Array
import io.anuke.mindustry.Vars
import io.anuke.mindustry.maps.Map
import org.mapdb.DBMaker
import org.mapdb.HTreeMap
import org.mapdb.Serializer
import java.io.Serializable
import java.lang.Math.pow
import java.util.*
import java.util.concurrent.TimeUnit

object Config {
    object Data{
        data class PlayerData(
                val uuid:String,
                val lastName:String,
                val firstJoin:Date,
                val lastJoin:Date,
                val lastAddress:String,
                val playedTime:Int,
                val level:Int,
                val exp:Int
        ): Serializable{
            fun getMaxExp(level:Int = this.level)=(pow(level.toDouble(),1.5)*1000).toInt()
            fun addPlayedTime(addTime:Int)=copy(playedTime=playedTime+addTime)
            fun addExp(addExp:Int): PlayerData {
                var exp = this.exp+addExp
                var level = this.level
                while (exp>=getMaxExp(level)){
                    exp-=getMaxExp(level)
                    level++;
                }
                return copy(exp = exp,level = level)
            }
        }
        val db = DBMaker.fileDB(Vars.dataDirectory.child("pluginData").file())
                .closeOnJvmShutdown()
                .fileMmapEnableIfSupported()
                .make()
        @Suppress("UNCHECKED_CAST")
        val playerData = db.hashMap("PlayerData", Serializer.STRING_ASCII, Serializer.JAVA as Serializer<PlayerData>).createOrOpen()
    }
    val motd = """
        |Welcome to this Server
        |[green]欢迎来到本服务器[]
        |常用指令:
        |  [yellow]/status[] 查看服务器状态
        |  [yellow]/info[] 查看个人信息
        |  [yellow]/maps[] 查看所有地图
        |  [yellow]/slots[] 查看自动存档
        |  [yellow]/vote[] 投票命令
        |    [green]/vote map x[] 投票选图
        |    [green]/vote kick xxx[] 投票踢人
        |    [green]/vote gameOver[] 投票投降
        |    [green]/vote rollback[] 投票回滚(10分钟自动保存)
        |    [green]/vote skipWave[] 投票快进跳波
        """.trimMargin()
    val pluginLog = Vars.dataDirectory.child("logs").child("PluginLog.log")
    val voteTime = TimeUnit.SECONDS.toMillis(60)
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

