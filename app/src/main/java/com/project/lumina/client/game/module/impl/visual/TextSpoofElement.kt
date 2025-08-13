package com.project.lumina.client.game.module.impl.visual

import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.protocol.bedrock.packet.TextPacket

class TextSpoofElement(iconResId: Int = AssetManager.getAsset("ic_script")) : Element(
    name = "TextSpoof",
    category = CheatCategory.Visual,
    iconResId,
    displayNameResId = AssetManager.getString("module_text_spoof_display_name")
) {

    private var oldTextValue: String = "steve"
    private var newTextValue: String = "LuminaCN User"

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is TextPacket) {
            if (packet.message.contains(oldTextValue)) {
                val newMessage = packet.message.replace(oldTextValue, newTextValue)

                
                val newPacket = TextPacket().apply {
                    type = packet.type
                    isNeedsTranslation = packet.isNeedsTranslation
                    sourceName = packet.sourceName
                    message = newMessage
                    xuid = packet.xuid
                    platformChatId = packet.platformChatId
                    filteredMessage = packet.filteredMessage
                }

                
                session.clientBound(newPacket)

                
                interceptablePacket.isIntercepted = true
            }
        }
    }
}