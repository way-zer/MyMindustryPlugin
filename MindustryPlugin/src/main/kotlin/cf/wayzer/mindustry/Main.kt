package cf.wayzer.mindustry

import arc.Core
import arc.util.CommandHandler
import cf.wayzer.libraryManager.Dependency
import cf.wayzer.libraryManager.LibraryManager
import cf.wayzer.mindustry.util.IntRangeReader
import mindustry.Vars
import mindustry.plugin.Plugin
import java.nio.file.Paths
import java.util.*
import kotlin.concurrent.schedule

class Main : Plugin() {
    override fun init() {
        I18nHelper.init()
        IntRangeReader.register()
        Config.load()
        hackServerControl()
        Helper.setTeamAssigner()
        Listener.register()
        ScheduleTasks.allStart()
        if (Config.base.autoHost) {
            Helper.logToConsole("Auto Host after 5 seconds")
            timer.schedule(5000L) {
                if (!Vars.net.server()) Vars.netServer.openServer()
                Helper.loadMap()
            }
        }
    }

    override fun registerServerCommands(handler: CommandHandler) {
        ServerCommander.register(handler)
    }

    override fun registerClientCommands(handler: CommandHandler) {
        ClientCommander.register(handler)
    }

    private fun hackServerControl() {
        val obj = Core.app.listeners.find { it.javaClass.simpleName == "ServerControl" }
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
            LibraryManager(Paths.get("libs")).apply {
                addAliYunMirror()
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

                require(Dependency("io.github.config4k:config4k:0.4.2"))
                require(Dependency("com.typesafe:config:1.3.3"))
                require(Dependency("org.jetbrains.kotlin:kotlin-reflect:1.3.10"))

                loadToClassLoader(javaClass.classLoader)
            }
        }
    }
}
