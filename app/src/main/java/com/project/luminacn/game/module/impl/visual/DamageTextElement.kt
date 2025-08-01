package com.project.luminacn.game.module.impl.visual

import com.project.luminacn.R
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.constructors.Element
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.game.entity.Player
import com.project.luminacn.util.AssetManager
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket

class DamageTextElement(
    iconResId: Int = ir.alirezaivaz.tablericons.R.drawable.ic_text_plus
) : Element(
    name = "DamageText",
    category = CheatCategory.Visual,
    iconResId,
    displayNameResId = AssetManager.getString("module_damage_text_display_name")
) {

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is EntityEventPacket && packet.type == EntityEventType.HURT) {
            val entityId = packet.runtimeEntityId

            if (entityId == session.localPlayer.runtimeEntityId) return

            val entity = session.level.entityMap[entityId]
            if (entity is Player) {
                val name = entity.username
                val status = "§f$name§r §c敌人攻击"
                session.displayClientMessage(" $status")
            }
        }
    }
}