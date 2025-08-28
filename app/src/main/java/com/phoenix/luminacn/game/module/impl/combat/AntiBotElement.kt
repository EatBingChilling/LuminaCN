package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.entity.Player
import com.phoenix.luminacn.game.entity.LocalPlayer
import com.phoenix.luminacn.util.AssetManager
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.module.api.setting.stringValue

class AntiBotElement(iconResId: Int = AssetManager.getAsset("ic_ghost_black_24dp")) : Element(
    name = "AntiBot",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_antibot_display_name")
) {
        private val mode by stringValue(this, "Mode", "Playerlist", listOf("Playlist", "Lifeboat"))

        override fun getStatusInfo(): String {
            return when (mode) {
                "Playerlist" -> "Playerlist"
                "Lifeboat" -> "Lifeboat"
                else -> mode
            }
        }

        fun Player.isBot(): Boolean {
            if (this is LocalPlayer) return false

            return when (mode) {
                "Lifeboat" -> {
                    val currentPlayers = GameManager.netBound?.getCurrentPlayers() ?: emptyList()
                    val realPlayerInfo = currentPlayers.find { it.uuid == this.uuid }
                    if (realPlayerInfo == null) return true
                    if (this.username != realPlayerInfo.name) return true
                    if (this.username.isBlank() != realPlayerInfo.name.isBlank()) return true
                    if (this.username.contains("\n") != realPlayerInfo.name.contains("\n")) return true
                    false
                }

                "PlayerList" -> {
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