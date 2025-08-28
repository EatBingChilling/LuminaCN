package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.R
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.entity.Entity
import com.phoenix.luminacn.game.entity.EntityUnknown
import com.phoenix.luminacn.game.entity.LocalPlayer
import com.phoenix.luminacn.game.entity.Player
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import com.phoenix.luminacn.util.AssetManager

class GodModeElement(iconResId: Int = AssetManager.getAsset("ic_ghost_black_24dp")) : Element(
    name = "GodMode",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_godmode_display_name")
) {
    private val ghostX by floatValue("Ghost X 偏移", 0f, 0f..1024f)
    private val ghostY by floatValue("Ghost Y 偏移", 256f, 0f..1024f)
    private val ghostZ by floatValue("Ghost Z 偏移", 0f, 0f..1024f)
    private val GHOST_OFFSET get() = Vector3f.from(ghostX, ghostY, ghostZ)


    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet

        if (packet is PlayerAuthInputPacket) {
            val ghostPos = session.localPlayer.vec3Position.add(GHOST_OFFSET)
            session.clientBound(
                MovePlayerPacket().apply {
                    runtimeEntityId = session.localPlayer.runtimeEntityId
                    position        = ghostPos
                    rotation        = packet.rotation
                    mode            = MovePlayerPacket.Mode.NORMAL
                    onGround        = true
                    tick            = session.localPlayer.tickExists
                }
            )
            interceptablePacket.intercept()
        }
        if (packet is MovePlayerPacket && packet.runtimeEntityId == session.localPlayer.runtimeEntityId) {
            packet.position = packet.position.add(GHOST_OFFSET)
        }
    }
}