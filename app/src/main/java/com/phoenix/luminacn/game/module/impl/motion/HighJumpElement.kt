package com.phoenix.luminacn.game.module.impl.motion

import com.phoenix.luminacn.R
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket


class HighJumpElement(iconResId: Int = AssetManager.getAsset("ic_chevron_double_up_black_24dp")) : Element(
    name = "HighJump",
    category = CheatCategory.Motion,
    iconResId,
    displayNameResId = AssetManager.getString("module_high_jump_display_name")
) {

    private var jumpHeight by floatValue("跳跃高度", 1f, 0.5f..5f)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {

            if (packet.inputData.contains(PlayerAuthInputData.VERTICAL_COLLISION)) {
                if (packet.inputData.contains(PlayerAuthInputData.JUMP_DOWN)) {
                    val motionPacket = SetEntityMotionPacket().apply {
                        runtimeEntityId = session.localPlayer.runtimeEntityId
                        motion = Vector3f.from(
                            session.localPlayer.motionX,
                            jumpHeight,
                            session.localPlayer.motionZ
                        )
                    }
                    session.clientBound(motionPacket)
                }
            }
        }
    }
}