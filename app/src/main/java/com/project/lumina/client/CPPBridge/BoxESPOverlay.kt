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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

import org.cloudburstmc.math.vector.Vector3f

object BoxESPOverlay : OverlayWindow() {

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

    private var shouldShowOverlay by mutableStateOf(false)
    private var entities by mutableStateOf<List<Vector3f>>(emptyList())
    private var playerPos by mutableStateOf(Vector3f.from(0f, 0f, 0f))

    fun setOverlayEnabled(enabled: Boolean) {
        shouldShowOverlay = enabled
        if (enabled) OverlayManager.showOverlayWindow(this)
        else OverlayManager.dismissOverlayWindow(this)
    }

    fun updatePlayerPosition(pos: Vector3f) {
        playerPos = pos
    }

    fun updateEntities(list: List<Vector3f>) {
        entities = list
    }

    @Composable
    override fun Content() {
        if (!shouldShowOverlay) return
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenWidth = size.width
            val screenHeight = size.height

            entities.forEach { entity ->
                val halfW = 0.3f
                val height = 1.8f

                val corners = listOf(
                    Vector3f.from(entity.x - halfW, entity.y, entity.z - halfW),
                    Vector3f.from(entity.x + halfW, entity.y + height, entity.z + halfW)
                )

                val screenCorners = corners.mapNotNull {
                    TracersOverlay.projectToScreenAdvanced(
                        it, playerPos, 0f, 0f, screenWidth, screenHeight
                    )
                }

                if (screenCorners.size == 2) {
                    val (min, max) = screenCorners.sortedBy { it.x + it.y }
                    drawRect(
                        color = Color.Cyan,
                        topLeft = Offset(min.x, min.y),
                        size = Size(
                            max.x - min.x,
                            max.y - min.y
                        ),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
