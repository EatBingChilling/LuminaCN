package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.entity.Entity
import com.phoenix.luminacn.game.entity.LocalPlayer
import com.phoenix.luminacn.game.entity.*
import org.cloudburstmc.math.vector.Vector3f
import com.phoenix.luminacn.util.AssetManager
import com.phoenix.luminacn.R
import kotlin.math.*

class RotationAuraElement(iconResId: Int = AssetManager.getAsset("ic_sword_cross_black_24dp")) : Element(
    name = "RotationAura",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_rotationaura_display_name")
) {

    private var attackRange by floatValue("范围", 5.0f, 1.0f..10.0f)
    private var rotationSpeed by floatValue("速度", 2.0f, 1.0f..5.0f)

    private var lastAttackTime = 0L
    private var attackDelay = 1000L // Milliseconds between attacks (1 second)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        // Find the closest target within range
        val target = getClosestTarget() ?: return

        val currentTime = System.currentTimeMillis()

        // Check if enough time has passed since the last attack
        if (currentTime - lastAttackTime >= attackDelay) {
            // Rotate towards the target
            rotateTowardsTarget(target)

            // Perform the attack (simplified, add attack logic)
            attack(target)

            lastAttackTime = currentTime
        }
    }

    // Function to get the closest target (could be a player or mob)
    private fun getClosestTarget(): Entity? {
        return session.level.entityMap.values
            .filter { it != session.localPlayer && it.distance(session.localPlayer) <= attackRange && it.isTarget() }
            .sortedBy { it.distance(session.localPlayer) }
            .firstOrNull()
    }

    // Function to check if the entity is a valid target (player or mob)
    private fun Entity.isTarget(): Boolean {
        return when (this) {
            is LocalPlayer -> false // Don't target yourself
            is Entity -> true // You can expand this condition to filter mobs or players
            else -> false
        }
    }

    // Function to rotate smoothly towards the target
    private fun rotateTowardsTarget(target: Entity) {
        val targetPosition = target.vec3Position
        val playerPosition = session.localPlayer.vec3Position

        // Calculate the direction vector from the player to the target
        val direction = Vector3f.from(
            targetPosition.x - playerPosition.x,
            targetPosition.y - playerPosition.y,
            targetPosition.z - playerPosition.z
        )

        // Calculate the yaw (horizontal rotation)
        val yaw = Math.toDegrees(atan2(direction.z.toDouble(), direction.x.toDouble())).toFloat()

        // Calculate the pitch (vertical rotation)
        val pitch = -Math.toDegrees(atan2(direction.y.toDouble(), sqrt(direction.x * direction.x + direction.z * direction.z).toDouble())).toFloat()

        // Smoothly interpolate the yaw and pitch values
        val currentYaw = session.localPlayer.vec3Rotation.y
        val currentPitch = session.localPlayer.vec3Rotation.x
        val smoothYaw = interpolateRotation(currentYaw, yaw)
        val smoothPitch = interpolateRotation(currentPitch, pitch)


    }

    // Function to smoothly interpolate rotation values
    private fun interpolateRotation(current: Float, target: Float): Float {
        val delta = (target - current + 180) % 360 - 180 // Normalize to -180 to 180 range
        return current + delta * (rotationSpeed / 10.0f) // Control speed of rotation
    }

    // Function to perform an attack on the target (simplified attack logic)
    private fun attack(target: Entity) {
        // Simulate attacking the target (you can add logic to perform the actual attack)
        session.localPlayer.attack(target)

        // Optionally trigger a swing animation or other effects

    }
}
