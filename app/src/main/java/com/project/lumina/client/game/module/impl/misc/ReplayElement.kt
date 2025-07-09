/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * This is open source — not open credit.
 *
 * If you're here to build, welcome. If you're here to repaint and reupload
 * with your tag slapped on it… you're not fooling anyone.
 *
 * Changing colors and class names doesn't make you a developer.
 * Copy-pasting isn't contribution.
 *
 * You have legal permission to fork. But ask yourself — are you improving,
 * or are you just recycling someone else's work to feed your ego?
 *
 * Open source isn't about low-effort clones or chasing clout.
 * It's about making things better. Sharper. Cleaner. Smarter.
 *
 * So go ahead, fork it — but bring something new to the table,
 * or don't bother pretending.
 *
 * This message is philosophical. It does not override your legal rights under GPLv3.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * GPLv3 Summary:
 * - You have the freedom to run, study, share, and modify this software.
 * - If you distribute modified versions, you must also share the source code.
 * - You must keep this license and copyright intact.
 * - You cannot apply further restrictions — the freedom stays with everyone.
 * - This license is irrevocable, and applies to all future redistributions.
 *
 * Full text: https://www.gnu.org/licenses/gpl-3.0.html
 */

package com.project.lumina.client.game.module.impl.misc

import com.project.lumina.client.R
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.* 
import org.cloudburstmc.math.vector.Vector3f 
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.project.lumina.client.application.AppContext 
import java.io.File
import kotlin.concurrent.thread

class ReplayElement : Element(
    name = "Replay",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_replay_display_name")
) {

    private val recordingInterval by intValue("间隔", 50, 20..200)
    private val autoSave by boolValue("自动保存", true)
    private val playbackSpeed by floatValue("速度", 1.0f, 0.1f..3.0f)
    private val recordInputs by boolValue("记录输入", true)

    @Serializable
    private data class ReplayFrame(
        val position: Vector3fData,
        val rotation: Vector3fData,
        val inputs: Set<String>,
        val timestamp: Long
    )

    @Serializable
    private data class Vector3fData(
        val x: Float,
        val y: Float,
        val z: Float
    )

    private var isRecording = false
    private var isPlaying = false
    private val frames = mutableListOf<ReplayFrame>()
    private var recordingStartTime = 0L
    private var lastRecordTime = 0L
    private var playbackThread: Thread? = null
    private var originalPosition: Vector3f? = null

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            if (isRecording) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastRecordTime >= recordingInterval) {
                    frames.add(
                        ReplayFrame(
                        Vector3fData(
                            packet.position.x,
                            packet.position.y,
                            packet.position.z
                        ),
                        Vector3fData(
                            packet.rotation.x,
                            packet.rotation.y,
                            packet.rotation.z
                        ),
                        packet.inputData.map { it.name }.toSet(),
                        currentTime - recordingStartTime
                    )
                    )
                    lastRecordTime = currentTime
                }
            }

            if (isPlaying) {
                interceptablePacket.intercept()
            }
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        try {
            session.displayClientMessage("""
                §l§b[Replay] §r§7命令:
                §f.replay record §7- 开始录制
                §f.replay play §7- 播放最后一次录制
                §f.replay stop §7- 停止录制或回放
                §f.replay save <名称> §7- 保存录制
                §f.replay load <名称> §7- 加载录制
            """.trimIndent())
        } catch (e: Exception) {
            println("Error displaying Replay commands: ${e.message}")
        }
    }

    fun startRecording() {
        if (isPlaying) {
            session.displayClientMessage("§c不能在播放时开始录制")
            return
        }

        frames.clear()
        isRecording = true
        recordingStartTime = System.currentTimeMillis()
        lastRecordTime = recordingStartTime

        
        originalPosition = session.localPlayer.vec3Position

        session.displayClientMessage("§a开始录制行走路径")
    }

    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        if (autoSave) {
            saveReplay("replay_${System.currentTimeMillis()}")
        }
        session.displayClientMessage("§c停止录制，共 (${frames.size} 帧)")
    }

    fun startPlayback() {
        if (isRecording) {
            session.displayClientMessage("§c不能在录制过程中播放回放")
            return
        }

        if (frames.isEmpty()) {
            session.displayClientMessage("§c没有录制到任何帧")
            return
        }

        isPlaying = true

        
        originalPosition = session.localPlayer.vec3Position

        playbackThread = thread(name = "ReplayPlayback") {
            try {
                frames.forEachIndexed { index, frame ->
                    if (!isPlaying) return@thread

                    val delay = if (index < frames.size - 1) {
                        ((frames[index + 1].timestamp - frame.timestamp) / playbackSpeed).toLong()
                    } else 0L

                    
                    session.clientBound(SetEntityMotionPacket().apply {
                        runtimeEntityId = session.localPlayer.runtimeEntityId
                        motion = Vector3f.from(
                            frame.position.x,
                            frame.position.y,
                            frame.position.z
                        )
                    })

                    
                    session.clientBound(MovePlayerPacket().apply {
                        runtimeEntityId = session.localPlayer.runtimeEntityId
                        position = Vector3f.from(
                            frame.position.x,
                            frame.position.y,
                            frame.position.z
                        )
                        rotation = Vector3f.from(
                            frame.rotation.x,
                            frame.rotation.y,
                            frame.rotation.z
                        )
                        mode = MovePlayerPacket.Mode.NORMAL
                    })

                    
                    if (recordInputs) {
                        session.clientBound(PlayerAuthInputPacket().apply {
                            position = Vector3f.from(
                                frame.position.x,
                                frame.position.y,
                                frame.position.z
                            )
                            rotation = Vector3f.from(
                                frame.rotation.x,
                                frame.rotation.y,
                                frame.rotation.z
                            )
                            
                            val inputs = frame.inputs.mapNotNull { inputName ->
                                try {
                                    PlayerAuthInputData.valueOf(inputName)
                                } catch (e: IllegalArgumentException) {
                                    null
                                }
                            }
                            
                            inputData.addAll(inputs)
                        })
                    }

                    Thread.sleep(delay)
                }
            } catch (e: InterruptedException) {
                
            } finally {
                isPlaying = false
                
                originalPosition?.let { pos ->
                    session.clientBound(MovePlayerPacket().apply {
                        runtimeEntityId = session.localPlayer.runtimeEntityId
                        position = pos
                        rotation = session.localPlayer.vec3Rotation
                        mode = MovePlayerPacket.Mode.NORMAL
                    })
                }
                session.displayClientMessage("§e回放结束")
            }
        }
    }

    fun stopPlayback() {
        if (!isPlaying) return

        isPlaying = false
        playbackThread?.interrupt()
        playbackThread = null

        
        originalPosition?.let { pos ->
            session.clientBound(MovePlayerPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                position = pos
                rotation = session.localPlayer.vec3Rotation
                mode = MovePlayerPacket.Mode.NORMAL
            })
        }
        session.displayClientMessage("§c回放停止")
    }

    fun saveReplay(name: String) {
        if (frames.isEmpty()) {
            session.displayClientMessage("§c没有需要保存的帧")
            return
        }

        try {
            val replayDir = File(AppContext.instance.filesDir, "replays").apply {
                mkdirs()
            }
            val file = File(replayDir, "$name.json")
            file.writeText(Json.encodeToString(frames))
            session.displayClientMessage("§aSaved replay to $name")
        } catch (e: Exception) {
            session.displayClientMessage("§cFailed to save replay: ${e.message}")
        }
    }

    fun loadReplay(name: String) {
        try {
            val file = File(File(AppContext.instance.filesDir, "replays"), "$name.json")
            if (!file.exists()) {
                session.displayClientMessage("§c回放文件找不到")
                return
            }

            frames.clear()
            frames.addAll(Json.decodeFromString(file.readText()))
            session.displayClientMessage("§a已加载回放文件，内含 ${frames.size} 个帧")
        } catch (e: Exception) {
            session.displayClientMessage("§c回放加载失败: ${e.message}")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        stopRecording()
        stopPlayback()
    }
}