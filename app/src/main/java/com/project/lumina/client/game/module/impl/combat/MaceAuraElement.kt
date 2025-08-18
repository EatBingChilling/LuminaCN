/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * This is open source — not open credit.
 *
 * If you're here to build, welcome. If you're here to repaint and reupload
 * with your tag slapped on it… you're not fooling anyone.
 *
 * Changing colors and class names doesn't make you a developer.
 * Copy-pasting isn't contribution.
 *
 * You have legal permission to fork. But ask yourself — are you improving,
 * or are you just recycling someone else's work to feed your ego?
 *
 * Open source isn't about low-effort clones or chasing clout.
 * It's about making things better. Sharper. Cleaner. Smarter.
 *
 * So go ahead, fork it — but bring something new to the table,
 * or don't bother pretending.
 *
 * This message is philosophical. It does not override your legal rights under GPLv3.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * GPLv3 Summary:
 * - You have the freedom to run, study, share, and modify this software.
 * - If you distribute modified versions, you must also share the source code.
 * - You must keep this license and copyright intact.
 * - You cannot apply further restrictions — the freedom stays with everyone.
 * - This license is irrevocable, and applies to all future redistributions.
 *
 * Full text: https://www.gnu.org/licenses/gpl-3.0.html
 */

package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.R
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.game.entity.Entity
import com.project.lumina.client.game.entity.EntityUnknown
import com.project.lumina.client.game.entity.LocalPlayer
import com.project.lumina.client.game.entity.Player
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import android.util.Log
import kotlin.math.ceil
import com.project.lumina.client.util.AssetManager

class MaceAuraElement(iconResId: Int = AssetManager.getAsset("ic_cube_scan_black_24dp")) : Element(
    name = "MaceAura",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_maceaura_display_name")
) {
    
    private var radius by floatValue("半径", 5.0f, 0.1f..10.0f)
    private var height by floatValue("高度", 10.0f, 1.0f..30.0f)
    private var targetAnimals by boolValue("生物", false)
    private var targetMonsters by boolValue("敌对生物", true)
    private var targetPlayers by boolValue("玩家", true)
    private var targetFriends by boolValue("白名单", false)
    private var attackDelay by intValue("间隔", 1200, 500..2000) 

    
    private var lastAttackTime = 0L
    private var maceStep = 0 
    private var targetEntity: Entity? = null
    private var packetsSent = 0
    private var packetsRequired = 0

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAttackTime < attackDelay) return

        
        val hand = session.localPlayer.inventory.hand
        if (packet.inputData.contains(PlayerAuthInputData.START_SWIMMING) ||
            kotlin.math.abs(packet.delta.y) > 0.1f ||
            hand == ItemData.AIR ||
            !isMace(hand)
        ) {
            
            if (hand != ItemData.AIR) {
                Log.d("MaceAura", "Hand: netId=${hand.netId}, damage=${hand.damage}, count=${hand.count}")
            }
            return
        }

        if (maceStep == 0) {
            
            targetEntity = findTargetsInRange().firstOrNull()
            if (targetEntity == null) return

            
            packetsRequired = ceil(height / 10.0f).toInt()
            packetsSent = 0

            
            sendMovePacket(height)
            maceStep = 1
        } else if (maceStep == 1) {
            
            if (packetsSent < packetsRequired) {
                sendMovePacket(height, onGround = false)
                packetsSent++
            } else {
                
                sendMovePacket(0f)
                maceStep = 2
            }
        } else if (maceStep == 2) {
            
            targetEntity?.let { session.localPlayer.attack(it) }
            targetEntity = null
            maceStep = 0
            lastAttackTime = currentTime
            packetsSent = 0
            packetsRequired = 0
        }
    }

    private fun sendMovePacket(yOffset: Float, onGround: Boolean = true) {
        val currentPos = session.localPlayer.vec3Position
        val newPosition = Vector3f.from(
            currentPos.x,
            currentPos.y + yOffset,
            currentPos.z
        )

        val movePlayerPacket = MovePlayerPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            position = newPosition
            rotation = session.localPlayer.vec3Rotation
            mode = MovePlayerPacket.Mode.NORMAL
            this.onGround = onGround
            ridingRuntimeEntityId = 0
            tick = session.localPlayer.tickExists
        }

        session.clientBound(movePlayerPacket)
    }

    private fun findTargetsInRange(): List<Entity> {
        return session.level.entityMap.values
            .filter { entity ->
                entity.distance(session.localPlayer) <= radius &&
                        entity.isTarget()
            }
            .sortedBy { it.distance(session.localPlayer) }
    }

    private fun Entity.isTarget(): Boolean {
        return when {
            this is LocalPlayer -> false
            this is Player -> {
                if (!targetPlayers) false
                else if (!targetFriends) {
                    
                    !isBot()
                } else true
            }
            this is EntityUnknown -> {
                when {
                    targetMonsters && this.isHostile() -> true
                    targetAnimals && this.isPassive() -> true
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        val playerList = session.level.playerMap[this.uuid] ?: return true
        return playerList.name.isBlank()
    }

    private fun Entity.isHostile(): Boolean {
        val hostileIdentifiers = listOf(
            "minecraft:zombie", "minecraft:husk", "minecraft:drowned",
            "minecraft:skeleton", "minecraft:creeper", "minecraft:spider",
            "minecraft:enderman", "minecraft:wither", "minecraft:blaze",
            "minecraft:ghast", "minecraft:shulker", "minecraft:vindicator",
            "minecraft:piglin", "minecraft:zombified_piglin", "minecraft:wither_skeleton"
        )
        return (this as? EntityUnknown)?.identifier in hostileIdentifiers
    }

    private fun Entity.isPassive(): Boolean {
        val passiveIdentifiers = listOf(
            "minecraft:cow", "minecraft:pig", "minecraft:sheep",
            "minecraft:chicken", "minecraft:horse", "minecraft:llama",
            "minecraft:rabbit", "minecraft:mooshroom", "minecraft:villager",
            "minecraft:goat", "minecraft:donkey", "minecraft:mule"
        )
        return (this as? EntityUnknown)?.identifier in passiveIdentifiers
    }

    private fun isMace(item: ItemData): Boolean {
        
        
        
        return item.netId == 344 
    }
}