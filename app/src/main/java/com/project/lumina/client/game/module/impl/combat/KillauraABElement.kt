package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.entity.*
import com.project.lumina.client.game.module.api.setting.stringValue
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.*

class KillauraABElement(iconResId: Int = AssetManager.getAsset("ic_sword_cross_black_24dp")) : Element(
    name = "KillauraAB",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_killauraab_display_name")
) {
    private val playerOnly by boolValue("玩家", false)
    private val mobsOnly by boolValue("生物", true)
    private val range by floatValue("范围", 3.7f, 2f..7f)
    private val delay by intValue("间隔", 5, 1..20)
    private val cps by intValue("CPS", 5, 1..20)
    private val packets by intValue("发包", 1, 1..10)
    private val derp by boolValue("Derp", true)
    private val mode by stringValue(this, "Mode", "Single", listOf("Single", "Multi", "switch"))
    private val switchDelay by intValue("Delay", 100, 50..200)

    private val notification by boolValue("通知", true)
    private val notificationInterval = 1000L

    private var lastAttack = 0L
    private var lastSwitchTime = 0L
    private var switchIndex = 0
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

        when (mode) {
            "Single" -> {
                val target = targets.first()
                attackTarget(target, now)
            }
            "switch" -> {
                if ((now - lastSwitchTime) >= switchDelay) {
                    if (switchIndex >= targets.size) switchIndex = 0
                    val target = targets[switchIndex++ % targets.size]
                    attackTarget(target, now)
                    lastSwitchTime = now
                }
            }
            "Multi" -> {
                targets.forEach { target -> attackTarget(target, now) }
            }
        }
    }

    private fun attackTarget(target: Entity, now: Long) {
        if (derp) spoofRotation(session.localPlayer, target)
        repeat(packets) { session.localPlayer.attack(target) }
        lastAttack = now
    }

    private fun spoofRotation(player: LocalPlayer, target: Entity) {
        val dx = target.vec3Position.x - player.vec3Position.x
        val dz = target.vec3Position.z - player.vec3Position.z
        val dy = target.vec3Position.y - player.vec3Position.y

        val yaw = Math.toDegrees(atan2(-dx, dz).toDouble()).toFloat()
        val pitch = Math.toDegrees((-atan2(dy, sqrt(dx * dx + dz * dz))).toDouble()).toFloat()

        session.clientBound(MovePlayerPacket().apply {
            runtimeEntityId = player.runtimeEntityId
            position = player.vec3Position
            rotation = Vector3f.from(yaw, pitch, yaw)
            mode = MovePlayerPacket.Mode.NORMAL
            isOnGround = true
            tick = (clientTickCounter++.and(0xFFFF)).toLong()
        })
    }

    private var clientTickCounter = 0
        get() = field++

    private fun showNotification(targets: List<Entity>, now: Long) {
        val target = targets.first()
        val coords = "(${target.vec3Position.x.toInt()}, ${target.vec3Position.y.toInt()}, ${target.vec3Position.z.toInt()})"
        val distance = String.format("%.1f", target.distance(session.localPlayer))
        val entityName = getEntityName(target)
        val targetCount = if (mode == "Multi") " (${targets.size} 个目标)" else ""

        session.showNotification(
            "正在攻击 $entityName$targetCount",
            "$coords | 距离: ${distance}b | CPS: $cps | 模式: ${getModeName()}",
            com.project.lumina.client.R.drawable.swords_24px
        )
        lastNotificationTime = now
    }

    private fun getModeName(): String = when (mode) {
        "Single" -> "单目标"
        "switch" -> "切换目标"
        "Multi" -> "多目标"
        else -> "未知"
    }

    private fun getEntityName(entity: Entity): String = when (entity) {
        is Player -> entity.username
        is EntityUnknown -> entity.identifier.substringAfter(':').replaceFirstChar { it.uppercase() }
        else -> "Unknown"
    }

    private fun getTargets(): List<Entity> = session.level.entityMap.values.filter {
        it.distance(session.localPlayer) <= range && it.isValid()
    }.sortedBy { it.distance(session.localPlayer) }

    private fun Entity.isValid(): Boolean = when (this) {
        is LocalPlayer -> false
        is Player -> (!mobsOnly) && !isBot()
        is EntityUnknown -> (!playerOnly) && isMob() && !isShadow()
        else -> false
    }

    private fun EntityUnknown.isMob(): Boolean = identifier in MobList.mobTypes

    private fun EntityUnknown.isShadow(): Boolean = identifier == "hivecommon:shadow"

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        return session.level.playerMap[uuid]?.name?.isBlank() ?: true
    }

    private val now get() = System.currentTimeMillis()
}