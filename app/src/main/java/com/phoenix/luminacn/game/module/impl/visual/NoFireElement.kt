package com.phoenix.luminacn.game.module.impl.visual

import com.phoenix.luminacn.R
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.InterceptablePacket
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket
import com.phoenix.luminacn.util.AssetManager

class NoFireElement : Element(
    name = "NoFire",
    category = CheatCategory.Visual,
    displayNameResId = AssetManager.getString("module_nofire")
) {

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

       val packet = interceptablePacket.packet

        if(packet is SetEntityDataPacket){
            if(packet.runtimeEntityId == session.localPlayer.runtimeEntityId){
                if(packet.metadata.flags.contains(EntityFlag.ON_FIRE)){
                    packet.metadata.flags.remove(EntityFlag.ON_FIRE)
                }
            }
        }

        }



}