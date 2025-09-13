package com.phoenix.luminacn.game.module.impl.motion

import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.util.AssetManager
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

    private var motionSpeed by floatValue("速度", 0.5f, 0f..3f)
    private val verticalSpeed by floatValue("垂直移动速度", 1.5f, 1f..3f)
    private val glideEnabled by boolValue("Glide", true)
    private val addValue by floatValue("Glide", 0.01f, -0.2f..0.2f)
    private var lastMotionTime = 0L
    private var canFly = false

    private val fixedInterval = 100L

    private val flyAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(Ability.entries)
            walkSpeed = 0.1f
            flySpeed = motionSpeed
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
    private fun updateFlySpeed() {
        flyAbilitiesPacket.abilityLayers.firstOrNull()?.flySpeed = motionSpeed
    }

    private fun handleFlyAbilities(isEnabled: Boolean) {
        if (canFly != isEnabled) {
            flyAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            resetAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            if (isEnabled) {
                updateFlySpeed()
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
            if (isEnabled && System.currentTimeMillis() - lastMotionTime >= fixedInterval) {
                val vertical = when {
                    packet.inputData.contains(PlayerAuthInputData.WANT_UP) -> verticalSpeed
                    packet.inputData.contains(PlayerAuthInputData.WANT_DOWN) -> -verticalSpeed
                    else -> if (glideEnabled) addValue else 0f
                }
                val motionPacket = SetEntityMotionPacket().apply {
                    runtimeEntityId = session.localPlayer.runtimeEntityId
                    motion = Vector3f.from(0f, vertical, 0f)
                }
                session.clientBound(motionPacket)
                lastMotionTime = System.currentTimeMillis()
            }
        }
    }
}