package com.phoenix.luminacn.game.module.impl.world

import com.phoenix.luminacn.R
import com.phoenix.luminacn.application.AppContext
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.world.World
import com.phoenix.luminacn.game.world.chunk.Chunk
import com.phoenix.luminacn.game.world.save.LevelDBWorld
import com.phoenix.luminacn.game.world.save.LevelDBLevelData
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import java.io.File

class WorldSaveTesterElement : Element(
    name = "world_save_test",
    category = CheatCategory.World,
    displayNameResId = R.string.module_world_save_test_display_name
) {
    private var hasRun = false

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || hasRun) return

        if (interceptablePacket.packet !is PlayerAuthInputPacket) return

        val world = session.world
        val player = session.localPlayer

        val pos = Vector3i.from(
            player.posX.toInt(),
            player.posY.toInt(),
            player.posZ.toInt()
        )

        val chunk = getAccessibleChunk(world, pos.x, pos.z)
        if (chunk == null) {
            session.displayClientMessage("❌ 玩家位置为加载区块")
            isEnabled = false
            return
        }

        val folder = File(AppContext.instance.filesDir, "world_saves")
        if (!folder.exists()) folder.mkdirs()


        val dbWorld = LevelDBWorld(folder)
        dbWorld.saveChunk(chunk)
        dbWorld.close()

        session.displayClientMessage("✅ 区块位置 [${chunk.x}, ${chunk.z}] 保存到 LevelDB.")


        val levelDatFile = File(folder, "level.dat")
        val levelData = LevelDBLevelData(protocol = 649, inventoryVersion = "1.20.73")
        levelDatFile.writeBytes(levelData.toBytes())

        session.displayClientMessage("✅ 写入 level.dat (name: ${levelData.name})")

        hasRun = true
        isEnabled = false
    }

    private fun getAccessibleChunk(world: World, x: Int, z: Int): Chunk? {
        return try {
            val method = world::class.java.getDeclaredMethod("getChunkAt", Int::class.java, Int::class.java)
            method.isAccessible = true
            method.invoke(world, x, z) as? Chunk
        } catch (e: Exception) {
            null
        }
    }
}
