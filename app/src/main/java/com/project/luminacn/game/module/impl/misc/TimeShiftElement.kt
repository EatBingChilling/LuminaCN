package com.project.luminacn.game.module.impl.misc

import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.util.AssetManager
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket

class TimeShiftElement : Element(
    name = "TimeVariation",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_time_shift_display_name")
) {

    private val time by intValue("时间", 6000, 0..24000)
    private var lastTimeUpdate = 0L
    private var timeMultiplier by floatValue("时间调整", 2f, 0.1f..10f)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val currentTime = System.currentTimeMillis()

            
            if (currentTime - lastTimeUpdate >= 100) {
                lastTimeUpdate = currentTime

                val timePacket = SetTimePacket()
                timePacket.time = time
                session.clientBound(timePacket)
            }
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated) {
            val timePacket = SetTimePacket()
            timePacket.time = 0
            session.clientBound(timePacket)
        }
    }
}
