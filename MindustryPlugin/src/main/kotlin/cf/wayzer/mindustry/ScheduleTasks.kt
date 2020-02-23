package cf.wayzer.mindustry

import arc.Core
import cf.wayzer.mindustry.util.ScheduleTask
import mindustry.Vars
import mindustry.core.GameState
import mindustry.gen.Call
import mindustry.io.SaveIO
import java.util.concurrent.TimeUnit

object ScheduleTasks {
    val autoSave = ScheduleTask<Unit> {
        if (!it && Vars.state.`is`(GameState.State.playing)) {
            val minute = ((timerTask!!.scheduledExecutionTime() / TimeUnit.MINUTES.toMillis(1)) % 60).toInt() //Get the minute
            Core.app.post {
                val id = Config.vote.savesRange.first + minute / 10
                SaveIO.save(SaveIO.fileFor(id))
                Helper.broadcast("[green]自动存档完成(10分钟一次),存档号 [red]$id")
            }
        }
        return@ScheduleTask Config.nextSaveTime
    }
    val alertTask = ScheduleTask<Int> { firstRun ->
        if (firstRun) {
            data = 0
        } else {
            data %= Config.base.alerts.size
            val msg = Config.base.alerts[data]
            if (Config.base.alertUseToast)
                Call.onInfoToast(msg, 15f)
            else
                Helper.broadcast(msg, true)
            data++
        }
        return@ScheduleTask Config.base.alertTime.delayToDate()
    }

    fun allStart() {
        autoSave.start()
        alertTask.start()
    }
}
