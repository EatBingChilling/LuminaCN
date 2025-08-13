package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.game.entity.Player
import com.project.lumina.client.game.entity.LocalPlayer
import com.project.lumina.client.util.AssetManager
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.module.api.setting.stringValue

class AntiBotElement(iconResId: Int = AssetManager.getAsset("ic_ghost_black_24dp")) : Element(
    name = "AntiBot",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_antibot_display_name")
) {
    private val mode by stringValue(this, "Mode", "Playerlist", listOf("Playlist", "Lifeboat"))

    fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false

        return when (mode) {
            "Playerlist" -> {
                val currentPlayers = GameManager.netBound?.getCurrentPlayers() ?: emptyList()
                val realPlayerInfo = currentPlayers.find { it.uuid == this.uuid }
                if (realPlayerInfo == null) return true
                if (this.username != realPlayerInfo.name) return true
                if (this.username.isBlank() != realPlayerInfo.name.isBlank()) return true
                if (this.username.contains("\n") != realPlayerInfo.name.contains("\n")) return true
                false
            }
            "Lifeboat" -> {
                val currentPlayers = GameManager.netBound?.getCurrentPlayers() ?: emptyList()
                val playerInfo = currentPlayers.find { it.uuid == this.uuid }
                playerInfo == null || playerInfo.name.isBlank()
            }
            else -> false
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
    }
}