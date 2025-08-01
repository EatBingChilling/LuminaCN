package com.project.luminacn.game.module.impl.combat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.constructors.Element
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.game.entity.Entity
import com.project.luminacn.game.entity.LocalPlayer
import com.project.luminacn.game.entity.Player
import com.project.luminacn.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LockHeedElement(iconResId: Int = AssetManager.getAsset("ic_lockheed")) : Element(
    name = "LockHeed",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_lock_heed_display_name")
) {

    private val maxRange by floatValue("范围", 500f, 50f..500f)
    private val baseSpeed by floatValue("速度", 1.5f, 0.5f..25.0f)
    private val jitterPower by floatValue("抖动", 0.1f, 0f..0.5f)
    private val strafeRadius by floatValue("半径", 2f, 0f..10f)
    private val cps by intValue("CPS", 20, 1..30)
    private val packetsPerAttack by intValue("发包", 3, 1..5)
    private val yOffset by floatValue("Y轴", 0.0f, -10.0f..10.0f)
    private val noClip by boolValue("穿墙", false)
    private var isPathBlocked by mutableStateOf(false)
    private var lastMoveTime = 0L
    private var lastAttackTime = 0L
    private var angle = 0.0

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        val now = System.currentTimeMillis()
        val moveDelta = now - lastMoveTime
        val attackDelta = now - lastAttackTime

        val player = session.localPlayer
        val target = findNearestTarget(player) ?: return


        if (attackDelta >= (1000 / cps)) {
            repeat(packetsPerAttack) {
                player.attack(target)
            }
            lastAttackTime = now
        }

        if (moveDelta < 20L) return
        lastMoveTime = now

        val playerPos = player.vec3Position
        val targetPos = target.vec3Position

        val dx = targetPos.x - playerPos.x
        val dy = (targetPos.y + yOffset) - playerPos.y
        val dz = targetPos.z - playerPos.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        val speedScale = baseSpeed + (distance / 3.5f)

        val moveVec = if (distance > 4) {
            val direction = Vector3f.from(dx, dy, dz).normalize()
            direction.mul(speedScale).add(jitterVec())
        } else {
            angle = (angle + speedScale * 40) % 360
            val rad = Math.toRadians(angle)
            val offsetX = cos(rad) * strafeRadius
            val offsetZ = sin(rad) * strafeRadius
            Vector3f.from(
                offsetX.toFloat() + jitter(),
                jitter(),
                offsetZ.toFloat() + jitter()
            )
        }

        val motion = if (player.vec3Position.y < 0.5f) {
            Vector3f.from(0f, 1.2f, 0f) // anti-void jump
        } else moveVec

        val newPos = player.vec3Position.add(motion)


        if(packet is PlayerAuthInputPacket){
            if (packet.inputData.contains(PlayerAuthInputData.HORIZONTAL_COLLISION)) {

               isPathBlocked = true

            }

            isPathBlocked = false

        }

        if (!noClip && isPathBlocked) return

        session.clientBound(MovePlayerPacket().apply {
            runtimeEntityId = player.runtimeEntityId
            position = newPos
            rotation = player.vec3Rotation
            mode = MovePlayerPacket.Mode.NORMAL
            onGround = false
            ridingRuntimeEntityId = 0
            tick = player.tickExists
        })
    }

    private fun jitter(): Float =
        ((Math.random() - 0.5) * 2 * jitterPower).toFloat()

    private fun jitterVec(): Vector3f =
        Vector3f.from(jitter(), jitter(), jitter())

    private fun findNearestTarget(player: LocalPlayer): Entity? {
        return session.level.entityMap.values
            .filter { it is Player && it != player && !isBot(it) }
            .filter { it.vec3Position.distance(player.vec3Position) <= maxRange }
            .minByOrNull { it.vec3Position.distance(player.vec3Position) }
    }

    private fun isBot(entity: Entity): Boolean {
        if (entity !is Player || entity is LocalPlayer) return false
        val data = session.level.playerMap[entity.uuid]
        return data?.name.isNullOrEmpty()
    }


}