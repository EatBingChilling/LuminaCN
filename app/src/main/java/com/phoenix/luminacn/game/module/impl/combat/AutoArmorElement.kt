package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.NetBound
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.event.EventPacketInbound
import com.phoenix.luminacn.game.event.EventPacketOutbound
import com.phoenix.luminacn.game.event.EventTick
import com.phoenix.luminacn.game.inventory.AbstractInventory
import com.phoenix.luminacn.game.inventory.PlayerInventory
import com.phoenix.luminacn.game.module.api.setting.stringValue
import com.phoenix.luminacn.game.registry.itemDefinition
import com.phoenix.luminacn.game.utils.constants.ItemTags
import com.phoenix.luminacn.game.utils.math.toVector3i
import com.phoenix.luminacn.util.AssetManager
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket

class AutoArmorElement(iconResId: Int = AssetManager.getAsset("ic_heart_black_24dp")) : Element(
    name = "AutoArmor",
    category = CheatCategory.Combat,
    iconResId = iconResId,
    displayNameResId = AssetManager.getString("module_autoarmor_display_name")
) {

    private var mode by stringValue(this, "Mode", AutoArmorMode.WITHOUT_OPENING.choiceName, AutoArmorMode.values().map { it.choiceName })
    private val cps by intValue("CPS", 5, 1..20)

    private var hasSimulated = false
    private var hasSimulatedWaitForClose = false
    private var lastActionTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {}

    private val armorSlots = arrayOf(
        ArmorSlot(PlayerInventory.SLOT_HELMET, ItemTags.TAG_IS_HELMET),
        ArmorSlot(PlayerInventory.SLOT_CHESTPLATE, ItemTags.TAG_IS_CHESTPLATE),
        ArmorSlot(PlayerInventory.SLOT_LEGGINGS, ItemTags.TAG_IS_LEGGINGS),
        ArmorSlot(PlayerInventory.SLOT_BOOTS, ItemTags.TAG_IS_BOOTS)
    )

    override fun onDisabled() {
        hasSimulated = false
        hasSimulatedWaitForClose = false
    }

    private val handleTick = handle<EventTick> { event ->
        // 使用父类的 isEnabled 属性替代已移除的 enabled
        if (!isEnabled) {
            return@handle
        }

        val currentTime = System.currentTimeMillis()
        val minDelay = 1000L / cps
        if (currentTime - lastActionTime < minDelay) {
            return@handle
        }

        val player = event.session.localPlayer
        val openContainer = player.openContainer

        val currentMode = AutoArmorMode.fromChoiceName(mode)

        when (currentMode) {
            AutoArmorMode.WITH_OPENING -> {
                if (openContainer == null) {
                    return@handle
                }

                if (equipBestArmor(player.inventory, event.session)) {
                    lastActionTime = currentTime
                }
            }

            AutoArmorMode.WITHOUT_OPENING -> {
                if (openContainer != null) {
                    if (equipBestArmor(player.inventory, event.session)) {
                        lastActionTime = currentTime
                    }
                } else {
                    if (checkFakeOpen(event.session)) {
                        return@handle
                    }

                    if (equipBestArmor(player.inventory, event.session)) {
                        lastActionTime = currentTime
                    }

                    // 使用正确的数据包发送方法
                    event.session.sendPacket(ContainerClosePacket().apply {
                        id = 0
                        isServerInitiated = false
                    })
                    hasSimulated = false
                    hasSimulatedWaitForClose = true
                }
            }
        }
    }

    private fun equipBestArmor(inventory: AbstractInventory, session: NetBound): Boolean {
        var actionPerformed = false

        armorSlots.forEach { armorSlot ->
            val currentItem = inventory.content[armorSlot.slot]
            val currentScore = if (currentItem != ItemData.AIR) {
                armorSlot.judge(currentItem)
            } else {
                0f
            }

            val bestSlot = inventory.findBestItem(armorSlot.slot) { item ->
                if (armorSlot.isItemValid(item)) {
                    armorSlot.judge(item)
                } else {
                    0f
                }
            }

            if (bestSlot != null && bestSlot != armorSlot.slot) {
                val bestItem = inventory.content[bestSlot]
                val bestScore = armorSlot.judge(bestItem)

                if (bestScore > currentScore) {
                    inventory.moveItem(bestSlot, armorSlot.slot, inventory, session)
                    actionPerformed = true
                }
            }
        }

        return actionPerformed
    }

    private fun checkFakeOpen(session: NetBound): Boolean {
        if (!hasSimulated && session.localPlayer.openContainer == null) {
            session.sendPacket(InteractPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                action = InteractPacket.Action.OPEN_INVENTORY
            })
            hasSimulated = true
            lastActionTime = System.currentTimeMillis()
            return true
        }
        return false
    }

    private val handlePacketInbound = handle<EventPacketInbound> { event ->
        val packet = event.packet

        if (hasSimulated && packet is ContainerOpenPacket && packet.id == 0.toByte()) {
            event.cancel()
        } else if (hasSimulatedWaitForClose && packet is ContainerClosePacket && packet.id == 0.toByte()) {
            event.cancel()
            hasSimulatedWaitForClose = false
        }
    }

    private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
        val packet = event.packet

        if (hasSimulated && packet is InteractPacket && packet.action == InteractPacket.Action.OPEN_INVENTORY) {
            hasSimulated = false
            event.cancel()
            event.session.sendPacket(ContainerOpenPacket().apply {
                id = 0.toByte()
                type = ContainerType.INVENTORY
                blockPosition = event.session.localPlayer.vec3Position.toVector3i()
                uniqueEntityId = event.session.localPlayer.uniqueEntityId
            })
        }
    }

    private data class ArmorSlot(
        val slot: Int,
        val tag: String,
        val judge: (ItemData) -> Float = { item ->
            val def = item.itemDefinition
            if (def.tags.contains(tag)) {
                def.getTier().toFloat()
            } else {
                0f
            }
        }
    ) {
        fun isItemValid(item: ItemData): Boolean {
            return item.itemDefinition.tags.contains(tag)
        }
    }

    private enum class AutoArmorMode(val choiceName: String) {
        WITH_OPENING("Vanilla"),
        WITHOUT_OPENING("Packet");

        companion object {
            fun fromChoiceName(name: String): AutoArmorMode {
                return values().firstOrNull { it.choiceName == name } ?: WITHOUT_OPENING
            }
        }
    }
}