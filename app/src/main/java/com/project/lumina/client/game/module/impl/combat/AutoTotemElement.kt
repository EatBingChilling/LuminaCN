package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.NetBound
import com.project.lumina.client.game.InterceptablePacket // InterceptablePacketをインポート
import com.project.lumina.client.game.event.EventHook
import com.project.lumina.client.game.event.EventTick
import com.project.lumina.client.game.inventory.PlayerInventory
import com.project.lumina.client.game.registry.itemDefinition
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData

/**
 * AutoTotemElement is a module that automatically moves a Totem of Undying
 * from the player's main inventory to their offhand if the offhand slot is empty
 * or does not already contain a totem.
 *
 * Feature behavior is controlled by the `autoTotemEnabled` setting.
 * The module itself must also be enabled for the feature to work.
 */
class AutoTotemElement(iconResId: Int = AssetManager.getAsset("ic_placeholder_black_24dp")) : Element( // TODO: Replace placeholder icon with a proper totem icon asset
    name = "AutoTotem",
    category = CheatCategory.Combat,
    iconResId = iconResId,
    displayNameResId = AssetManager.getString("module_autototem_display_name") // TODO: Add "module_autototem_display_name" to strings.xml
) {
    /**
     * Boolean setting to enable or disable the AutoTotem functionality.
     * Defaults to true (enabled). This setting is exposed in the ClickGUI.
     */
    val autoTotemEnabled by boolValue("AutoTotem", true)

    private var tickListener: EventHook<EventTick>? = null

    /**
     * Called before a packet is bound to the client.
     * This module does not need to intercept or modify packets, so this is an empty implementation.
     */
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        // No packet manipulation needed for AutoTotem
    }

    /**
     * Called when the module is enabled.
     * Registers the tick listener if AutoTotem functionality is also enabled.
     */
    override fun onEnabled() {
        super.onEnabled()
        if (autoTotemEnabled && isEnabled) { // Ensure both module and specific setting are active
            registerTickListener()
        }
    }

    /**
     * Called when the module is disabled.
     * Unregisters the tick listener.
     */
    override fun onDisabled() {
        super.onDisabled()
        unregisterTickListener()
    }

    // onValueChangedでエラーが出ているため、一旦コメントアウト。
    // ElementクラスのValue変更通知の仕組みを再確認する必要がある。
    // もしConfigurableインターフェースの onValueChanged(Value<*>) が意図したものであれば、
    // Elementクラスのオーバーライド可能なメソッドシグネチャを正確に確認する。
    // 現状は onEnabled / onDisabled と autoTotemEnabled の組み合わせでリスナー管理を行う。
//    /**
//     * Called when a setting value for this module changes in the GUI.
//     * If the 'AutoTotem' setting is changed, it registers or unregisters the
//     * tick listener accordingly, provided the module itself is enabled.
//     */
//    override fun onValueChanged(value: com.project.lumina.client.constructors.Value<*>) {
//        super.onValueChanged(value) // This might be the unresolved reference if Element doesn't have it
//        if (value.name == "AutoTotem") {
//            if (isEnabled) { // Only act if the module itself is enabled
//                if (autoTotemEnabled) {
//                    registerTickListener()
//                } else {
//                    unregisterTickListener()
//                }
//            }
//        }
//    }

    /**
     * Registers the tick listener if not already registered and a session exists.
     * The listener calls `onTick()` every game tick.
     */
    private fun registerTickListener() {
        if (tickListener == null && isSessionCreated) {
            // EventHookの型引数を明示的に指定
            tickListener = EventHook<EventTick>(EventTick::class.java, { event: EventTick ->
                onTick(event)
            })
            session.eventManager.register(tickListener!!)
        }
    }

    /**
     * Unregisters the tick listener if it is currently registered and a session exists.
     */
    private fun unregisterTickListener() {
        tickListener?.let {
            if (isSessionCreated) {
                session.eventManager.removeHandler(it)
            }
            tickListener = null
        }
    }

    /**
     * Called on every game tick when the listener is active.
     * Checks if a totem needs to be moved to the offhand and performs the action.
     */
    private fun onTick(event: EventTick) {
        // Ensure module is enabled, AutoTotem setting is on, and session is active
        if (!isEnabled || !autoTotemEnabled || !isSessionCreated) return

        val player = session.localPlayer ?: return
        val inventory = player.inventory ?: return

        // If offhand already has a totem, no action needed
        if (inventory.offhand.itemDefinition.identifier == "minecraft:totem_of_undying") {
            return
        }

        // Search for a totem in the main inventory slots (0-35)
        for (i in 0..35) { // Main inventory includes hotbar (0-8) and main storage (9-35)
            val itemInSlot = inventory.content[i]
            // Check if the slot is not empty and contains a totem
            if (itemInSlot != null && itemInSlot != ItemData.AIR && itemInSlot.itemDefinition.identifier == "minecraft:totem_of_undying") {
                // Found a totem, move it to the offhand slot
                inventory.moveItem(
                    sourceSlot = i,
                    destinationSlot = PlayerInventory.SLOT_OFFHAND, // Defined as 40 in PlayerInventory
                    destinationInventory = inventory, // Moving within the same player inventory
                    session = session
                )
                // Totem moved, no need to continue searching in this tick
                return
            }
        }
    }
}
