package cf.wayzer.mindustry.util

import java.time.Duration
import kotlin.math.roundToInt

class DownTime(private val time: Long,
               startCallback: () -> Unit,
               progress: (minutes: Int) -> Boolean,
               end: () -> Unit) : ScheduleTask<Long>({ firstRun ->
    if (firstRun) {
        data = System.currentTimeMillis() + time // as endTime
        startCallback()
        ((time - 1) % (60 * 1000L)).delayToDate()
    } else {
        val min = ((data - System.currentTimeMillis()) / 60 / 1000.0).roundToInt()
        if (min <= 0) {
            end();null
        } else {
            progress(min)
            Duration.ofMinutes(1).delayToDate()
        }
    }
})
