package com.project.luminacn.game.module.impl.misc

import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.util.AssetManager
import com.project.luminacn.game.module.api.setting.stringValue
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.LevelEvent
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class WeatherControllerElement : Element(
    name = "Weather Controller",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_weather_controller_display_name")
) {

    private var clear by boolValue("晴朗", true)
    private var rain by boolValue("雨天", false)
    private var thunderstorm by boolValue("雷雨", false)
    private var targetWeather by stringValue(this, "目标天气", "晴朗", listOf("clear", "rain", "thunder"))

    private var lastUpdate = 0L
    private var lastPosition = Vector3f.ZERO

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            lastPosition = Vector3f.from(packet.position.x, packet.position.y, packet.position.z)
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastUpdate >= 100) {
                lastUpdate = currentTime

                session.clientBound(LevelEventPacket().apply {
                    type = LevelEvent.STOP_RAINING
                    position = lastPosition
                    data = 0
                })
                session.clientBound(LevelEventPacket().apply {
                    type = LevelEvent.STOP_THUNDERSTORM
                    position = lastPosition
                    data = 0
                })

                when {
                    clear -> {

                    }

                    rain -> {
                        session.clientBound(LevelEventPacket().apply {
                            type = LevelEvent.START_RAINING
                            position = lastPosition
                            data = 10000
                        })
                    }

                    thunderstorm -> {
                        session.clientBound(LevelEventPacket().apply {
                            type = LevelEvent.START_RAINING
                            position = lastPosition
                            data = 10000
                        })
                        session.clientBound(LevelEventPacket().apply {
                            type = LevelEvent.START_THUNDERSTORM
                            position = lastPosition
                            data = 10000
                        })
                    }
                }
            }
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated) {
            session.clientBound(LevelEventPacket().apply {
                type = LevelEvent.STOP_RAINING
                position = lastPosition
                data = 0
            })
            session.clientBound(LevelEventPacket().apply {
                type = LevelEvent.STOP_THUNDERSTORM
                position = lastPosition
                data = 0
            })
        }
    }
}
