package com.project.luminacn.game.module.impl.misc

import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.util.AssetManager
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import java.util.*
import kotlin.collections.HashMap

class PingSpoofElement(iconResId: Int = AssetManager.getAsset("ic_timer_sand_black_24dp")) : Element(
    name = "FakePing",
    category = CheatCategory.Misc,
    iconResId = iconResId,
    displayNameResId = AssetManager.getString("module_fakeping_display_name")
) {
    private val pingValue by intValue("Ping(毫秒)", 300, 50..1000)
    private val jitter by intValue("抖动(毫秒)", 50, 0..200)
    private val tickInterval by intValue("Tick间隔", 1, 1..20)

    private val pendingResponses = HashMap<Long, Long>() // Key: original server timestamp, Value: scheduled send time
    private val random = Random()

    override fun onEnabled() {
        super.onEnabled()
        pendingResponses.clear()
    }

    override fun onDisabled() {
        super.onDisabled()
        pendingResponses.clear()
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        when {
            packet is NetworkStackLatencyPacket && packet.fromServer -> {
                handleLatencyPacket(interceptablePacket, packet)
            }
            packet is PlayerAuthInputPacket && packet.tick % tickInterval == 0L -> {
                processPendingResponses()
            }
        }
    }

    private fun handleLatencyPacket(interceptablePacket: InterceptablePacket, packet: NetworkStackLatencyPacket) {
        interceptablePacket.intercept()

        val delay = calculateDelay()
        pendingResponses[packet.timestamp] = System.currentTimeMillis() + delay

        // Prevent memory leaks
        if (pendingResponses.size > 100) {
            pendingResponses.clear()
        }
    }

    private fun processPendingResponses() {
        val currentTime = System.currentTimeMillis()
        val readyPackets = pendingResponses.entries.filter { it.value <= currentTime }

        readyPackets.forEach { (serverTimestamp, _) ->
            session?.serverBound(NetworkStackLatencyPacket().apply {
                // Multiply by 1M to match vanilla client behavior
                timestamp = serverTimestamp * 1_000_000
                // Keep needs_response:1 like vanilla clients do
                fromServer = true
            })
            pendingResponses.remove(serverTimestamp)
        }
    }

    private fun calculateDelay(): Long {
        val baseDelay = pingValue.toLong()
        val jitterOffset = if (jitter > 0) random.nextInt(jitter * 2) - jitter else 0
        return (baseDelay + jitterOffset).coerceAtLeast(0)
    }
}
