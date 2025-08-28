package com.phoenix.luminacn.game.module.impl.combat

import com.phoenix.luminacn.R
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.entity.*
import com.phoenix.luminacn.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType
import org.cloudburstmc.protocol.bedrock.packet.*
import kotlin.math.*

class KillauraV2Element(
    iconResId: Int = AssetManager.getAsset("ic_sword_cross_black_24dp")
) : Element(
    name = "KillAuraV2",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_killaurav2_display_name")
) {

    private var reachEnabled by boolValue("使用Reach", true)
    private var reachDistance by floatValue("攻击长度", 3f, 3f..10f)
    private var hitboxWidth by floatValue("碰撞箱宽度", 1.5f, 0.5f..8f)
    private var hitboxHeight by floatValue("碰撞箱高度", 1.5f, 0.5f..8f)
    private var hitboxScale by floatValue("碰撞箱尺寸", 1.5f, 0.1f..8f)
    private var hitboxPlayersOnly by boolValue("只修改玩家碰撞箱", true)
    private var hitboxMobsOnly by boolValue("只修改生物碰撞箱", false)
    private var attackDelayMs by intValue("攻击延迟(ms)", 1000, 1..1000)
    private var auraRange by floatValue("瞄准范围", 5.0f, 1.0f..10.0f)
    private var auraRotationSpeed by floatValue("瞄准速度", 2.0f, 1.0f..500.0f)
    private val hitboxParticles = 8
    private val hitboxVisualize = false
    private var lastParticleTime = 0L
    private val particleInterval = 500L
    private var lastAttackTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        val now = System.currentTimeMillis()

        if (reachEnabled && now - lastAttackTime >= attackDelayMs) {
            val targets = findReachTargets()
            if (targets.isNotEmpty()) {
                targets.forEach { target ->
                    session.localPlayer.swing()
                    session.localPlayer.attack(target)
                }
                lastAttackTime = now
            }
        }

        if (session.localPlayer.tickExists % 40 == 0L) {
            session.level.entityMap.values.forEach { entity ->
                if (entity.isHitboxTarget()) {
                    val meta = EntityDataMap().apply {
                        put(EntityDataTypes.WIDTH, hitboxWidth)
                        put(EntityDataTypes.HEIGHT, hitboxHeight)
                        put(EntityDataTypes.SCALE, hitboxScale)
                    }
                    session.clientBound(SetEntityDataPacket().apply {
                        runtimeEntityId = entity.runtimeEntityId
                        metadata = meta
                    })

                    if (hitboxVisualize && now - lastParticleTime >= particleInterval) {
                        visualizeHitbox(entity)
                        lastParticleTime = now
                    }
                }
            }
        }

        val auraTarget = findAuraTarget()
        if (auraTarget != null && now - lastAttackTime >= attackDelayMs) {
            rotateTowards(auraTarget)
            session.localPlayer.attack(auraTarget)
            lastAttackTime = now
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        if (!isSessionCreated) return
        session.level.entityMap.values.forEach { entity ->
            if (entity.isHitboxTarget()) {
                val meta = EntityDataMap().apply {
                    put(EntityDataTypes.WIDTH, 0.6f)
                    put(EntityDataTypes.HEIGHT, 1.8f)
                    put(EntityDataTypes.SCALE, 1.0f)
                }
                session.clientBound(SetEntityDataPacket().apply {
                    runtimeEntityId = entity.runtimeEntityId
                    metadata = meta
                })
            }
        }
    }

    private fun findReachTargets(): List<Entity> =
        session.level.entityMap.values
            .filter {
                it is Player &&
                        it !is LocalPlayer &&
                        !it.isBot() &&
                        it.distance(session.localPlayer) <= reachDistance &&
                        it.distance(session.localPlayer) > 0f
            }
            .sortedBy { it.distance(session.localPlayer) }

    private fun Entity.isHitboxTarget(): Boolean = when (this) {
        is LocalPlayer -> false
        is Player -> {
            when {
                hitboxPlayersOnly && !hitboxMobsOnly -> !isBot()
                hitboxPlayersOnly && hitboxMobsOnly  -> !isBot()
                else                                 -> false
            }
        }
        is EntityUnknown -> {
            when {
                hitboxMobsOnly && !hitboxPlayersOnly -> isMob()
                hitboxMobsOnly && hitboxPlayersOnly  -> isMob()
                else                                 -> false
            }
        }
        else -> false
    }

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        return session.level.playerMap[uuid]?.name?.isBlank() ?: true
    }

    private fun EntityUnknown.isMob(): Boolean =
        this.identifier in MobList.mobTypes

    private fun visualizeHitbox(entity: Entity) {
        val pos = entity.vec3Position
        val radius = hitboxWidth / 2f
        repeat(hitboxParticles) { i ->
            val angle = Math.toRadians((i * 360.0 / hitboxParticles))
            val x = pos.x + radius * kotlin.math.cos(angle)
            val z = pos.z + radius * kotlin.math.sin(angle)
            session.clientBound(EntityEventPacket().apply {
                runtimeEntityId = entity.runtimeEntityId
                type = EntityEventType.LOVE_PARTICLES
                data = 0
            })
        }
    }

    private fun findAuraTarget(): Entity? =
        session.level.entityMap.values
            .filter { it != session.localPlayer && it.distance(session.localPlayer) <= auraRange && it.isAuraTarget() }
            .sortedBy { it.distance(session.localPlayer) }
            .firstOrNull()

    private fun Entity.isAuraTarget(): Boolean = when (this) {
        is LocalPlayer -> false
        else           -> true
    }

    private fun rotateTowards(target: Entity) {
        val targetPos = target.vec3Position
        val playerPos = session.localPlayer.vec3Position
        val dx = targetPos.x - playerPos.x
        val dy = targetPos.y - playerPos.y
        val dz = targetPos.z - playerPos.z
        val yaw = Math.toDegrees(atan2(dz.toDouble(), dx.toDouble())).toFloat()
        val pitch = -Math.toDegrees(
            atan2(
                dy.toDouble(),
                sqrt((dx * dx + dz * dz).toDouble())
            )
        ).toFloat()
        val currentYaw = session.localPlayer.vec3Rotation.y
        val currentPitch = session.localPlayer.vec3Rotation.x
        val smoothYaw = lerpAngle(currentYaw, yaw)
        val smoothPitch = lerpAngle(currentPitch, pitch)

    }

    private fun lerpAngle(current: Float, target: Float): Float {
        val delta = (target - current + 180) % 360 - 180
        return current + delta * (auraRotationSpeed / 10f)
    }
}