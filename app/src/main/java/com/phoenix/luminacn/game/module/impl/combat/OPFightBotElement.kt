package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.entity.Entity
import com.phoenix.luminacn.game.entity.Player
import com.phoenix.luminacn.game.module.api.setting.stringValue
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import com.phoenix.luminacn.util.AssetManager
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class OPFightBotElement(iconResId: Int = AssetManager.getAsset("ic_eye")) : Element(
    name = "OPFightBot(TEST)",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_opfightbot_display_name")
) {

    private var playersOnly by boolValue("仅玩家", false)
    private var filterInvisible by boolValue("过滤隐形", true)

    private var mode by stringValue("模式", "环绕", listOf("随机", "环绕", "锁背", "跳跃"))
    private var range by floatValue("范围", 1.5f, 1.5f..4.0f)
    private var passive by boolValue("被动", false)

    private var horizontalSpeed by floatValue("横向速度", 5.0f, 1.0f..7.0f)
    private var verticalSpeed by floatValue("纵向速度", 4.0f, 1.0f..7.0f)
    private var strafeSpeed by intValue("环绕速度", 20, 10..90)
    private var hopSpeedValue by floatValue("跳跃速度", 0.5f, 0.1f..2.0f)
    private var jumpValue by floatValue("跳跃高度", 0.32f, 0.1f..0.8f)
    private var offsetValue by floatValue("高度", 0f, -0.5f..0.5f)

    private var yoffset by floatValue("Y轴高度", 0.5f, -5.0f..5.0f)

    override fun getStatusInfo(): String {
        return when (mode) {
            "随机" -> "随机"
            "环绕" -> "环绕"
            "锁背" -> "锁背"
            "跳跃" -> "跳跃"
            else -> mode
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        if (interceptablePacket.packet !is PlayerAuthInputPacket) return

        val inputPacket = interceptablePacket.packet as PlayerAuthInputPacket

        val playerPos = session.localPlayer.vec3Position
        val target = session.level.entityMap.values
            .filter { it != session.localPlayer }
            .filter { !playersOnly || it is Player }
            .filter { !isEntityInvisible(it) }
            .minByOrNull {
                val dx = it.vec3Position.x - playerPos.x
                val dy = it.vec3Position.y - playerPos.y
                val dz = it.vec3Position.z - playerPos.z
                dx * dx + dy * dy + dz * dz
            } ?: return

        val distance = target.vec3Position.distance(playerPos)
        val targetPos = target.vec3Position

        if (distance < range) {
            val direction = Math.toRadians(when (mode) {
                "随机" -> Random.Default.nextDouble() * 360.0
                "环绕" -> ((session.localPlayer.tickExists * strafeSpeed) % 360).toDouble()
                "锁背" -> target.vec3Rotation.y + 180.0
                "跳跃" -> ((session.localPlayer.tickExists * strafeSpeed) % 360).toDouble()
                else -> 0.0
            }).toFloat()

            if (mode != "跳跃") {
                val newPos = Vector3f.from(
                    targetPos.x - sin(direction) * range,
                    targetPos.y + yoffset,
                    targetPos.z + cos(direction) * range
                )

                val yaw = atan2(targetPos.z - playerPos.z, targetPos.x - playerPos.x).toFloat() + Math.toRadians(90.0).toFloat()
                val pitch = -atan2(
                    targetPos.y - playerPos.y,
                    Vector3f.from(targetPos.x, playerPos.y, targetPos.z).distance(playerPos)
                )

                session.clientBound(MovePlayerPacket().apply {
                    runtimeEntityId = session.localPlayer.runtimeEntityId
                    position = newPos
                    rotation = Vector3f.from(pitch, yaw, yaw)
                    mode = MovePlayerPacket.Mode.NORMAL
                    onGround = true
                    tick = session.localPlayer.tickExists
                })
            } else {
                val isOnGround = session.localPlayer.isOnGround

                if (isOnGround) {
                    session.clientBound(SetEntityMotionPacket().apply {
                        runtimeEntityId = session.localPlayer.runtimeEntityId
                        motion = Vector3f.from(
                            (-sin(direction) * hopSpeedValue).toFloat(),
                            jumpValue,
                            (cos(direction) * hopSpeedValue).toFloat()
                        )
                    })
                } else {
                    val currentMotionY = session.localPlayer.motionY ?: 0f
                    session.clientBound(SetEntityMotionPacket().apply {
                        runtimeEntityId = session.localPlayer.runtimeEntityId
                        motion = Vector3f.from(
                            (-sin(direction) * hopSpeedValue).toFloat(),
                            currentMotionY - (offsetValue + 0.1265f),
                            (cos(direction) * hopSpeedValue).toFloat()
                        )
                    })
                }
            }
        } else if (!passive) {
            val direction = atan2(targetPos.z - playerPos.z, targetPos.x - playerPos.x) - Math.toRadians(90.0).toFloat()

            val newPos = Vector3f.from(
                playerPos.x - sin(direction) * horizontalSpeed,
                targetPos.y.coerceIn(playerPos.y - verticalSpeed, playerPos.y + verticalSpeed),
                playerPos.z + cos(direction) * horizontalSpeed
            )

            session.clientBound(MovePlayerPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                position = newPos
                rotation = session.localPlayer.vec3Rotation
                mode = MovePlayerPacket.Mode.NORMAL
                onGround = true
                tick = session.localPlayer.tickExists
            })
        }
    }

    private fun isEntityInvisible(entity: Entity): Boolean {
        if (!filterInvisible) return false

        if (entity.vec3Position.y < -30) return true

        val invisibilityFlag = entity.metadata[EntityDataTypes.FLAGS]?.let { flags ->
            if (flags is Long) {
                (flags and (1L shl 5)) != 0L
            } else false
        } ?: false

        if (invisibilityFlag) return true

        val name = entity.metadata[EntityDataTypes.NAME] as? String ?: ""
        if (name.contains("invisible", ignoreCase = true) || name.isEmpty()) return true

        return false
    }
}