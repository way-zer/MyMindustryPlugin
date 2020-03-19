package cf.wayzer.mindustry

import arc.util.Interval
import arc.util.Time
import mindustry.Vars
import mindustry.entities.type.Player
import mindustry.game.Team

// Data for one play(map)
object RuntimeData {
    enum class Intervals(private val tick: Float) {
        UnitWarn(2 * 60f);

        operator fun invoke() = interval[this.ordinal, tick]

        companion object {
            private val interval = Interval(values().size)
        }
    }

    var startTime = 0L
    val beginTime = mutableMapOf<String, Long>()
    val gameTime = mutableMapOf<String, Int>()
    val teams = mutableMapOf<String, Team>()
    val robots = mutableMapOf<String, Int>()
    var pvpProtect = false
    fun reset(players: Iterable<Player> = Vars.playerGroup.all()) {
        pvpProtect = false
        startTime = System.currentTimeMillis()
        beginTime.clear()
        gameTime.clear()
        teams.clear()
        robots.clear()
        // 设置所有在场玩家时间
        players.forEach {
            beginTime[it.uuid] = System.currentTimeMillis()
        }
    }

    fun calTime() {
        beginTime.forEach { (uuid, start) ->
            gameTime.merge(uuid, (System.currentTimeMillis() - start).toInt(), Int::plus)
            beginTime[uuid] = System.currentTimeMillis()
        }
    }

    /**
     * ensure map don't change in last time
     * @param time in millis
     */
    fun ensureNotChange(time: Int): Boolean {
        return Time.millis() - startTime > time
    }
}
