package com.project.luminacn.game.module.impl.motion

import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.constructors.Element
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.game.entity.Entity
import com.project.luminacn.game.entity.LocalPlayer
import com.project.luminacn.game.entity.Player
import com.project.luminacn.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class FarSightElement(iconResId: Int = AssetManager.getAsset("ic_farsight")) : Element(
    name = "FarSight",
    category = CheatCategory.Motion,
    iconResId,
    displayNameResId = AssetManager.getString("module_farsight_display_name")
) {

    private val maxRange by floatValue("范围", 500f, 10f..500f)
    private val grabSpeed by floatValue("速度", 8.0f, 1f..50f)
    private val yOffset by floatValue("Y轴", 1.0f, -5f..5f)

    private val jitterEnabled by boolValue("抖动行走", true)
    private val derpEnabled by boolValue("陀螺", true)

    private var lastMoveTime = 0L
    private var derpYaw = 0f
    private var derpPitch = 0f
    private var derpTargetYaw = 0f
    private var derpTargetPitch = 0f
    private var lastDerpUpdate = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        val player = session.localPlayer
        val target = findNearestEnemy(player) ?: return

        val now = System.currentTimeMillis()
        if (now - lastMoveTime < 20L) return
        lastMoveTime = now

        val playerPos = player.vec3Position
        val targetPos = target.vec3Position.add(0f, yOffset, 0f)

        val dx = targetPos.x - playerPos.x
        val dy = targetPos.y - playerPos.y
        val dz = targetPos.z - playerPos.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (distance > maxRange) return

        val ratio = min(1.0f, grabSpeed / distance)

        var newY = playerPos.y + dy * ratio

        if (jitterEnabled) {
            val jitter = Random.nextFloat() * 0.5f + 0.5f
            newY += if (Random.nextBoolean()) jitter else -jitter
        }

        val newPos = Vector3f.from(
            playerPos.x + dx * ratio,
            newY,
            playerPos.z + dz * ratio
        )

        val rotation = if (derpEnabled) {
            if (now - lastDerpUpdate > 500L) {
                derpTargetYaw = Random.nextFloat() * 360f - 180f
                derpTargetPitch = Random.nextFloat() * 180f - 90f
                lastDerpUpdate = now
            }
            derpYaw += (derpTargetYaw - derpYaw) * 0.05f
            derpPitch += (derpTargetPitch - derpPitch) * 0.05f
            Vector3f.from(derpYaw.coerceIn(-180f, 180f), derpPitch.coerceIn(-90f, 90f), 0f)
        } else player.vec3Rotation

        session.clientBound(MovePlayerPacket().apply {
            runtimeEntityId = player.runtimeEntityId
            position = newPos
            this.rotation = rotation
            mode = MovePlayerPacket.Mode.NORMAL
            onGround = false
            ridingRuntimeEntityId = 0
            tick = player.tickExists
        })
    }

    private fun findNearestEnemy(player: LocalPlayer): Entity? {
        return session.level.entityMap.values
            .filter { it !== player && it is Player && !isBot(it) }
            .filter { it.vec3Position.distance(player.vec3Position) <= maxRange }
            .minByOrNull { it.vec3Position.distance(player.vec3Position) }
    }

    private fun isBot(entity: Entity): Boolean {
        if (entity !is Player || entity is LocalPlayer) return false
        val data = session.level.playerMap[entity.uuid]
        return data?.name.isNullOrEmpty()
    }
}