package cf.wayzer.mindustry

import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.io.Serializable
import java.util.*
import kotlin.math.pow

object Data {
    data class PlayerData(
            val uuid: String,
            val lastName: String,
            val firstJoin: Date,
            val lastJoin: Date,
            val lastAddress: String,
            val playedTime: Int,
            val level: Int,
            val exp: Int,
            val selectedLang: String? = null
    ) : Serializable {
        fun getMaxExp(level: Int = this.level) = (level.toDouble().pow(1.5) * 1000).toInt()
        fun addPlayedTime(addTime: Int) = copy(playedTime = playedTime + addTime)
        fun addExp(addExp: Int): PlayerData {
            var exp = this.exp + addExp
            var level = this.level
            while (exp >= getMaxExp(level)) {
                exp -= getMaxExp(level)
                level++
            }
            return copy(exp = exp, level = level)
        }

        val lang: String
            get() {
                if (selectedLang == null || !Config.base.allLang.contains(selectedLang))
                    return Config.base.defaultLang
                return selectedLang
            }

        companion object {
            private const val serialVersionUID = -1823529285823858212L
        }
    }

    private val db = DBMaker.fileDB(Config.dataFile.file())
            .checksumHeaderBypass()
            .closeOnJvmShutdown()
            .fileMmapEnableIfSupported()
            .make()
    @Suppress("UNCHECKED_CAST")
    val playerData = db.hashMap("PlayerData", Serializer.STRING_ASCII, Serializer.JAVA as Serializer<PlayerData>).createOrOpen()
    val adminList = db.hashSet("adminList", Serializer.STRING).createOrOpen()
}
