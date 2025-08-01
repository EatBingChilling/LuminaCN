package com.project.luminacn.game.module.impl.motion

import com.project.luminacn.R
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.util.AssetManager
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class SprintElement(iconResId: Int = AssetManager.getAsset("ic_run_fast_black_24dp")) : Element(
    name = "Sprint",
    category = CheatCategory.Motion,
    iconResId,
    displayNameResId = AssetManager.getString("module_sprint_display_name")
) {
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet

        if (packet is PlayerAuthInputPacket) {
            val inputData = packet.inputData as MutableSet<PlayerAuthInputData>
            
            if (isEnabled) {
                inputData.add(PlayerAuthInputData.SPRINTING)
                inputData.add(PlayerAuthInputData.START_SPRINTING)
            } else {
                inputData.add(PlayerAuthInputData.STOP_SPRINTING)
            }
        }
    }
}