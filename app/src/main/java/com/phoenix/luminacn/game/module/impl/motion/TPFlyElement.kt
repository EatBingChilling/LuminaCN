package com.phoenix.luminacn.game.module.impl.motion

import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.utils.math.MathUtil.JITTER_VAL
import com.phoenix.luminacn.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import kotlin.math.cos
import kotlin.math.sin

class TPFlyElement(
    iconResId: Int = AssetManager.getAsset("ic_menu_arrow_up_black_24dp")
) : Element(
    name = "TPFly",
    category = CheatCategory.Motion,
    iconResId,
    displayNameResId = AssetManager.getString("module_tpfly_display_name")
) {
    private val stepSpeed      by floatValue("水平速度", 0.7f, 0.1f..3.0f)
    private val verticalSpeed  by floatValue("垂直速度", 0.5f, 0.1f..2.0f)
    private val sprintFactor   by floatValue("疾跑加速倍率", 2.0f, 1.0f..4.0f)

    override fun onEnabled() {
        super.onEnabled()
        session.clientBound(
            SetEntityMotionPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                motion = Vector3f.from(0f, 0f, 0f)
            }
        )
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        var inputX = 0f
        var inputZ = 0f

        with(packet.inputData) {
            if (contains(PlayerAuthInputData.UP))        inputZ += 1f
            if (contains(PlayerAuthInputData.DOWN))      inputZ -= 1f
            if (contains(PlayerAuthInputData.LEFT))      inputX -= 1f
            if (contains(PlayerAuthInputData.RIGHT))     inputX += 1f

            if (contains(PlayerAuthInputData.UP_LEFT))   { inputZ += JITTER_VAL; inputX -= JITTER_VAL }
            if (contains(PlayerAuthInputData.UP_RIGHT))  { inputZ += JITTER_VAL; inputX += JITTER_VAL }
            if (contains(PlayerAuthInputData.DOWN_LEFT)) { inputZ -= JITTER_VAL; inputX -= JITTER_VAL }
            if (contains(PlayerAuthInputData.DOWN_RIGHT)){ inputZ -= JITTER_VAL; inputX += JITTER_VAL }
        }

        val yaw  = packet.rotation.y
        val yawRad = Math.toRadians(yaw.toDouble())
        val speed = stepSpeed * (if (packet.inputData.contains(PlayerAuthInputData.SPRINTING)) sprintFactor else 1f)

        val dx = (-sin(yawRad) * inputZ + cos(yawRad) * inputX) * speed
        val dz = ( cos(yawRad) * inputZ + sin(yawRad) * inputX) * speed

        val dy = when {
            packet.inputData.contains(PlayerAuthInputData.WANT_UP)   ->  verticalSpeed
            packet.inputData.contains(PlayerAuthInputData.WANT_DOWN) -> -verticalSpeed
            else -> 0f
        }

        val old = session.localPlayer.vec3Position
        val newPos = Vector3f.from(
            (old.x + dx).toFloat(),
            (old.y + dy).toFloat(),
            (old.z + dz).toFloat()
        )

        session.clientBound(
            MovePlayerPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                position = newPos
                rotation = packet.rotation
                mode = MovePlayerPacket.Mode.NORMAL
                onGround = false
                tick = session.localPlayer.tickExists
            }
        )
    }
}