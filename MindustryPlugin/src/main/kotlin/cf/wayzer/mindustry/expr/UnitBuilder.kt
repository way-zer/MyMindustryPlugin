package cf.wayzer.mindustry.expr

import mindustry.Vars
import mindustry.content.UnitTypes
import mindustry.entities.type.BaseUnit
import mindustry.entities.type.Player
import mindustry.entities.type.base.BuilderDrone
import mindustry.world.blocks.BuildBlock.BuildEntity

class UnitBuilder(private val targetPlayer: Player) : BuilderDrone() {
    override fun update() {
        //check before call super(prevent super change state)
        val check = !this.isBuilding && timer[BaseUnit.timerTarget2, 14.0f]
        super.update()
        if (targetPlayer.con == null) {
            return this.kill()
        }
        if (dead) {
            health = maxHealth()
            dead = false
        }
        if (check) {
            //reset state
            placeQueue.clear()
            setState(startState)
            target = targetPlayer

            targetPlayer.buildRequest()?.let {
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

    companion object {
        fun createForPlayer(p: Player) {
            val unit = UnitBuilder(p)
            unit.init(UnitTypes.phantom, p.team)
            unit.set(p.x, p.y)
            unit.add()
        }
    }
}
