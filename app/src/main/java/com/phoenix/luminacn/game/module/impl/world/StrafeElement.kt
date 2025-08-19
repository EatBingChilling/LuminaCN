package com.phoenix.luminacn.game.module.impl.world

import com.phoenix.luminacn.R
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.entity.Entity
import com.phoenix.luminacn.game.entity.EntityUnknown
import com.phoenix.luminacn.game.entity.LocalPlayer
import com.phoenix.luminacn.game.entity.Player
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import com.phoenix.luminacn.util.AssetManager

class StrafeElement(iconResId: Int = AssetManager.getAsset("ic_run_fast_black_24dp")) : Element(
    name = "Strafe",
    category = CheatCategory.World,
    iconResId,
    displayNameResId = AssetManager.getString("module_strafe_display_name")
) {
    private var targetRange by intValue("范围", 3, 2..20)
    private val movementSpeed by intValue("速度", 1, 1..20)
    private val circleRadius by intValue("环绕半径", 1, 1..5)
    private val offsetX by intValue("X 轴调整", 0, -8..8)
    private val offsetY by intValue("Y 轴调整", 0, -8..8)
    private val offsetZ by intValue("Z 轴调整", 0, -8..8)
    private var preserveY by boolValue("Y轴保持", true)
    private var faceTarget by boolValue("面向目标", true)
    private var playersOnly by boolValue("玩家", true)
    private var mobsOnly by boolValue("生物", false)

    private var strafeAngle = 0.0f
    private var lastTarget: Entity? = null

    fun onDisable() {
        lastTarget = null
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || interceptablePacket.packet !is PlayerAuthInputPacket) return

        val targets = findTargetsInRange()

        if (targets.isNotEmpty()) {
            val target = lastTarget?.let {
                if (it.distance(session.localPlayer) < targetRange && it.isValidTarget()) it else targets.first()
            } ?: targets.first()

            lastTarget = target
            strafeAroundTarget(target)
        } else {
            lastTarget = null
        }
    }

    private fun strafeAroundTarget(target: Entity) {
        val targetPos = target.vec3Position

        strafeAngle = (strafeAngle + movementSpeed) % 360f

        val angleRadians = Math.toRadians(strafeAngle.toDouble())
        val offsetXValue = circleRadius * cos(angleRadians).toFloat()
        val offsetZValue = circleRadius * sin(angleRadians).toFloat()

        val yPos = if (preserveY) {
            session.localPlayer.vec3Position.y
        } else {
            targetPos.y
        }

        val newPosition = Vector3f.from(
            targetPos.x + offsetXValue + offsetX,
            yPos + offsetY,
            targetPos.z + offsetZValue + offsetZ
        )

        val rotation = if (faceTarget) {
            calculateRotationToTarget(newPosition, targetPos)
        } else {
            session.localPlayer.vec3Rotation
        }

        val movePlayerPacket = MovePlayerPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            position = newPosition
            this.rotation = rotation
            mode = MovePlayerPacket.Mode.NORMAL
            onGround = true  
            ridingRuntimeEntityId = 0
            tick = session.localPlayer.tickExists
        }

        session.clientBound(movePlayerPacket)  
    }

    private fun calculateRotationToTarget(playerPos: Vector3f, targetPos: Vector3f): Vector3f {
        val deltaX = targetPos.x - playerPos.x
        val deltaZ = targetPos.z - playerPos.z

        val yaw = (Math.toDegrees(atan2(deltaZ, deltaX).toDouble()).toFloat() - 90f + 360f) % 360f

        return Vector3f.from(
            session.localPlayer.vec3Rotation.x,
            yaw,
            session.localPlayer.vec3Rotation.z
        )
    }

    private fun findTargetsInRange(): List<Entity> {
        return session.level.entityMap.values
            .filter { it.runtimeEntityId != session.localPlayer.runtimeEntityId }
            .filter { it.distance(session.localPlayer) < targetRange && it.isValidTarget() }
            .sortedBy { it.distance(session.localPlayer) }
    }

    private fun Entity.isValidTarget(): Boolean {
        return when (this) {
            is Player -> (playersOnly || (playersOnly && mobsOnly)) && !isBot()
            is EntityUnknown -> (mobsOnly || (playersOnly && mobsOnly)) && true  
            else -> false
        }
    }

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        val playerList = session.level.playerMap[this.uuid] ?: return true
        return playerList.name.isBlank()
    }
}