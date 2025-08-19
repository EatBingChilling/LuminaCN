package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.entity.LocalPlayer
import com.phoenix.luminacn.game.entity.Player
import com.phoenix.luminacn.game.entity.Entity
import com.phoenix.luminacn.game.entity.EntityUnknown
import com.phoenix.luminacn.game.entity.MobList
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.util.AssetManager
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class KillauraCDElement : Element(
    name = "KillauraCD",
    category = CheatCategory.Combat,
    iconResId = AssetManager.getAsset("ic_sword_cross_black_24dp"),
    displayNameResId = AssetManager.getString("module_killauracd_display_name")
) {
    private val mode by intValue("模式", 0, 0..2)
    private val cps by intValue("CPS", 5, 1..20)
    private val radius by floatValue("范围", 3.7f, 2f..7f)
    private val playerOnly by boolValue("玩家", false)
    private val mobsOnly by boolValue("生物", true)
    private val switchDelay by intValue("切换延迟", 100, 50..200)

    private var lastAttack = 0L
    private var lastSwitchTime = 0L
    private var switchIndex = 0
    private var clientTickCounter = 0

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || interceptablePacket.packet !is PlayerAuthInputPacket) return

        val packet = interceptablePacket.packet as PlayerAuthInputPacket
        val now = System.currentTimeMillis()
        val minDelay = 1000L / cps

        if ((now - lastAttack) < minDelay) return

        val targets = getTargets()
        if (targets.isEmpty()) return

        when (mode) {
            0 -> {
                val target = targets.first()
                attackTarget(target, now)
            }
            1 -> {
                if ((now - lastSwitchTime) >= switchDelay) {
                    if (switchIndex >= targets.size) switchIndex = 0
                    val target = targets[switchIndex++ % targets.size]
                    attackTarget(target, now)
                    lastSwitchTime = now
                }
            }
            2 -> {
                targets.forEach { target -> attackTarget(target, now) }
            }
        }
    }

    private fun attackTarget(target: Entity, now: Long) {
        repeat(1) { session.localPlayer.attack(target) }
        lastAttack = now
    }

    private fun getTargets(): List<Entity> = session.level.entityMap.values
        .filter { it.isValid() && it.distance(session.localPlayer) <= radius }
        .sortedBy { it.distance(session.localPlayer) }

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

    private fun Entity.distance(other: Entity): Float {
        return this.vec3Position.distance(other.vec3Position)
    }
}