package com.project.lumina.client.game.module.impl.world

import com.project.lumina.client.R
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AutoNavigatorElement : Element(
    name = "AutoNavigator",
    category = CheatCategory.World,
    displayNameResId = R.string.module_autonavigator_display_name
) {

    private var targetX = 0.0
    private var targetY = 0.0
    private var targetZ = 0.0
    private var isPathing = false
    private var lastJumpTime = 0L
    private var lastUpdateTime = 0L
    private var lastMotionTime = 0L
    private val jumpCooldown = 500L
    private val updateInterval = 2000L 
    private val motionInterval = 100L 

    private val walkSpeed by floatValue("速度", 0.6f, 0.1f..2.0f)
    private val jumpHeight by floatValue("跳跃高度", 0.42f, 0.1f..1.0f)
    private val distanceThreshold by floatValue("完成距离", 1.0f, 0.5f..5.0f)
    private val verticalTolerance by floatValue("宽容", 1.0f, 0.1f..3.0f)
    private val obstacleCheckRange by floatValue("障碍范围", 0.5f, 0.1f..2.0f)

    override fun onEnabled() {
        super.onEnabled()
        if (isSessionCreated) {
            session.displayClientMessage(
                """
                    §l§b[自动导航] §r§7指令:
                    §f.goto <x> <y> <z> §7来自动导航到你的指定位置
                    §f.stop §7来结束它
               """.trimIndent()
            )
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if(!isEnabled) return
        val packet = interceptablePacket.packet

        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            val message = packet.message

            when {
                message.startsWith(".goto") -> {
                    interceptablePacket.intercept() 
                    handleGotoCommand(message)
                }
                message.startsWith(".stop") -> {
                    interceptablePacket.intercept() 
                    isPathing = false
                    session.displayClientMessage("§l§b[自动导航] §r§a导航结束!")
                }
            }
        }

        if (!isEnabled || !isPathing) return

        if (packet is PlayerAuthInputPacket) {
            val player = session.localPlayer
            val currentPos = player.vec3Position
            val currentTime = System.currentTimeMillis()
            
            val horizontalDistance = sqrt((currentPos.x - targetX) * (currentPos.x - targetX) + (currentPos.z - targetZ) * (currentPos.z - targetZ))
            val verticalDistance = kotlin.math.abs(currentPos.y - targetY)
            if (horizontalDistance <= distanceThreshold && verticalDistance <= verticalTolerance) {
                isPathing = false
                session.displayClientMessage("§l§b[自动导航] §r§a已到达 ${targetX.toInt()}, ${targetY.toInt()}, ${targetZ.toInt()}!")
                return
            }
            
            if (currentTime - lastUpdateTime >= updateInterval) {
                val remainingDistance = sqrt(
                    (currentPos.x - targetX) * (currentPos.x - targetX) +
                            (currentPos.y - targetY) * (currentPos.y - targetY) +
                            (currentPos.z - targetZ) * (currentPos.z - targetZ)
                )
                session.displayClientMessage("§l§b[自动导航] §r§7距离剩余: §f${"%.1f".format(remainingDistance)} 个方块")
                lastUpdateTime = currentTime
            }

            if (currentTime - lastMotionTime >= motionInterval) {
                
                val dx = targetX - currentPos.x
                val dz = targetZ - currentPos.z
                val angle = -Math.toDegrees(atan2(dx, dz)).toFloat()

                packet.rotation = Vector3f.from(
                    player.rotationPitch, 
                    angle,
                    0f 
                )

                val adjustedSpeed = walkSpeed.toDouble() * (1.0 + (horizontalDistance / 10.0).coerceAtMost(1.0))
                val motionX = -sin(Math.toRadians(angle.toDouble())) * adjustedSpeed
                val motionZ = cos(Math.toRadians(angle.toDouble())) * adjustedSpeed

                val shouldJump = packet.inputData.contains(PlayerAuthInputData.VERTICAL_COLLISION) &&
                        verticalDistance > 0.5 &&
                        currentPos.y < targetY + verticalTolerance &&
                        System.currentTimeMillis() - lastJumpTime >= jumpCooldown

                val motionY = when {
                    shouldJump -> {
                        lastJumpTime = System.currentTimeMillis()
                        jumpHeight.toDouble()
                    }
                    packet.inputData.contains(PlayerAuthInputData.VERTICAL_COLLISION) -> 0.0
                    else -> -0.08
                }

                
                val motionPacket = SetEntityMotionPacket().apply {
                    runtimeEntityId = player.runtimeEntityId
                    motion = Vector3f.from(motionX, motionY, motionZ)
                }
                session.clientBound(motionPacket)

                lastMotionTime = currentTime
            }
        }
    }

    private fun handleGotoCommand(message: String) {
        val args = message.split(" ")
        if (args.size != 4) {
            session.displayClientMessage("§l§b[自动导航] §r§c用法: .goto <x> <y> <z>")
            return
        }

        try {
            targetX = args[1].toDouble()
            targetY = args[2].toDouble()
            targetZ = args[3].toDouble()
            isPathing = true
            isEnabled = true
            lastUpdateTime = System.currentTimeMillis() 
            session.displayClientMessage(
                "§l§b[自动导航] §r§7正在行走至 §f${targetX.toInt()} ${targetY.toInt()} ${targetZ.toInt()}"
            )
        } catch (e: NumberFormatException) {
            session.displayClientMessage("§l§b[自动导航] §r§c非法的坐标")
        }
    }
}