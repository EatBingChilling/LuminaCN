package com.project.luminacn.game.module.impl.world

import com.project.luminacn.R
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.CheatCategory
import org.cloudburstmc.protocol.bedrock.data.Ability
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.RequestAbilityPacket
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket
import kotlin.collections.addAll

class NoClipElement(iconResId: Int = R.drawable.ic_circle_double_black_24dp) : Element(
    name = "NoClip",
    category = CheatCategory.World,
    iconResId,
    displayNameResId = R.string.module_no_clip_display_name
) {

    private val enableNoClipAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(
                arrayOf(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS,
                    Ability.MAY_FLY,
                    Ability.FLY_SPEED,
                    Ability.WALK_SPEED,
                    Ability.OPERATOR_COMMANDS
                )
            )
            abilityValues.add(Ability.NO_CLIP)
            walkSpeed = 0.1f
            flySpeed = 0.15f
        })
    }

    private val disableNoClipAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(
                arrayOf(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS,
                    Ability.FLY_SPEED,
                    Ability.WALK_SPEED,
                    Ability.OPERATOR_COMMANDS
                )
            )
            abilityValues.remove(Ability.NO_CLIP)
            walkSpeed = 0.1f
        })
    }

    private var noClipEnabled = false

    private var speed by floatValue("速度", 0.5f, 0.1f..2.0f)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet is RequestAbilityPacket && packet.ability == Ability.NO_CLIP) {
            interceptablePacket.intercept()
            return
        }

        if (packet is UpdateAbilitiesPacket) {
            interceptablePacket.intercept()
            return
        }

        if (packet is PlayerAuthInputPacket) {
            if (!noClipEnabled && isEnabled) {
                enableNoClipAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
                session.clientBound(enableNoClipAbilitiesPacket)
                noClipEnabled = true
            } else if (noClipEnabled && !isEnabled) {
                disableNoClipAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
                session.clientBound(disableNoClipAbilitiesPacket)
                noClipEnabled = false
            }
        }
    }

}