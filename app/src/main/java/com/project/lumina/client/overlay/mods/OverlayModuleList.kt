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

package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.CPPBridge.NativeHsvToRgb
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import com.project.lumina.client.ui.theme.*

class OverlayModuleList : OverlayWindow() {
    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.END
            x = 10 
            y = 5
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private val moduleState = ModuleState()
        private val overlayInstance by lazy { OverlayModuleList() }
        private var shouldShowOverlay = false
        private var capitalizeAndMerge = false 
        private var displayMode = "None" 

        val moduleColors = listOf(
            Color(0xFFff5555), 
            Color(0xFF55ff55), 
            Color(0xFF5555ff), 
            Color(0xFFffff55)  
        )

        private fun hsvToRgb(h: Float, s: Float, v: Float): Color {
            val rgb = NativeHsvToRgb.hsvToRgb(h) 
            return Color(
                red = rgb[0].coerceIn(0f, 1f),
                green = rgb[1].coerceIn(0f, 1f),
                blue = rgb[2].coerceIn(0f, 1f)
            )
        }

        fun showText(moduleName: String) {
            if (shouldShowOverlay) {
                moduleState.addModule(moduleName)
                try {
                    OverlayManager.showOverlayWindow(overlayInstance)
                } catch (e: Exception) {
                    
                }
            }
        }

        fun removeText(moduleName: String) {
            moduleState.removeModule(moduleName)
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) {
                try {
                    OverlayManager.dismissOverlayWindow(overlayInstance)
                } catch (e: Exception) {
                    
                }
            }
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        
        fun setCapitalizeAndMerge(enabled: Boolean) {
            capitalizeAndMerge = enabled
        }

        fun getCapitalizeAndMerge(): Boolean = capitalizeAndMerge

        fun setDisplayMode(mode: String) {
            if (mode in listOf("None", "Bar", "Split", "Outline")) {
                displayMode = mode
            } else {
                throw IllegalArgumentException("Invalid display mode. Must be one of: None, Bar, Split, Outline")
            }
        }

        fun getDisplayMode(): String = displayMode
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        var overlayVisible by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            overlayVisible = true
        }

        val overlayAlpha by animateFloatAsState(
            targetValue = if (overlayVisible) 1f else 0f,
            animationSpec = tween(500, easing = EaseOutCubic),
            label = "overlayAlpha"
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .padding(8.dp)
                .alpha(overlayAlpha),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            
            val sortedModules = moduleState.modules.sortedByDescending { it.name.length }
            sortedModules.forEachIndexed { index, module ->
                key(module.id) {
                    ModuleItem(
                        module = module,
                        moduleState = moduleState,
                        index = index,
                        textBrush = Brush.horizontalGradient(
                            listOf(
                                OArrayList1,
                                OArrayList2
                            )
                        ),
                        displayMode = "None",
                        entryDelay = index * 50,
                        capitalizeAndMerge = false
                    )
                }
            }
        }
    }

    @Composable
    fun ModuleItem(
        module: ModuleItem,
        moduleState: ModuleState,
        index: Int,
        textBrush: Brush,
        displayMode: String,
        entryDelay: Int = 0,
        capitalizeAndMerge: Boolean
    ) {
        var visible by remember { mutableStateOf(false) }
        var exitState by remember { mutableStateOf(false) }
        val isEnabled by remember { derivedStateOf { moduleState.isModuleEnabled(module.name) } }
        val isMarkedForRemoval by remember { derivedStateOf { moduleState.modulesToRemove.contains(module.name) } }

        LaunchedEffect(Unit) {
            delay(entryDelay.toLong())
            visible = true
        }

        LaunchedEffect(isMarkedForRemoval) {
            if (isMarkedForRemoval) {
                exitState = true
                delay(300) 
                moduleState.removeModule(module.name)
            }
        }

        val offsetX by animateFloatAsState(
            targetValue = when {
                exitState -> 200f
                visible -> 0f
                else -> -200f
            },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "offsetX"
        )

        val alpha by animateFloatAsState(
            targetValue = if (visible && !exitState) 1f else 0f,
            animationSpec = tween(300),
            label = "alpha"
        )

        val scale by animateFloatAsState(
            targetValue = when {
                exitState -> 0.8f
                visible -> 1f
                else -> 0.8f
            },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "scale"
        )

        val baseColor = OArrayBase

        Row(
            modifier = Modifier
                .offset(x = offsetX.dp)
                .alpha(alpha)
                .scale(scale)
                .wrapContentWidth()
                .height(28.dp)
                .padding(horizontal = 4.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(4.dp),
                    spotColor = baseColor.copy(alpha = 0.5f)
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.3f),
                            baseColor.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isEnabled) {
                        moduleState.markForRemoval(module.name)
                    }
                }
        ) {
            Text(
                text = if (capitalizeAndMerge) {
                    module.name.split(" ").joinToString("") { word ->
                        word.replaceFirstChar { it.uppercase() }
                    }
                } else {
                    module.name
                },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(
                    shadow = Shadow(
                        color = baseColor.copy(alpha = 0.7f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

@Composable
fun EmptyStateMessage(gradientPosition: Float) {
    var colors by remember { mutableStateOf(listOf(Color.Red, Color.Blue, Color.Green, Color.Red)) }

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(1000f * gradientPosition, 1000f * gradientPosition)
    )

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val slideAnim by animateFloatAsState(
        targetValue = if (visible) 0f else 200f,
        animationSpec = tween(400, easing = FluidEasing),
        label = "emptyStateSlide"
    )

    val alphaAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "emptyStateAlpha"
    )

    Box(
        modifier = Modifier
            .offset(x = slideAnim.roundToInt().dp)
            .alpha(alphaAnim)
            .shadow(8.dp, shape = RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "No enabled artifacts",
            fontSize = 12.sp,
            style = TextStyle(
                brush = brush,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.9f),
                    offset = Offset(1f, 1f),
                    blurRadius = 2f
                )
            )
        )
    }
}


private val CyberColors = listOf(
    Color(0xFF00E5FF), 
    Color(0xFF00B8D4), 
    Color(0xFF64FFDA), 
    Color(0xFF18FFFF)  
)

class ModuleState {
    private val _modules = mutableStateListOf<ModuleItem>()
    val modules: List<ModuleItem> get() = _modules.toList()
    private var nextId = 0
    private val _modulesToRemove = mutableStateListOf<String>()
    val modulesToRemove: List<String> get() = _modulesToRemove.toList()

    fun addModule(moduleName: String) {
        if (_modules.none { it.name == moduleName }) {
            _modules.add(ModuleItem(nextId++, moduleName))
            _modulesToRemove.remove(moduleName) 
        }
    }

    fun markForRemoval(moduleName: String) {
        if (!_modulesToRemove.contains(moduleName)) {
            _modulesToRemove.add(moduleName)
        }
    }

    fun removeModule(moduleName: String) {
        _modules.removeAll { it.name == moduleName }
        _modulesToRemove.remove(moduleName)
    }

    fun isModuleEnabled(moduleName: String): Boolean {
        return _modules.any { it.name == moduleName } && !_modulesToRemove.contains(moduleName)
    }

    fun toggleModule(moduleName: String) {
        if (isModuleEnabled(moduleName)) {
            markForRemoval(moduleName)
        } else {
            addModule(moduleName)
        }
    }
}

data class ModuleItem(val id: Int, val name: String)


private val FluidEntryEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
private val FluidExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val FluidEasing = CubicBezierEasing(0.43f, 0.13f, 0.23f, 0.96f)
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val FastOutSlowInEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)