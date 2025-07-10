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

package com.project.lumina.client.overlay.mods

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.R
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow

class ClientOverlay(private val context: Context) : OverlayWindow() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    private var watermarkText by mutableStateOf(prefs.getString("text", "") ?: "")
    private var textColor by mutableStateOf(prefs.getInt("color", Color.WHITE))
    private var shadowEnabled by mutableStateOf(prefs.getBoolean("shadow", true))
    private var fontSize by mutableStateOf(prefs.getInt("size", 18))

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private var overlayInstance: ClientOverlay? = null
        private var shouldShowOverlay = true

        fun showOverlay(context: Context) {
            if (shouldShowOverlay) {
                overlayInstance = ClientOverlay(context)
                try {
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

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) {
                dismissOverlay()
            }
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay
    }

    fun showConfigDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.overlay_config_dialog, null)
        val editText = dialogView.findViewById<EditText>(R.id.editText)
        val seekRed = dialogView.findViewById<SeekBar>(R.id.seekRed)
        val seekGreen = dialogView.findViewById<SeekBar>(R.id.seekGreen)
        val seekBlue = dialogView.findViewById<SeekBar>(R.id.seekBlue)
        val switchShadow = dialogView.findViewById<Switch>(R.id.switchShadow)
        val seekSize = dialogView.findViewById<SeekBar>(R.id.seekSize)

        editText.setText(watermarkText)
        seekRed.progress = Color.red(textColor)
        seekGreen.progress = Color.green(textColor)
        seekBlue.progress = Color.blue(textColor)
        switchShadow.isChecked = shadowEnabled
        seekSize.progress = fontSize

        val dialog = AlertDialog.Builder(context)
            .setTitle("配置水印")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                watermarkText = editText.text.toString()
                val red = seekRed.progress
                val green = seekGreen.progress
                val blue = seekBlue.progress
                textColor = Color.rgb(red, green, blue)
                shadowEnabled = switchShadow.isChecked
                fontSize = seekSize.progress

                prefs.edit()
                    .putString("text", watermarkText)
                    .putInt("color", textColor)
                    .putBoolean("shadow", shadowEnabled)
                    .putInt("size", fontSize)
                    .apply()
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val firaSansFamily = FontFamily(Font(R.font.packet))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            contentAlignment = Alignment.TopStart
        ) {
            val text = "LuminaCN${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"

            if (shadowEnabled) {
                for (i in 1..5) {
                    Text(
                        text = text,
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = firaSansFamily,
                        color = ComposeColor.Black.copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(start = 8.dp, top = 13.dp, bottom = 8.dp)
                            .offset(x = (i * 0.5f).dp, y = (i * 0.5f).dp)
                    )
                }
            }

            Text(
                text = text,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = firaSansFamily,
                color = ComposeColor(textColor),
                modifier = Modifier
                    .padding(start = 8.dp, top = 13.dp, bottom = 8.dp)
            )
        }
    }
}
