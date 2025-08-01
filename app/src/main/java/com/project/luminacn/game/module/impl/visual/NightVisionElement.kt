package com.project.luminacn.game.module.impl.visual

import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.game.utils.constants.Effect
import com.project.luminacn.game.module.api.setting.Effects
import com.project.luminacn.game.module.api.setting.EffectSetting
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import com.project.luminacn.util.AssetManager

class NightVisionElement(iconResId: Int = AssetManager.getAsset("ic_camera_outline_black_24dp")) : Element(
    name = "NightVision",
    category = CheatCategory.Visual,
    iconResId,
    displayNameResId = AssetManager.getString("module_night_vision_display_name")
) {

    private val amplifierValue by intValue("放大", 1, 1..5)
    private val effect by EffectSetting(
        this,
        EntityDataTypes.VISIBLE_MOB_EFFECTS,
        Effects.NIGHT_VISION
    )

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated) {
            session.clientBound(MobEffectPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                event = MobEffectPacket.Event.REMOVE
                effectId = Effect.Companion.NIGHT_VISION
            })
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            if (session.localPlayer.tickExists % 20 == 0L) {
                session.clientBound(MobEffectPacket().apply {
                    runtimeEntityId = session.localPlayer.runtimeEntityId
                    event = MobEffectPacket.Event.ADD
                    effectId = Effect.Companion.NIGHT_VISION
                    amplifier = amplifierValue
                    isParticles = false
                    duration = 360000
                })
            }
        }
    }
}