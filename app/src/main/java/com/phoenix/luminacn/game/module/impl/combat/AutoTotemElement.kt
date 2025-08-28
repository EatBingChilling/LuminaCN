package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.NetBound
import com.phoenix.luminacn.game.InterceptablePacket // InterceptablePacketをインポート
import com.phoenix.luminacn.game.event.EventHook
import com.phoenix.luminacn.game.event.EventTick
import com.phoenix.luminacn.game.inventory.PlayerInventory
import com.phoenix.luminacn.game.registry.itemDefinition
import com.phoenix.luminacn.util.AssetManager
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData

/**
 * AutoTotemElement is a module that automatically moves a Totem of Undying
 * from the player's main inventory to their offhand if the offhand slot is empty
 * or does not already contain a totem.
 *
 * Feature behavior is controlled by the `autoTotemEnabled` setting.
 * The module itself must also be enabled for the feature to work.
 */
class AutoTotemElement(iconResId: Int = AssetManager.getAsset("ic_heart_black_24dp")) : Element(
    name = "AutoTotem",
    category = CheatCategory.Combat,
    iconResId = iconResId,
    displayNameResId = AssetManager.getString("module_autototem_display_name") // TODO: Add "module_autototem_display_name" to strings.xml
) {
    val autoTotemEnabled by boolValue("AutoTotem", true)
    private var tickListener: EventHook<EventTick>? = null
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
    }
    override fun onEnabled() {
        super.onEnabled()
        if (autoTotemEnabled && isEnabled) {
            registerTickListener()
        }
    }
    override fun onDisabled() {
        super.onDisabled()
        unregisterTickListener()
    }
    private fun registerTickListener() {
        if (tickListener == null && isSessionCreated) {
            tickListener = EventHook<EventTick>(EventTick::class.java, { event: EventTick ->
                onTick(event)
            })
            session.eventManager.register(tickListener!!)
        }
    }
    private fun unregisterTickListener() {
        tickListener?.let {
            if (isSessionCreated) {
                session.eventManager.removeHandler(it)
            }
            tickListener = null
        }
    }
    private fun onTick(event: EventTick) {
        if (!isEnabled || !autoTotemEnabled || !isSessionCreated) return

        val player = session.localPlayer ?: return
        val inventory = player.inventory ?: return

        if (inventory.offhand.itemDefinition.identifier == "minecraft:totem_of_undying") {
            return
        }

        for (i in 0..35) {
            val itemInSlot = inventory.content[i]
            if (itemInSlot != null && itemInSlot != ItemData.AIR && itemInSlot.itemDefinition.identifier == "minecraft:totem_of_undying") {
                inventory.moveItem(
                    sourceSlot = i,
                    destinationSlot = PlayerInventory.SLOT_OFFHAND,
                    destinationInventory = inventory,
                    session = session
                )
                return
            }
        }
    }
}
