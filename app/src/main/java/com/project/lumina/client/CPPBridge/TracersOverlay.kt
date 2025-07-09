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
 * or don’t bother pretending.
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

package com.project.lumina.client.CPPBridge

import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow


import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.project.lumina.client.constructors.NetBound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.cloudburstmc.math.vector.Vector3f
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class EntityData(
    val position: Vector3f,
    val color: Color = Color.Red,
    val type: EntityType = EntityType.PLAYER,
    val heightOffset: Float = 0.0f 
)

enum class EntityType {
    PLAYER, ENEMY, NEUTRAL
}

class TracersOverlay : OverlayWindow() {
    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private var overlayInstance: TracersOverlay? = null
        private var shouldShowOverlay = false
        private val scope = CoroutineScope(Dispatchers.Main)
        private var entities by mutableStateOf<List<EntityData>>(emptyList())
        private var playerPosition by mutableStateOf(Vector3f.from(0f, 0f, 0f))
        private var playerYaw by mutableStateOf(0f)
        private var playerPitch by mutableStateOf(0f)
        private var currentLineWidth by mutableStateOf(1.5f)

        private fun calculatePriorityColor(entity: EntityData): Color {
            return when (entity.type) {
                EntityType.ENEMY -> Color.Red
                EntityType.PLAYER -> Color.Green
                EntityType.NEUTRAL -> Color.Yellow
            }
        }

        private fun calculateTracerOpacity(distance: Float): Float {
            val maxDistance = 100f
            return maxOf(0f, 1f - (distance / maxDistance))
        }

        private fun calculateDistance(pos1: Vector3f, pos2: Vector3f): Float {
            return sqrt(
                (pos1.x - pos2.x).pow(2) +
                        (pos1.y - pos2.y).pow(2) +
                        (pos1.z - pos2.z).pow(2)
            )
        }

        fun updateLineWidth(width: Float) {
            currentLineWidth = width
        }

        fun projectToScreenAdvanced(
            position: Vector3f,
            playerPos: Vector3f,
            playerYaw: Float,
            playerPitch: Float,
            screenWidth: Float,
            screenHeight: Float
        ): Offset? {
            val relativePos = Vector3f.from(
                position.x - playerPos.x,
                position.y - playerPos.y,
                position.z - playerPos.z
            )

            
            val yawRad = Math.toRadians(playerYaw.toDouble()).toFloat()
            val rotatedX = relativePos.x * cos(yawRad) + relativePos.z * sin(yawRad)
            val rotatedZ = -relativePos.x * sin(yawRad) + relativePos.z * cos(yawRad)
            val rotatedRelPos = Vector3f.from(rotatedX, relativePos.y, rotatedZ)

            
            val pitchRad = Math.toRadians(playerPitch.toDouble()).toFloat()
            val rotatedY = rotatedRelPos.y * cos(pitchRad) - rotatedRelPos.z * sin(pitchRad)
            val rotatedZ2 = rotatedRelPos.y * sin(pitchRad) + rotatedRelPos.z * cos(pitchRad)

            
            if (rotatedZ2 <= 0) return null

            
            val screenX = screenWidth / 2f + (rotatedX / rotatedZ2) * (screenWidth / 2f)
            val screenY = screenHeight / 2f - (rotatedY / rotatedZ2) * (screenHeight / 2f)

            
            if (screenX < 0 || screenX > screenWidth || screenY < 0 || screenY > screenHeight) {
                return null
            }

            return Offset(screenX, screenY)
        }

        fun showOverlay() {
            if (shouldShowOverlay) {
                try {
                    overlayInstance = overlayInstance ?: TracersOverlay()
                    OverlayManager.showOverlayWindow(overlayInstance!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun dismissOverlay() {
            try {
                overlayInstance?.let { OverlayManager.dismissOverlayWindow(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun setOverlayEnabled(enabled: Boolean, NetBound: NetBound? = null) {
            shouldShowOverlay = enabled
            if (enabled && NetBound != null) showOverlay() else dismissOverlay()
        }

        fun updateEntities(newEntities: List<EntityData>) {
            entities = newEntities.map { entity ->
                val offsetPos = Vector3f.from(
                    entity.position.x,
                    entity.position.y + entity.heightOffset,
                    entity.position.z
                )
                entity.copy(position = offsetPos)
            }
        }

        fun updatePlayerPosition(position: Vector3f, yaw: Float, pitch: Float) {
            playerPosition = position
            playerYaw = yaw
            playerPitch = pitch
        }
    }

    @Composable
    override fun Content() {
        if (!shouldShowOverlay) return
        TracersDisplay(entities, playerPosition, playerYaw, playerPitch, currentLineWidth)
    }

    @Composable
    private fun TracersDisplay(
        entities: List<EntityData>,
        playerPos: Vector3f,
        yaw: Float,
        pitch: Float,
        lineWidth: Float 
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenCenter = Offset(size.width / 2f, size.height / 2f)

            entities.forEach { entity ->
                val screenPos = projectToScreenAdvanced(
                    entity.position,
                    playerPos,
                    yaw,
                    pitch,
                    size.width,
                    size.height
                )

                if (screenPos != null) {
                    val distance = calculateDistance(entity.position, playerPos)
                    val color = calculatePriorityColor(entity).copy(alpha = calculateTracerOpacity(distance))

                    drawLine(
                        color = color,
                        start = screenCenter,
                        end = screenPos,
                        strokeWidth = lineWidth.dp.toPx(), 
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}