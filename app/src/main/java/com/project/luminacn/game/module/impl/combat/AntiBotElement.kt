package com.project.luminacn.game.module.impl.combat

import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.constructors.GameManager
import com.project.luminacn.constructors.Element
import com.project.luminacn.game.entity.Player
import com.project.luminacn.game.entity.LocalPlayer
import com.project.luminacn.util.AssetManager
import com.project.luminacn.game.InterceptablePacket

class AntiBotElement(iconResId: Int = AssetManager.getAsset("ic_ghost_black_24dp")) : Element(
    name = "AntiBot",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_reach_display_name")
) {

    private var antiBotModeValue by intValue("Mode", 0, 0..1)

    fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false

        return when (antiBotModeValue) {
            0 -> {
                val currentPlayers = GameManager.netBound?.getCurrentPlayers() ?: emptyList()
                val realPlayerInfo = currentPlayers.find { it.uuid == this.uuid }
                if (realPlayerInfo == null) return true
                if (this.username != realPlayerInfo.name) return true
                if (this.username.isBlank() != realPlayerInfo.name.isBlank()) return true
                if (this.username.contains("\n") != realPlayerInfo.name.contains("\n")) return true
                false
            }
            1 -> {
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