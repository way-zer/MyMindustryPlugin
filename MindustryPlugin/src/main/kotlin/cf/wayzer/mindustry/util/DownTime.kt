package cf.wayzer.mindustry.util

import cf.wayzer.mindustry.Config
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToInt

class DownTime(private val timer: Timer, private val time:Long,
               private val startCallback:()->Unit, val progress:(minutes:Int)->Boolean, val end:()->Unit) {
    private var endTime :Long = 0
    private var task:TimerTask? = null

    private fun task(@Suppress("UNUSED_PARAMETER") that: TimerTask){
        val min = ((endTime - System.currentTimeMillis())/60/1000.0).roundToInt()
        if(min<=0)return end()
        if(progress(min)&&min>=1)task=timer.schedule(60*1000L,::task)
    }
    fun start(){
        task?.cancel()
        endTime = System.currentTimeMillis()+time
        timer.schedule(1000L){startCallback()}
        task=timer.schedule((Config.pvpProtectTime-1)%(60*1000),::task)
    }

    fun cancel(){
        task?.cancel()
    }
}
