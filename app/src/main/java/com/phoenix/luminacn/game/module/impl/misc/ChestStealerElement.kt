package com.phoenix.luminacn.game.module.impl.misc

import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.util.AssetManager
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.inventory.ContainerInventory
import com.phoenix.luminacn.game.inventory.PlayerInventory
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket

class ChestStealerElement : Element(
    name = "ChestStealer",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_cheststealer_display_name"),
    iconResId = AssetManager.getAsset("ic_alien")
) {
    private val delayMs = 150L
    private var lastStealTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        if (packet is ContainerOpenPacket && packet.id.toInt() != 0) {
            stealItems()
        }
    }

    private fun stealItems() {
        val player = session?.localPlayer ?: return
        val container = player.openContainer as? ContainerInventory ?: return
        val inventory = player.inventory as? PlayerInventory ?: return

        if (System.currentTimeMillis() - lastStealTime < delayMs) return

        for (slot in container.content.indices) {
            val item = container.content[slot]
            if (item != ItemData.AIR) {
                val toSlot = inventory.findEmptySlot()
                if (toSlot != null) {
                    container.moveItem(slot, toSlot, inventory, session)
                    lastStealTime = System.currentTimeMillis()
                    return
                }
            }
        }

        session?.clientBound(ContainerClosePacket().apply {
            id = container.containerId.toByte()
            isServerInitiated = true
        })
    }
}
