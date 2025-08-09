package com.project.lumina.client.game.module.impl.misc

import android.os.Handler
import android.os.Looper
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.overlay.mods.ClientOverlay
import com.project.lumina.client.util.AssetManager

class WaterMarkElement(
    iconResId: Int = AssetManager.getAsset("ic_waterpolo")
) : Element(
    name = "WaterMark",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_watermark_display_name"),
    iconResId = iconResId
) {

    private val handler = Handler(Looper.getMainLooper())

    override fun onEnabled() {
        super.onEnabled()
        try {
            if (isSessionCreated) {
                ClientOverlay.setOverlayEnabled(true)
                ClientOverlay.showOverlay()
                handler.postDelayed({
                    ClientOverlay.showConfigDialog()
                }, 100)
            } else {
                println("Session not created, cannot enable WaterMark overlay.")
            }
        } catch (e: Exception) {
            println("Error enabling WaterMark overlay: ${e.message}")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        ClientOverlay.setOverlayEnabled(false)
        ClientOverlay.dismissOverlay()
        handler.removeCallbacksAndMessages(null)
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
    }
}
