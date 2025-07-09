package com.project.lumina.client.game.module.impl.visual

import com.project.lumina.client.R
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.Ability
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer
import org.cloudburstmc.protocol.bedrock.data.GameType
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.RequestAbilityPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket
import com.project.lumina.client.util.AssetManager

class FreeCameraElement(iconResId: Int = AssetManager.getAsset("ic_movie_open_black_24dp")) : Element(
    name = "FreeCam",
    category = CheatCategory.Visual,
    iconResId,
    displayNameResId = AssetManager.getString("module_free_camera_display_name")
) {

    private var originalPosition: Vector3f? = null
    private var cameraSpeed by floatValue("速度", 0.5f, 0.1f..2.0f)
    private val coroutineScope = CoroutineScope(Dispatchers.Default) 
    private var countdownJob: Job? = null 

    private val enableFlyNoClipPacket = SetPlayerGameTypePacket().apply {
        gamemode = GameType.SPECTATOR.ordinal
    }

    private val disableFlyNoClipPacket = SetPlayerGameTypePacket().apply {
        gamemode = GameType.SURVIVAL.ordinal
    }

    override fun onEnabled() {
        super.onEnabled()
        try {
            if (isSessionCreated) {
                
                countdownJob?.cancel()
                countdownJob = coroutineScope.launch {
                    try {
                        for (i in 5 downTo 1) {
                            val countdownMessage = "§l§b[Lumina] §r§7自由视角将会在 §e$i §7秒内打开"
                            sendCountdownMessage(countdownMessage)
                            delay(1000)
                        }

                        originalPosition = Vector3f.from(
                            session.localPlayer.posX,
                            session.localPlayer.posY,
                            session.localPlayer.posZ
                        )
                        session.clientBound(enableFlyNoClipPacket)
                    } catch (e: Exception) {
                        println("Error in FreeCamera countdown or packet sending: ${e.message}")
                    }
                }
            } else {
                println("Session not created, cannot enable FreeCamera.")
            }
        } catch (e: Exception) {
            println("Error launching FreeCamera coroutine: ${e.message}")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        
        countdownJob?.cancel()
        countdownJob = null

        if (isSessionCreated && originalPosition != null) {
            val motionPacket = SetEntityMotionPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                motion = originalPosition
            }
            session.clientBound(motionPacket)
            originalPosition = null

            session.clientBound(disableFlyNoClipPacket)
        }
    }

    private fun sendCountdownMessage(message: String) {
        val textPacket = TextPacket().apply {
            type = TextPacket.Type.RAW
            isNeedsTranslation = false
            this.message = message
            xuid = ""
            sourceName = ""
        }

        session.clientBound(textPacket)
    }

    private var flySpeed by floatValue("Speed", 0.15f, 0.1f..1.5f)

    private val enableFlyAbilitiesPacket = UpdateAbilitiesPacket().apply {
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
                    Ability.OPERATOR_COMMANDS,
                    Ability.MAY_FLY,
                    Ability.FLY_SPEED,
                    Ability.WALK_SPEED,
                    Ability.FLYING
                )
            )
            walkSpeed = 0.1f
            flySpeed = this@FreeCameraElement.flySpeed
        })
    }

    private val disableFlyAbilitiesPacket = UpdateAbilitiesPacket().apply {
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
                    Ability.OPERATOR_COMMANDS,
                    Ability.FLY_SPEED,
                    Ability.WALK_SPEED
                )
            )
            walkSpeed = 0.1f
        })
    }
    private var canFly = true
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {

        val packet = interceptablePacket.packet
        if (packet is RequestAbilityPacket && packet.ability == Ability.FLYING) {
            interceptablePacket.intercept()
            return
        }

        if (packet is UpdateAbilitiesPacket) {
            interceptablePacket.intercept()
            return
        }

        if (packet is PlayerAuthInputPacket) {

            if (!canFly && isEnabled) {
                enableFlyAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
                session.clientBound(enableFlyAbilitiesPacket)
                canFly = true
            } else if (canFly && !isEnabled) {
                disableFlyAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
                session.clientBound(disableFlyAbilitiesPacket)
                canFly = false
                return
            }


            if (isEnabled) {
                var verticalMotion = 0f


                if (packet.inputData.contains(PlayerAuthInputData.JUMPING)) {
                    verticalMotion = flySpeed
                } else if (packet.inputData.contains(PlayerAuthInputData.SNEAKING)) {
                    verticalMotion = -flySpeed
                }

                if (verticalMotion != 0f) {
                    val motionPacket = SetEntityMotionPacket().apply {
                        runtimeEntityId = session.localPlayer.runtimeEntityId
                        motion = Vector3f.from(0f, verticalMotion, 0f)
                    }
                    session.clientBound(motionPacket)
                }
            }
        }

        if (packet is PlayerAuthInputPacket && isEnabled ) {
            interceptablePacket.intercept()


        }
    }

    
    fun destroy() {
        coroutineScope.cancel()
    }
}