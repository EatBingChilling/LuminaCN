package com.phoenix.luminacn.game.module.impl.visual

import android.graphics.Canvas
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.game.entity.Player
import com.phoenix.luminacn.shiyi.EntityNameTag
import com.phoenix.luminacn.shiyi.RenderLayerView
import org.cloudburstmc.math.matrix.Matrix4f
import org.cloudburstmc.math.vector.Vector2d
import kotlin.math.cos
import kotlin.math.sin
import com.phoenix.luminacn.util.AssetManager

class NameTagElement(iconResId: Int = AssetManager.getAsset("ic_guy_fawkes_mask_black_24dp")) : Element(
    name = "NameTage",
    category = CheatCategory.Visual,
    iconResId,
    displayNameResId = AssetManager.getString("module_name_tag_display_name")
) {
    private val fovValue by intValue("Fov", 80, 40..110)
    private val showAllEntities by boolValue("Bots", false)
    private val originalSizeValue by boolValue("OriginalSize", false) // 默认改为false
    private val avoidScreenValue by boolValue("AvoidScreen", true)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
    }

    override fun onEnabled() {
        super.onEnabled()
        session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
        displayList.clear()
    }

    var displayList = HashMap<Player, EntityNameTag>()

    init {
        handle<RenderLayerView.EventRender> { event ->
            event.needRefresh = true
            if (avoidScreenValue && event.session.localPlayer.openContainer != null) return@handle
            
            val player = event.session.localPlayer
            val canvas = event.canvas
            
            // 修复屏幕尺寸获取
            val screenWidth: Int
            val screenHeight: Int
            
            if (originalSizeValue) {
                // 获取真实屏幕尺寸
                val context = canvas.context ?: return@handle
                val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                screenWidth = displayMetrics.widthPixels
                screenHeight = displayMetrics.heightPixels
            } else {
                screenWidth = canvas.width
                screenHeight = canvas.height
            }

            // 添加调试日志
            android.util.Log.d("NameTag", "Screen size: ${screenWidth}x${screenHeight}")

            val viewProjMatrix = Matrix4f.createPerspective(
                Math.toRadians((fovValue.toFloat() + 10).toDouble()).toFloat(), 
                screenWidth.toFloat() / screenHeight, 
                0.1f, 
                128f
            ).mul(
                Matrix4f.createTranslation(-player.posX.toFloat(), -player.posY.toFloat(), -player.posZ.toFloat())
                    .mul(rotY(-player.rotationYaw - 180))
                    .mul(rotX(-player.rotationPitch))
            )

            val entities = if (showAllEntities) {
                event.session.level.entityMap.values
            } else {
                event.session.level.entityMap.values.filterIsInstance<Player>()
            }

            android.util.Log.d("NameTag", "Found ${entities.size} entities to render")

            entities.forEach { entity ->
                if (entity == player) return@forEach

                if (entity is Player) {
                    drawEntityBox(entity, viewProjMatrix, screenWidth, screenHeight, canvas)
                }
            }
        }
    }

    private fun drawEntityBox(entity: Player, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas) {
        if (displayList[entity] == null) {
            displayList[entity] = EntityNameTag()
        }
        displayList[entity]!!.draw(entity, viewProjMatrix, screenWidth, screenHeight, canvas, this)
    }

    fun worldToScreen(posX: Double, posY: Double, posZ: Double, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2d? {
        // 添加调试日志
        android.util.Log.d("NameTag", "WorldToScreen: pos($posX, $posY, $posZ) screen(${screenWidth}x${screenHeight})")
        
        val w = viewProjMatrix.get(3, 0) * posX +
                viewProjMatrix.get(3, 1) * posY +
                viewProjMatrix.get(3, 2) * posZ +
                viewProjMatrix.get(3, 3)
        
        if (w < 0.01f) {
            android.util.Log.d("NameTag", "W value too small: $w")
            return null
        }
        
        val inverseW = 1 / w

        val screenX = screenWidth / 2f + (0.5f * ((viewProjMatrix.get(0, 0) * posX + viewProjMatrix.get(0, 1) * posY +
                viewProjMatrix.get(0, 2) * posZ + viewProjMatrix.get(0, 3)) * inverseW) * screenWidth)
        val screenY = screenHeight / 2f - (0.5f * ((viewProjMatrix.get(1, 0) * posX + viewProjMatrix.get(1, 1) * posY +
                viewProjMatrix.get(1, 2) * posZ + viewProjMatrix.get(1, 3)) * inverseW) * screenHeight)
        
        android.util.Log.d("NameTag", "Screen position: ($screenX, $screenY)")
        return Vector2d.from(screenX, screenY)
    }

    private fun rotX(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(
            1f, 0f, 0f, 0f,
            0f, c, -s, 0f,
            0f, s, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun rotY(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(
            c, 0f, s, 0f,
            0f, 1f, 0f, 0f,
            -s, 0f, c, 0f,
            0f, 0f, 0f, 1f
        )
    }
}