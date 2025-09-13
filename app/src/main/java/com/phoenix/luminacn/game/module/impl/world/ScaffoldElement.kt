package com.phoenix.luminacn.game.module.impl.world

import android.util.Log
import com.phoenix.luminacn.R
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.entity.LocalPlayer
import com.phoenix.luminacn.game.inventory.PlayerInventory
import com.phoenix.luminacn.game.registry.BlockMapping
import com.phoenix.luminacn.game.world.World
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventorySource
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.floor

class ScaffoldElement(iconResId: Int = R.drawable.ic_cube_outline_black_24dp) : Element(
    name = "Scaffold",
    category = CheatCategory.World,
    iconResId = iconResId,
    displayNameResId = R.string.module_scaffold_display_name
) {

    private var lastPlaceTime = 0L
    private val placeCooldown = 200L 
    private val lookaheadTime = 0.1f 
    private val eyeHeight = 1.62f

    override fun onEnabled() {
        super.onEnabled()
        Log.d("Scaffold", "Scaffold enabled")
    }

    override fun onDisabled() {
        super.onDisabled()
        Log.d("Scaffold", "Scaffold disabled")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket || !isEnabled) return

        val player = session.localPlayer
        val world = session.world
        val inventory = player.inventory as PlayerInventory
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastPlaceTime < placeCooldown) return
        lastPlaceTime = currentTime

        val pos = packet.position
        val velocity = Vector3f(packet.motion.x, packet.motion.y, packet.motion.z)
        val pitch = packet.rotation.x
        val yaw = packet.rotation.y

        val isMoving = velocity.lengthSquared() > 0.01f
        val blockBelow = world.getBlockId(
            floor(pos.x).toInt(),
            floor(pos.y - 1).toInt(),
            floor(pos.z).toInt()
        )
        if (!isMoving || blockBelow != 0) return

        val targetPos = calculateTargetPosition(pos, velocity, pitch, yaw)
        val blockFace = determineBlockFace(pitch, yaw)

        val blockSlot = findSuitableBlock(inventory) ?: return
        val blockItem = inventory.content[blockSlot]
        if (blockItem == ItemData.AIR) return

        val adjustedPitch = if (pitch > -45) pitch - 10 else pitch
        packet.rotation = Vector3f(adjustedPitch, yaw, 0f)


        val transactionPacket = createTransactionPacket(player, targetPos, blockFace, blockItem, blockSlot)
        session.serverBound(transactionPacket)


        inventory.updateItem(session, blockSlot)
        inventory.notifySlotUpdate(session, blockSlot)
    }

    private fun calculateTargetPosition(pos: Vector3f, velocity: Vector3f, pitch: Float, yaw: Float): Vector3i {

        var x = floor(pos.x).toInt()
        var y = floor(pos.y - 1).toInt()
        var z = floor(pos.z).toInt()


        val lookahead = velocity.mul(lookaheadTime)
        x += floor(lookahead.x).toInt()
        z += floor(lookahead.z).toInt()


        if (pitch > 45) { 
            y += 1
        } else if (pitch < -45) {

            val direction = getDirectionFromYaw(yaw)
            when (direction) {
                "north" -> z -= 1
                "south" -> z += 1
                "west" -> x -= 1
                "east" -> x += 1
            }
        }

        return Vector3i.from(x, y, z)
    }

    private fun determineBlockFace(pitch: Float, yaw: Float): Int {
        return when {
            pitch > 45 -> 1 
            pitch < -45 -> {
                val direction = getDirectionFromYaw(yaw)
                when (direction) {
                    "north" -> 2 // North face
                    "south" -> 3 // South face
                    "west" -> 4 // West face
                    "east" -> 5 // East face
                    else -> 2 // Default north
                }
            }
            else -> 1 // Default top face
        }
    }

    private fun getDirectionFromYaw(yaw: Float): String {
        val angle = (yaw % 360 + 360) % 360
        return when {
            angle in 315..360 || angle in 0..45 -> "south"
            angle in 45..135 -> "west"
            angle in 135..225 -> "north"
            angle in 225..315 -> "east"
            else -> "south"
        }
    }

    private fun findSuitableBlock(inventory: PlayerInventory): Int? {
        return inventory.searchForItemIndexed { slot, item ->
            item.itemDefinition.isBlock() && item != ItemData.AIR
        }
    }

    private fun createTransactionPacket(
        player: LocalPlayer,
        targetPos: Vector3i,
        blockFace: Int,
        blockItem: ItemData,
        blockSlot: Int
    ): InventoryTransactionPacket {
        return InventoryTransactionPacket().apply {
            transactionType = InventoryTransactionType.ITEM_USE
            actionType = 1 // CLICK_BLOCK
            blockPosition = targetPos
            blockFace = blockFace
            hotbarSlot = blockSlot
            itemInHand = blockItem
            playerPosition = player.vec3Position.add(0f, eyeHeight, 0f) 
            clickPosition = Vector3f.ZERO
            actions.add(
                InventoryActionData(
                    source = InventorySource.fromContainerWindowId(0),
                    slot = blockSlot,
                    fromItem = blockItem,
                    toItem = ItemData.AIR
                )
            )
        }
    }
}
