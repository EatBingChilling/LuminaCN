package com.project.lumina.client.game.module.impl.motion

import com.project.lumina.client.R
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.Ability
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket


class MotionFlyElement(iconResId: Int = AssetManager.getAsset("ic_flash_black_24dp")) : Element(
    name = "MotionFly",
    category = CheatCategory.Motion,
    iconResId,
    displayNameResId = AssetManager.getString("module_motion_fly_display_name")
) {

    private val verticalSpeedUp by floatValue("上升速度", 7.0f, 1.0f..20.0f)
    private val verticalSpeedDown by floatValue("下降速度", 7.0f, 1.0f..20.0f)
    private val motionInterval by floatValue("间隔", 100.0f, 100.0f..600.0f)
    private var lastMotionTime = 0L
    private var jitterState = false
    private var canFly = false
    private var motionSpeed by floatValue("速度", 0.5f, 0.1f..5.0f)

    private val flyAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(Ability.entries)
            walkSpeed = 0.1f
            flySpeed = 2.19f
        })
    }

    private val resetAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.removeAll { it == Ability.MAY_FLY || it == Ability.NO_CLIP }
            walkSpeed = 0.1f
            flySpeed = 0f
        })
    }

    private fun handleFlyAbilities(isEnabled: Boolean) {
        if (canFly != isEnabled) {
            flyAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            resetAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            if (isEnabled) {
                session.clientBound(flyAbilitiesPacket)
            } else {
                session.clientBound(resetAbilitiesPacket)
            }
            canFly = isEnabled
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {

        val packet = interceptablePacket.packet


        if (packet is PlayerAuthInputPacket) {
            handleFlyAbilities(isEnabled)
            if (isEnabled && System.currentTimeMillis() - lastMotionTime >= motionInterval) {
                val vertical = when {
                    packet.inputData.contains(PlayerAuthInputData.WANT_UP) -> verticalSpeedUp
                    packet.inputData.contains(PlayerAuthInputData.WANT_DOWN) -> -verticalSpeedDown
                    else -> 0f
                }
                val motionPacket = SetEntityMotionPacket().apply {
                    runtimeEntityId = session.localPlayer.runtimeEntityId
                    motion = Vector3f.from(0f, vertical + (if (jitterState) 0.1f else -0.1f), 0f)
                }
                session.clientBound(motionPacket)
                jitterState = !jitterState
                lastMotionTime = System.currentTimeMillis()
            }
        }

    }
}