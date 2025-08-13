package com.project.lumina.client.game.module.impl.motion

import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class AirJumpElement(iconResId: Int = AssetManager.getAsset("ic_cloud_upload_black_24dp")) : Element(
    name = "AirJump",
    category = CheatCategory.Motion,
    iconResId,
    displayNameResId = AssetManager.getString("module_air_jump_display_name")
) {
    private var mode by intValue("模式", 0, 0..1)
    
    private var jumpValue by floatValue("跳跃", 0.42f, 0.1f..3f)
    private var speedMultiplierValue by floatValue("速度调整", 1f, 0.5f..3f)
    private var speedBoostValue by boolValue("速度增益", false)
    private var jumpTriggered = false

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        when (mode) {
            0 -> method0(interceptablePacket)
            1 -> method1(interceptablePacket)
        }
    }

    private fun method0(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val isJumpPressed = packet.inputData.contains(PlayerAuthInputData.JUMP_DOWN)
            val player = session.localPlayer

            if (isJumpPressed && !jumpTriggered && !player.isOnGround) {
                val motionPacket = SetEntityMotionPacket().apply {
                    runtimeEntityId = player.runtimeEntityId
                    motion = if (speedBoostValue) {
                        Vector3f.from(
                            player.motionX * speedMultiplierValue,
                            jumpValue,
                            player.motionZ * speedMultiplierValue
                        )
                    } else {
                        Vector3f.from(
                            player.motionX,
                            jumpValue,
                            player.motionZ
                        )
                    }
                }
                session.clientBound(motionPacket)
                jumpTriggered = true
            } else if (!isJumpPressed) {
                jumpTriggered = false
            }
        }
    }

    private fun method1(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            if (packet.inputData.contains(PlayerAuthInputData.JUMP_DOWN)) {
                val motionPacket = SetEntityMotionPacket().apply {
                    runtimeEntityId = session.localPlayer.runtimeEntityId
                    motion = Vector3f.from(
                        session.localPlayer.motionX,
                        0.42f,
                        session.localPlayer.motionZ
                    )
                }
                session.clientBound(motionPacket)
            }
        }
    }
}
