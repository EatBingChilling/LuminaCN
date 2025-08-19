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

package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.game.module.api.setting.stringValue
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.event.handle
import com.phoenix.luminacn.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType

class CritBotElement(iconResId: Int = AssetManager.getAsset("ic_angle")) : Element(
    name = "Criticals",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_critbot_display_name")
) {
    private val mode by stringValue(this, "Mode", "Vanilla", listOf("Vanilla", "Jump"))

    private var canJump = true

    private object Vanilla {
        fun handlePacket(interceptablePacket: InterceptablePacket, parent: CritBotElement) {
            if (interceptablePacket.packet is MovePlayerPacket) {
                val movePacket = interceptablePacket.packet as MovePlayerPacket
                movePacket.onGround = false
            }
        }
    }

    private object Jump {
        fun handlePacket(interceptablePacket: InterceptablePacket, parent: CritBotElement) {
            if (!parent.isSessionCreated) return

            when (val packet = interceptablePacket.packet) {
                is MovePlayerPacket -> {
                    if (packet.onGround) {
                        parent.canJump = true
                    }
                }

                is PlayerActionPacket -> {
                    if (packet.action == PlayerActionType.START_BREAK && parent.canJump) {
                        val motionPacket = SetEntityMotionPacket().apply {
                            runtimeEntityId = parent.session.localPlayer.runtimeEntityId
                            motion = Vector3f.from(0f, 0.42f, 0f)
                        }
                        parent.session.clientBound(motionPacket)
                        parent.canJump = false
                    }
                }
            }
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isSessionCreated) return

        val packet = interceptablePacket.packet
        if (packet is SetEntityMotionPacket &&
            packet.runtimeEntityId != session.localPlayer.runtimeEntityId) {
            return
        }

        when (mode) {
            "Vanilla" -> Vanilla.handlePacket(interceptablePacket, this)
            "Jump" -> Jump.handlePacket(interceptablePacket, this)
        }
    }
}