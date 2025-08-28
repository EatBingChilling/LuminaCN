package com.phoenix.luminacn.game.module.impl.misc

import com.phoenix.luminacn.R
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.module.api.setting.stringValue
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random
import com.phoenix.luminacn.util.AssetManager


class DesyncElement(iconResId: Int = AssetManager.getAsset("ic_timer_sand_black_24dp")) : Element(
    name = "Desync",
    category = CheatCategory.Misc,
    iconResId,
    displayNameResId = AssetManager.getString("module_desync_display_name")
) {
    private var isDesynced = false
    private val storedPackets = ConcurrentLinkedQueue<PlayerAuthInputPacket>()
    private val updateDelay = 1000L
    private val minResendInterval = 100L
    private val maxResendInterval = 300L
    private var desyncMode by stringValue(this, "模式", "抖动", listOf("jitter", "freeze"))

    override fun onEnabled() {
        super.onEnabled()
        if(isSessionCreated) {
            isDesynced = true
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDisabled() {
        super.onDisabled()
        isDesynced = false

        GlobalScope.launch {
            delay(updateDelay)
            while (storedPackets.isNotEmpty()) {
                val packet = storedPackets.poll()
                if (packet != null) {
                    session.clientBound(packet)
                }
                delay(Random.nextLong(minResendInterval, maxResendInterval))
            }
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isDesynced) {
            return
        }

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            storedPackets.add(packet)
            interceptablePacket.intercept()
        }
    }

}