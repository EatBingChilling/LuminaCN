package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.entity.*
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.*

class KillauraElement(iconResId: Int = AssetManager.getAsset("ic_sword_cross_black_24dp")) : Element(
    name = "KillAura",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_killaura_display_name")
) {
    private val playerOnly by boolValue("玩家", false)
    private val mobsOnly by boolValue("生物", true)
    private val range by floatValue("范围", 3.7f, 2f..7f)
    private val delay by intValue("间隔", 5, 1..20)
    private val cps by intValue("CPS", 5, 1..20)
    private val packets by intValue("发包", 1, 1..10)
    private val tpAura by boolValue("TP光环", false)
    private val strafe by boolValue("旋转", false)
    private val tpBehind by boolValue("传送到背后", false)
    private val keepDist by floatValue("保持距离", 2.0f, 1f..5f)
    private val tpSpeed by intValue("TP速度", 500, 100..2000)
    private val strafeSpeed by floatValue("旋转速度", 1.0f, 0.1f..2.0f)
    private val strafeRadius by floatValue("旋转半径", 1.0f, 0.1f..5.0f)

    private val notification by boolValue("通知", true)
    private val multiTarget by boolValue("多目标", false)
    private val notificationInterval = 1000L

    private var strafeAngle = 0f
    private var lastAttack = 0L
    private var tpCooldown = 0L
    private var lastNotificationTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || interceptablePacket.packet !is PlayerAuthInputPacket) return

        val packet = interceptablePacket.packet as PlayerAuthInputPacket
        val now = System.currentTimeMillis()
        val minDelay = 1000L / cps

        if (packet.tick % delay != 0L || (now - lastAttack) < minDelay) return

        val targets = getTargets()
        if (targets.isEmpty()) return

        if (notification && targets.isNotEmpty() && now - lastNotificationTime >= notificationInterval) {
            showNotification(targets, now)
        }

        targets.forEach { target -> attack(target, now) }
    }

    private fun attack(target: Entity, now: Long) {
        if (tpAura && (now - tpCooldown) >= tpSpeed) {
            tp(target)
            tpCooldown = now
        }

        repeat(packets) { session.localPlayer.attack(target) }
        if (strafe) doStrafe(target)
        lastAttack = now
    }

    private fun doStrafe(target: Entity) {
        strafeAngle = (strafeAngle + strafeSpeed) % 360f
        val rad = Math.toRadians(strafeAngle.toDouble())
        val pos = target.vec3Position.add(
            (strafeRadius * cos(rad)).toFloat(),
            0f,
            (strafeRadius * sin(rad)).toFloat()
        )
        move(pos, Vector3f.ZERO)
    }

    private fun tp(target: Entity) {
        val dir = if (tpBehind) getBehindDir(target) else getOptimalDir(target)
        val pos = target.vec3Position.add(
            dir.x * keepDist,
            0f,
            dir.z * keepDist
        )
        move(pos, target.vec3Rotation, false)
    }

    private fun getBehindDir(target: Entity): Vector3f {
        val yaw = Math.toRadians(target.vec3Rotation.y.toDouble())
        return Vector3f.from(sin(yaw).toFloat(), 0f, -cos(yaw).toFloat()).norm()
    }

    private fun getOptimalDir(target: Entity): Vector3f {
        val playerPos = session.localPlayer.vec3Position
        val targetPos = target.vec3Position
        return Vector3f.from(
            playerPos.x - targetPos.x,
            0f,
            playerPos.z - targetPos.z
        ).norm()
    }

    private fun move(pos: Vector3f, rot: Vector3f, onGround: Boolean = true) {
        session.clientBound(MovePlayerPacket().apply {
            runtimeEntityId = session.localPlayer.runtimeEntityId
            position = pos
            rotation = rot
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = onGround
            ridingRuntimeEntityId = 0
            tick = session.localPlayer.tickExists
        })
    }

    private fun showNotification(targets: List<Entity>, now: Long) {
        val target = targets.first()
        val coords = "(${target.vec3Position.x.toInt()}, ${target.vec3Position.y.toInt()}, ${target.vec3Position.z.toInt()})"
        val distance = String.format("%.1f", target.distance(session.localPlayer))
        val entityName = getEntityName(target)
        val targetCount = if (multiTarget) " (${targets.size} 个目标)" else ""

        session.showNotification(
            "正在攻击 $entityName$targetCount",
            "$coords | 距离: ${distance}b | CPS: $cps",
            com.project.lumina.client.R.drawable.swords_24px
        )
        lastNotificationTime = now
    }

    private fun getEntityName(entity: Entity): String = when (entity) {
        is Player -> entity.username
        is EntityUnknown -> entity.identifier.substringAfter(':').replaceFirstChar { it.uppercase() }
        else -> "Unknown"
    }

    private fun getTargets(): List<Entity> = session.level.entityMap.values.filter {
        it.distance(session.localPlayer) <= range && it.isValid()
    }

    private fun Entity.isValid(): Boolean = when (this) {
        is LocalPlayer -> false
        is Player -> (playerOnly || (playerOnly && mobsOnly)) && !isBot()
        is EntityUnknown -> (mobsOnly || (playerOnly && mobsOnly)) && isMob() && !isShadow()
        else -> false
    }

    private fun EntityUnknown.isMob(): Boolean = identifier in MobList.mobTypes

    private fun EntityUnknown.isShadow(): Boolean = identifier == "hivecommon:shadow"

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        return session.level.playerMap[uuid]?.name?.isBlank() ?: true
    }

    private fun Vector3f.norm(): Vector3f {
        val len = length()
        return if (len != 0f) Vector3f.from(x / len, y / len, z / len) else this
    }

    private val now get() = System.currentTimeMillis()
}