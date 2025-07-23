package com.project.luminacn.game.module.impl.world

import com.project.luminacn.R
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.game.utils.constants.Effect
import com.project.luminacn.game.module.api.setting.Effects
import com.project.luminacn.game.module.api.setting.EffectSetting
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class HasteElement : Element(
    name = "Haste",
    category = CheatCategory.World,
    displayNameResId = R.string.module_haste_display_name
) {

    private val amplifierValue by floatValue("调整", 1f, 1f..5f)
    private val effect by EffectSetting(this, EntityDataTypes.VISIBLE_MOB_EFFECTS, Effects.HASTE)

    override fun onDisabled() {
        super.onDisabled()
        if (isSessionCreated) {
            session.clientBound(MobEffectPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                event = MobEffectPacket.Event.REMOVE
                effectId = Effect.Companion.HASTE
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
                    effectId = Effect.Companion.HASTE
                    amplifier = amplifierValue.toInt() - 1
                    isParticles = false
                    duration = 360000
                })
            }
        }
    }
}