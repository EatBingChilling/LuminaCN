package com.project.luminacn.game.module.impl.motion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.util.AssetManager
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket

class MotionVarElement : Element("_var_", CheatCategory.Motion) {

    init {
        isEnabled = true
    }

    companion object {
        var lastUpdateAbilitiesPacket: UpdateAbilitiesPacket? by mutableStateOf(null)
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (interceptablePacket.packet is UpdateAbilitiesPacket) {
            lastUpdateAbilitiesPacket = interceptablePacket.packet
        }
    }

}