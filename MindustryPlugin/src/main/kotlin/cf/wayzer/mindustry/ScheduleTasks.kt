package cf.wayzer.mindustry

import arc.Core
import cf.wayzer.i18n.I18nApi.i18n
import cf.wayzer.mindustry.util.ScheduleTask
import mindustry.Vars
import mindustry.core.GameState
import mindustry.io.SaveIO
import java.time.Duration
import java.util.concurrent.TimeUnit

object ScheduleTasks {
    val autoSave = ScheduleTask<Unit> {
        if (!it && Vars.state.`is`(GameState.State.playing)) {
            val minute = ((timerTask!!.scheduledExecutionTime() / TimeUnit.MINUTES.toMillis(1)) % 60).toInt() //Get the minute
            Core.app.post {
                val id = Config.vote.savesRange.first + minute / 10
                SaveIO.save(SaveIO.fileFor(id))
                Helper.broadcast("[green]自动存档完成(10分钟一次),存档号 [red]{id}".i18n("id" to id))
            }
        }
        return@ScheduleTask Config.nextSaveTime
    }
    val alertTask = ScheduleTask<Int> { firstRun ->
        if (firstRun) {
            data = 0
        } else if (Config.base.alerts.isNotEmpty()) {
            data %= Config.base.alerts.size
            val msg = Config.base.alerts[data]
            Helper.broadcast(msg.i18n(), Config.base.alertType, 15f)
            data++
        }
        return@ScheduleTask Config.base.alertTime.delayToDate()
    }

    val pvpProtectTask = ScheduleTask<Long> { firstRun ->
        val time = Config.base.pvpProtectTime.toMillis()
        if (firstRun) {
            data = System.currentTimeMillis() + 10000 //save startTime
            RuntimeData.pvpProtect = true
            Helper.broadcast("[yellow]PVP保护时间,禁止在其他基地攻击(持续{timeMin}分钟)".i18n("timeMin" to Config.base.pvpProtectTime.toMinutes()))
            return@ScheduleTask ((time - 1) % (60 * 1000L)).delayToDate()
        } else {
            if (RuntimeData.startTime > data) return@ScheduleTask null //map changed
            val min = Duration.ofMillis((time + 1) - (System.currentTimeMillis() - data)).toMinutes()
            if (min <= 0) {
                RuntimeData.pvpProtect = false
                Helper.broadcast("[yellow]PVP保护时间已结束, 全力进攻吧".i18n())
                return@ScheduleTask null
            }
            Helper.broadcast("[yellow]PVP保护时间还剩 {timeMin} 分钟".i18n("timeMin" to min), I18nHelper.MsgType.InfoToast, 10f, quite = true)
            Duration.ofMinutes(1).delayToDate()
        }
    }

    val runWaveTask = ScheduleTask<Array<Long>> { first ->
        //data format: leftWaves waitTime startTime
        if (first)
            data = arrayOf(((params.getOrNull(0) as? Int) ?: 10).toLong(),
                    3, System.currentTimeMillis())
        //Have change map
        if (RuntimeData.startTime > data[2]) return@ScheduleTask null
        //完成或等待超时
        if (data[0] < 0 || data[1] > 60) return@ScheduleTask null
        if (Vars.state.enemies < 300) {
            data[0]--
            Core.app.post { Vars.logic.runWave() }
            return@ScheduleTask Duration.ofSeconds(data[1]).delayToDate()
        }
        //延长等待时间
        val time = data[1]
        data[1] = data[1] * 2
        return@ScheduleTask Duration.ofSeconds(time).delayToDate()
    }

    fun allStart() {
        autoSave.start()
        alertTask.start()
    }
}
