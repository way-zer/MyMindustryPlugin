package cf.wayzer.mindustry.util

import cf.wayzer.mindustry.Main
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.concurrent.schedule

open class ScheduleTask<T : Any>(val task: ScheduleTask<T>.(first: Boolean) -> Date?) {
    //Null when first
    var timerTask: TimerTask? = null
    var params: Array<out Any> = emptyArray()

    //Any data you want save
    lateinit var data: T

    @Suppress("UNUSED_PARAMETER")
    private fun run(that: TimerTask) {
        timerTask = Main.timer.schedule(task(false) ?: let { return cancel() }, ::run)
    }

    fun start(vararg params: Any) {
        this.params = params
        timerTask?.cancel()
        timerTask = Main.timer.schedule(task(true) ?: let { return cancel() }, ::run)
    }

    fun cancel() {
        timerTask?.cancel()
        timerTask = null
    }

    fun Long.delayToDate(): Date {
        val now = Instant.now()
        return Date.from(now.plusMillis(this))
    }

    fun Duration.delayToDate(): Date {
        val now = Instant.now()
        return Date.from(now.plus(this))
    }
}
