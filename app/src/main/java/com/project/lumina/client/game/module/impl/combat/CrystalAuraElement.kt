package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.R
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.util.AssetManager
import com.project.lumina.client.game.entity.*
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.math.vector.Vector3f
import kotlin.math.atan2
import kotlin.math.sqrt

class CrystalAuraElement(iconResId: Int = AssetManager.getAsset("ic_sword_cross_black_24dp")) : Element(
    name = "CrystalAura",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_crystal_aura_display_name")
) {

    private var rangeValue by floatValue("范围", 6.0f, 3f..10f)
    private var cpsValue by intValue("CPS", 15, 1..30)
    private var suicideValue by boolValue("自杀", false)

    private var lastAttackTime = 0L
    private var tickCounter = 0

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        if (interceptablePacket.packet !is PlayerAuthInputPacket) return

        val currentTime = System.currentTimeMillis()
        val attackDelay = 1000L / cpsValue

        if ((currentTime - lastAttackTime) >= attackDelay) {
            val crystals = searchForCrystals()

            for (crystal in crystals) {
                val damage = calculateCrystalDamage(crystal)

                if (damage > 0f && (suicideValue || damage < session.localPlayer.health)) {
                    spoofRotationTo(crystal)
                    session.localPlayer.attack(crystal)
                    lastAttackTime = currentTime
                    break
                }
            }
        }
    }

    private fun searchForCrystals(): List<EntityUnknown> {
        return session.level.entityMap.values
            .filterIsInstance<EntityUnknown>()
            .filter {
                it.identifier == "minecraft:end_crystal" &&
                        it.distance(session.localPlayer) <= rangeValue
            }
    }

    private fun calculateCrystalDamage(crystal: EntityUnknown): Float {
        val baseDamage = 6f
        return if (crystal.distance(session.localPlayer) <= rangeValue) baseDamage else 0f
    }

    private fun spoofRotationTo(target: Entity) {
        val player = session.localPlayer
        val dx = target.vec3Position.x - player.vec3Position.x
        val dy = target.vec3Position.y - player.vec3Position.y
        val dz = target.vec3Position.z - player.vec3Position.z

        val dist = sqrt(dx * dx + dz * dz)
        val yaw = Math.toDegrees(atan2(-dx.toDouble(), dz.toDouble())).toFloat()
        val pitch = Math.toDegrees(-atan2(dy.toDouble(), dist.toDouble())).toFloat()

        tickCounter = (tickCounter + 1) and 0xFFFF

        val spoofPacket = MovePlayerPacket().apply {
            runtimeEntityId = player.runtimeEntityId
            position = player.vec3Position
            rotation = Vector3f.from(yaw, pitch, 0f)
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = true
            tick = tickCounter.toLong()
        }

        session.clientBound(spoofPacket)
    }
}
