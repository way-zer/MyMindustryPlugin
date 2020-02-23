package cf.wayzer.mindustry.expr

import cf.wayzer.mindustry.Main
import cf.wayzer.mindustry.RuntimeData
import mindustry.Vars
import mindustry.content.UnitTypes
import mindustry.entities.type.BaseUnit
import mindustry.entities.type.Player
import mindustry.entities.type.base.BuilderDrone
import mindustry.world.blocks.BuildBlock.BuildEntity
import java.io.DataInput
import kotlin.concurrent.schedule

class UnitBuilder(private val bindPlayer: Player) : BuilderDrone() {
    override fun update() {
        //check before call super(prevent super change state)
        val check = !this.isBuilding && timer[BaseUnit.timerTarget2, 14.0f]
        playerTarget = bindPlayer
        super.update()
        if (bindPlayer.con == null) {
            return this.kill()
        }
        if (check) {
            //reset state
            placeQueue.clear()
            playerTarget = bindPlayer
            target = bindPlayer

            bindPlayer.buildRequest()?.let {
                val tile = Vars.world.tile(it.x, it.y)
                if (tile?.entity is BuildEntity) {
                    val b = tile.ent<BuildEntity>()
                    val dist = Math.min(b.dst(x, y) - 220.0f, 0.0f)
                    if (dist / type.maxVelocity < b.buildCost * 0.9f) {
                        lastFound = it
                        target = b
                        isBreaking = it.breaking
                        setState(build)
                    }
                }
            }
        }
    }

    override fun onDeath() {
        super.onDeath()
        bindPlayer.sendMessage("[yellow]建筑机将在一分钟后复活")
        Main.timer.schedule(60 * 1000L) {
            if (bindPlayer.con != null && RuntimeData.ensureNotChange(60 * 1000))
                createForPlayer(bindPlayer)
        }
    }

    override fun read(data: DataInput?) {
        super.read(data)
        this.dead = true
    }

    companion object {
        fun createForPlayer(p: Player) {
            val unit = UnitBuilder(p)
            unit.init(UnitTypes.phantom, p.team)
            unit.set(p.x, p.y)
            unit.add()
        }
    }
}
