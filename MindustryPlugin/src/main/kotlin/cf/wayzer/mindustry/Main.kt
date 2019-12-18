package cf.wayzer.mindustry

import cf.wayzer.libraryManager.Dependency
import cf.wayzer.libraryManager.LibraryManager
import io.anuke.arc.Core
import io.anuke.arc.util.CommandHandler
import io.anuke.mindustry.Vars
import io.anuke.mindustry.core.GameState
import io.anuke.mindustry.io.SaveIO
import io.anuke.mindustry.plugin.Plugin
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

class Main : Plugin() {
    override fun init() {
        Listener.register()
        hackServerControl()
        timer.schedule(Config.nextSaveTime, ::autoSave)
    }

    private fun autoSave(that: TimerTask) {
        if (Vars.state.`is`(GameState.State.playing)) {
            val minute = ((that.scheduledExecutionTime() / TimeUnit.MINUTES.toMillis(1)) % 60).toInt() //Get the minute
            Core.app.post {
                SaveIO.save(SaveIO.fileFor(Config.saveRange.first + minute / 10))
                Helper.broadcast("[green]自动存档完成(10分钟一次)")
            }
        }
        timer.schedule(Config.nextSaveTime, ::autoSave)
    }

    override fun registerServerCommands(handler: CommandHandler) {
        ServerCommander.register(handler)
    }

    override fun registerClientCommands(handler: CommandHandler) {
        ClientCommander.register(handler)
    }

    private fun hackServerControl() {
        val obj = Core.app.listeners.find { it.javaClass.name == "ServerControl" }
        val cls = obj.javaClass
        //Close Internal GameOverListener
        val field = cls.getDeclaredField("inExtraRound")
        field.isAccessible = true
        field.setBoolean(obj, true)
    }

    companion object {
        private const val kotlinVersion = "1.3.41"
        val timer = Timer(true)

        init {
            LibraryManager(Path.of("./libs")).apply {
                addJCenter()
                require(Dependency("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"))
                require(Dependency("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion"))
                require(Dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"))
                require(Dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion"))
                require(Dependency("org.jetbrains:annotations:13.0"))

                require(Dependency("org.mapdb:mapdb:3.0.7"))
                require(Dependency("org.eclipse.collections:eclipse-collections:10.1.0"))
                require(Dependency("org.eclipse.collections:eclipse-collections-api:10.1.0"))
                require(Dependency("org.eclipse.collections:eclipse-collections-forkjoin:10.1.0"))
                require(Dependency("com.google.guava:guava:28.1-jre"))
                require(Dependency("net.jpountz.lz4:lz4:1.3.0"))
                require(Dependency("org.mapdb:elsa:3.0.0-M5"))

                Loader.load(this)
            }
        }
    }
}
