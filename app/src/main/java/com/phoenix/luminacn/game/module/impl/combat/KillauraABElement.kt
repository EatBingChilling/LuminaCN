package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.entity.*
import com.phoenix.luminacn.game.module.api.setting.stringValue
import com.phoenix.luminacn.util.AssetManager
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
        repeat(packets) { session.localPlayer.attack(target) }
        lastAttack = now
    }

    private fun showNotification(targets: List<Entity>, now: Long) {
        val target = targets.first()
        val coords = "(${target.vec3Position.x.toInt()}, ${target.vec3Position.y.toInt()}, ${target.vec3Position.z.toInt()})"
        val distance = String.format("%.1f", target.distance(session.localPlayer))
        val entityName = getEntityName(target)
        val targetCount = if (mode == "Multi") " (${targets.size} 个目标)" else ""

        session.showNotification(
            "正在攻击 $entityName$targetCount",
            "$coords | 距离: ${distance}b | CPS: $cps | 模式: ${getStatusInfo()}",
            com.phoenix.luminacn.R.drawable.swords_24px
        )
        lastNotificationTime = now
    }

    override fun getStatusInfo(): String {
        return when (mode) {
            "Single" -> "单目标"
            "switch" -> "切换目标"
            "Multi" -> "多目标"
            else -> mode
        }
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