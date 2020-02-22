package cf.wayzer.mindustry.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.ClassContainer
import io.github.config4k.CustomType
import io.github.config4k.registerCustomType

object IntRangeReader : CustomType {
    override fun parse(clazz: ClassContainer, config: Config, name: String): Any? {
        val str = config.getString(name)
        val sp = str.split("..")
        if (sp.size == 2) {
            return sp[0].toInt()..sp[1].toInt()
        }
        throw Error("Error Range format:$str")
    }

    override fun testParse(clazz: ClassContainer): Boolean {
        return clazz.mapperClass == IntRange::class
    }

    override fun testToConfig(obj: Any): Boolean {
        return obj::class == IntRange::class
    }

    override fun toConfig(obj: Any, name: String): Config {
        return ConfigFactory.parseString("$name: $obj")
    }

    fun register() {
        registerCustomType(this)
    }
}
