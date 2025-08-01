package com.project.luminacn.game.module.impl.visual

import com.project.luminacn.R
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.CheatCategory
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket
import com.project.luminacn.util.AssetManager

class NoHurtCameraElement(iconResId: Int = AssetManager.getAsset("ic_creeper_black_24dp")) : Element(
    name = "NoHurtCam",
    category = CheatCategory.Visual,
    iconResId,
    displayNameResId = AssetManager.getString("module_no_hurt_camera_display_name")
) {

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet
        if (packet is EntityEventPacket) {
            if (packet.runtimeEntityId == session.localPlayer.runtimeEntityId
                && packet.type == EntityEventType.HURT
            ) {
                interceptablePacket.intercept()
            }
        }
    }

}