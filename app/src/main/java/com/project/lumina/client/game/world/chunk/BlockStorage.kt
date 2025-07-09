package com.project.lumina.client.game.world.chunk

import com.project.lumina.client.game.world.chunk.palette.BitArray
import com.project.lumina.client.game.world.chunk.palette.BitArrayVersion
import com.project.lumina.client.game.world.chunk.palette.Pow2BitArray
import io.netty.buffer.ByteBuf
import org.cloudburstmc.protocol.common.util.VarInts

class BlockStorage {
    var bitArray: BitArray
    val palette = mutableListOf<Int>()

    constructor(version: BitArrayVersion, airId: Int) {
        bitArray = version.createPalette(MAX_BLOCKS)
        palette.add(airId)
    }

    constructor(buf: ByteBuf, network: Boolean) {
        val paletteHeader = buf.readByte().toInt()
        val paletteBits = paletteHeader shr 1
        val bitArrayVersion = BitArrayVersion.get(paletteBits, true)
        bitArray = bitArrayVersion.createPalette(MAX_BLOCKS)

        for (i in bitArray.getWords().indices) {
            bitArray.getWords()[i] = buf.readIntLE()
        }

        fun readInt(): Int {
            return if (network) VarInts.readInt(buf) else buf.readIntLE()
        }

        val paletteSize = readInt()
        repeat(paletteSize) {
            palette.add(readInt())
        }
    }

    fun getBlock(x: Int, y: Int, z: Int): Int {
        val index = (x shl 8) or (z shl 4) or y
        return palette[bitArray.get(index)]
    }

    fun setBlock(x: Int, y: Int, z: Int, id: Int) {
        val index = (x shl 8) or (z shl 4) or y
        val paletteIndex = palette.indexOf(id).takeIf { it != -1 } ?: run {
            palette.add(id)
            palette.lastIndex
        }
        bitArray.set(index, paletteIndex)
    }

    companion object {
        const val MAX_BLOCKS = 4096
    }
}
