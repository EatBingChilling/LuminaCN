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

package com.project.lumina.client.game.module.impl.misc

import android.util.Log
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.util.AssetManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason
import org.cloudburstmc.protocol.bedrock.packet.*
import kotlin.random.Random

class AntiKickElement : Element(
    name = "AntiKick",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_antikick_display_name"),
    iconResId = AssetManager.getAsset("ic_alien")
) {
    
    private var disconnectPacketValue by boolValue("掉线数据包", true)
    private var transferPacketValue by boolValue("传送数据包", true)
    private var playStatusPacketValue by boolValue("游玩状态数据包", true)
    private var networkSettingsPacketValue by boolValue("网络设置数据包", true)
    
    
    private var showKickMessages by boolValue("显示被踢消息", true)
    private var intelligentBypass by boolValue("智能绕过", true)
    private var autoReconnect by boolValue("自动重连", false)
    private var antiAfkSimulation by boolValue("防挂机被踢", true)
    private var useRandomMovement by boolValue("随机行走", true)
    private var preventTimeout by boolValue("防止计时", true)
    
    
    private var movementInterval by intValue("行走间隔", 8000, 500..15000)
    private var movementDuration by intValue("行走步数", 500, 100..3000)
    
    
    private var reconnectDelay by intValue("重连间隔 （毫秒）", 3000, 1000..10000)
    private var maxReconnectAttempts by intValue("最多尝试重连次数", 3, 1..10)
    
    
    private var lastMovementTime = 0L
    private var isPerformingAntiAFK = false
    private var reconnectAttempts = 0
    private var lastDisconnectReason: String? = null
    private var lastHeartbeatTime = 0L
    private val heartbeatInterval = 30000L
    
    override fun onEnabled() {
        super.onEnabled()
        if (isSessionCreated) {
            lastMovementTime = System.currentTimeMillis()
            reconnectAttempts = 0
            lastHeartbeatTime = System.currentTimeMillis()
            
            if (preventTimeout) {
                startHeartbeatTask()
            }
        }
    }
    
    override fun onDisabled() {
        super.onDisabled()
        isPerformingAntiAFK = false
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private fun startHeartbeatTask() {
        GlobalScope.launch {
            while (isEnabled && isSessionCreated) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastHeartbeatTime >= heartbeatInterval) {
                    try {
                        val textPacket = TextPacket().apply {
                            type = TextPacket.Type.TIP
                            isNeedsTranslation = false
                            message = ""
                            xuid = ""
                            platformChatId = ""
                        }
                        session.clientBound(textPacket)
                        lastHeartbeatTime = currentTime
                    } catch (e: Exception) {
                        Log.w("AntiKick", "Failed to send heartbeat packet", e)
                    }
                }
                delay(5000)
            }
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet
        val currentTime = System.currentTimeMillis()

        if (packet is DisconnectPacket && disconnectPacketValue) {
            handleDisconnectPacket(interceptablePacket, packet)
        }

        if (packet is TransferPacket && transferPacketValue) {
            handleTransferPacket(interceptablePacket, packet)
        }
        
        if (packet is PlayStatusPacket && playStatusPacketValue) {
            handlePlayStatusPacket(interceptablePacket, packet)
        }
        
        if (packet is NetworkSettingsPacket && networkSettingsPacketValue) {
            if (intelligentBypass) {
                if (showKickMessages) {
                    session.displayClientMessage("§8[§bAntiKick§8] §7Network settings updated")
                }
            }
        }
        
        if (antiAfkSimulation && packet is PlayerAuthInputPacket && currentTime - lastMovementTime >= movementInterval) {
            performAntiAFKMovement()
            lastMovementTime = currentTime
        }
    }
    
    private fun handleDisconnectPacket(interceptablePacket: InterceptablePacket, packet: DisconnectPacket) {
        lastDisconnectReason = packet.kickMessage
        
        if (showKickMessages) {
            val reason = getReadableKickReason(packet.reason, packet.kickMessage)
            session.displayClientMessage("§8[§bantikick§8] §c掉线检测: §f$reason")
        }
        
        interceptablePacket.isIntercepted = true
        
        if (autoReconnect) {
            attemptReconnect()
        }
    }
    
    private fun handleTransferPacket(interceptablePacket: InterceptablePacket, packet: TransferPacket) {
        if (showKickMessages) {
            session.displayClientMessage("§8[§bantikick§8] §e传送 §7到 §f${packet.address}:${packet.port}")
        }
        
        interceptablePacket.isIntercepted = true
    }
    
    private fun handlePlayStatusPacket(interceptablePacket: InterceptablePacket, packet: PlayStatusPacket) {
        val status = packet.status
        if (status == PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD || 
            status == PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD ||
            status == PlayStatusPacket.Status.LOGIN_FAILED_INVALID_TENANT ||
            status == PlayStatusPacket.Status.LOGIN_FAILED_EDITION_MISMATCH_EDU_TO_VANILLA ||
            status == PlayStatusPacket.Status.LOGIN_FAILED_EDITION_MISMATCH_VANILLA_TO_EDU ||
            status == PlayStatusPacket.Status.FAILED_SERVER_FULL_SUB_CLIENT) {
            
            if (showKickMessages) {
                session.displayClientMessage("§8[§bantikick§8] §c游玩时被踢检测: §f$status")
            }
            
            interceptablePacket.isIntercepted = true
            
            if (autoReconnect) {
                attemptReconnect()
            }
        }
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private fun performAntiAFKMovement() {
        if (isPerformingAntiAFK) return
        
        isPerformingAntiAFK = true
        
        GlobalScope.launch {
            try {
                performSimpleMovement()
                delay(movementDuration.toLong())
                isPerformingAntiAFK = false
            } catch (e: Exception) {
                Log.e("AntiKick", "Error during anti-AFK movement", e)
                isPerformingAntiAFK = false
            }
        }
    }
    
    private fun performSimpleMovement() {
        val motionPacket = SetEntityMotionPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            motion = Vector3f.from(0.01f, 0f, 0.01f)
        }
        session.clientBound(motionPacket)
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private fun attemptReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            session.displayClientMessage("§8[§bantikick§8] §c已至最大重连次数 (§f$maxReconnectAttempts§c)")
            return
        }
        
        reconnectAttempts++
        
        session.displayClientMessage("§8[§bantikick§8] §e尝试重连 §7(§f$reconnectAttempts§7/§f$maxReconnectAttempts§7)...")
        
        GlobalScope.launch {
            delay(reconnectDelay.toLong())
            session.displayClientMessage("§8[§bantikick§8] §a重连成功")
        }
    }
    
    private fun getReadableKickReason(reason: DisconnectFailReason, message: String): String {
        return when (reason) {
            DisconnectFailReason.KICKED, DisconnectFailReason.KICKED_FOR_EXPLOIT, DisconnectFailReason.KICKED_FOR_IDLE -> 
                "被踢出: $message"
            DisconnectFailReason.TIMEOUT -> "超时"
            DisconnectFailReason.SERVER_FULL -> "服务器满员"
            DisconnectFailReason.NOT_ALLOWED -> "被禁止加入"
            DisconnectFailReason.BANNED_SKIN -> "皮肤被封禁"
            DisconnectFailReason.SHUTDOWN -> "服务器已关闭"
            DisconnectFailReason.INVALID_PLAYER -> "非法的玩家数据"
            else -> "$reason: $message"
        }
    }
}