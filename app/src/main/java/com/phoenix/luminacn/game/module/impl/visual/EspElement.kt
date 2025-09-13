package com.phoenix.luminacn.game.module.impl.visual

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.entity.Entity
import com.phoenix.luminacn.game.entity.Player
import com.phoenix.luminacn.game.module.api.setting.stringValue
import com.phoenix.luminacn.shiyi.RenderOverlayView
import com.phoenix.luminacn.util.AssetManager
import com.phoenix.luminacn.util.ColorUtils
import org.cloudburstmc.math.matrix.Matrix4f
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.math.vector.Vector3f
import kotlin.math.cos
import kotlin.math.sin

class EspElement(iconResId: Int = AssetManager.getAsset("ic_eye_black_24dp")) : Element(
    name = "ESP",
    category = CheatCategory.Visual,
    iconResId,
    displayNameResId = AssetManager.getString("module_esp_display_name")
) {
    companion object {
        private var renderView: RenderOverlayView? = null

        fun setRenderView(view: RenderOverlayView) {
            renderView = view
        }
    }

    private val fov by floatValue("fov", 80f, 40f..180f)
    private val mode by stringValue("box_style", "2D", listOf("2D", "3D", "corner"))
    private val strokeWidth by floatValue("stroke_width", 2f, 1f..10f)
    private val rainbowValue by boolValue("Rainbow", true)
    private val colorRed by intValue("color_red", 255, 0..255)
    private val colorGreen by intValue("color_green", 0, 0..255)
    private val colorBlue by intValue("color_blue", 0, 0..255)
    private val showAllEntities by boolValue("show_all_entities", false)
    private val showDistance by boolValue("show_distance", true)
    private val showNames by boolValue("show_names", true)

    // --- 性能优化：将 Paint 和 Rect/RectF 对象提升为成员变量 ---
    // 避免在 onDraw 中重复创建对象，显著减少GC压力，提高帧率
    private val boxPaint = Paint().apply { style = Paint.Style.STROKE }
    private val bgPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val textPaint = Paint().apply {
        textSize = 30f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }
    private val textBounds = android.graphics.Rect()
    private val bgRect = android.graphics.RectF()

    // --- 性能优化：缓存矩阵 ---
    private var lastScreenWidth = 0
    private var lastScreenHeight = 0
    private var lastFov = 0f
    private val projectionMatrix = Matrix4f.create() // 创建一次，后续只更新

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
    }

    override fun onEnabled() {
        super.onEnabled()
        renderView?.invalidate()
    }

    override fun onDisabled() {
        super.onDisabled()
        renderView?.invalidate()
    }

    override fun getStatusInfo(): String {
        return when (mode) {
            "2d" -> "2D"
            "3d" -> "3D"
            "corner" -> "Corner"
            else -> mode
        }
    }

    private fun rotateX(angle: Float): Matrix4f {
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

    private fun rotateY(angle: Float): Matrix4f {
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

    private fun getEntityBoxVertices(entity: Entity): Array<Vector3f> {
        val width = 0.6f
        val height = 1.8f
        val pos = entity.vec3Position
        val halfWidth = width / 2f
        val yPos = if (entity is Player) pos.y - 1.62f else pos.y

        return arrayOf(
            Vector3f.from(pos.x - halfWidth, yPos, pos.z - halfWidth),
            Vector3f.from(pos.x - halfWidth, yPos + height, pos.z - halfWidth),
            Vector3f.from(pos.x + halfWidth, yPos + height, pos.z - halfWidth),
            Vector3f.from(pos.x + halfWidth, yPos, pos.z - halfWidth),
            Vector3f.from(pos.x - halfWidth, yPos, pos.z + halfWidth),
            Vector3f.from(pos.x - halfWidth, yPos + height, pos.z + halfWidth),
            Vector3f.from(pos.x + halfWidth, yPos + height, pos.z + halfWidth),
            Vector3f.from(pos.x + halfWidth, yPos, pos.z + halfWidth)
        )
    }

    private fun worldToScreen(pos: Vector3f, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2f? {
        val w = viewProjMatrix.get(3, 0) * pos.x +
                viewProjMatrix.get(3, 1) * pos.y +
                viewProjMatrix.get(3, 2) * pos.z +
                viewProjMatrix.get(3, 3)

        if (w < 0.01f) return null

        val inverseW = 1f / w
        val screenX = screenWidth / 2f + (0.5f * ((viewProjMatrix.get(0, 0) * pos.x +
                viewProjMatrix.get(0, 1) * pos.y +
                viewProjMatrix.get(0, 2) * pos.z +
                viewProjMatrix.get(0, 3)) * inverseW) * screenWidth + 0.5f)
        val screenY = screenHeight / 2f - (0.5f * ((viewProjMatrix.get(1, 0) * pos.x +
                viewProjMatrix.get(1, 1) * pos.y +
                viewProjMatrix.get(1, 2) * pos.z +
                viewProjMatrix.get(1, 3)) * inverseW) * screenHeight + 0.5f)

        return Vector2f.from(screenX, screenY)
    }

    private fun shouldRenderEntity(entity: Entity): Boolean {
        if (entity == session.localPlayer) return false
        if (!showAllEntities && entity !is Player) return false
        return true
    }

    fun render(canvas: Canvas) {
        if (!isEnabled || !isSessionCreated) return

        val player = session.localPlayer
        val entities = if (showAllEntities) {
            session.level.entityMap.values
        } else {
            session.level.entityMap.values.filterIsInstance<Player>()
        }

        if (entities.isEmpty()) return

        val screenWidth = canvas.width
        val screenHeight = canvas.height

        // --- 性能优化：仅在屏幕尺寸或FOV变化时更新投影矩阵 ---
        if (screenWidth != lastScreenWidth || screenHeight != lastScreenHeight || fov != lastFov) {
            Matrix4f.createPerspective(fov, screenWidth.toFloat() / screenHeight, 0.1f, 128f, projectionMatrix)
            lastScreenWidth = screenWidth
            lastScreenHeight = screenHeight
            lastFov = fov
        }

        val viewMatrix = Matrix4f.createTranslation(player.vec3Position)
            .mul(rotateY(-player.rotationYaw - 180))
            .mul(rotateX(-player.rotationPitch))
            .invert()

        val viewProjMatrix = projectionMatrix.mul(viewMatrix) // 注意矩阵乘法顺序 Proj * View

        // --- 性能优化：更新预先创建的 Paint 对象属性，而不是创建新对象 ---
        boxPaint.strokeWidth = this.strokeWidth
        val colors = ColorUtils.getChromaRainbow(100.0, 10.0)
        val currentColor = if (rainbowValue) Color.rgb(colors.r, colors.g, colors.b) else Color.rgb(colorRed, colorGreen, colorBlue)
        boxPaint.color = currentColor
        textPaint.color = currentColor // 同步文字颜色

        entities.forEach { entity ->
            if (shouldRenderEntity(entity)) {
                drawEntityBox(entity, viewProjMatrix, screenWidth, screenHeight, canvas)
            }
        }
    }

    private fun drawEntityBox(entity: Entity, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas) {
        val boxVertices = getEntityBoxVertices(entity)
        var minX = screenWidth.toDouble()
        var minY = screenHeight.toDouble()
        var maxX = 0.0
        var maxY = 0.0
        val screenPositions = mutableListOf<Vector2f>()

        boxVertices.forEach { vertex ->
            val screenPos = worldToScreen(vertex, viewProjMatrix, screenWidth, screenHeight)
                ?: return@forEach
            screenPositions.add(screenPos)
            minX = minX.coerceAtMost(screenPos.x.toDouble())
            minY = minY.coerceAtMost(screenPos.y.toDouble())
            maxX = maxX.coerceAtLeast(screenPos.x.toDouble())
            maxY = maxY.coerceAtLeast(screenPos.y.toDouble())
        }

        if (!(minX >= screenWidth || minY >= screenHeight || maxX <= 0 || maxY <= 0)) {
            when (mode) {
                "2D" -> draw2DBox(canvas, boxPaint, minX, minY, maxX, maxY)
                "3D" -> draw3DBox(canvas, boxPaint, screenPositions)
                "corner" -> drawCornerBox(canvas, boxPaint, minX, minY, maxX, maxY)
            }

            if (showNames || showDistance) {
                drawEntityInfo(canvas, entity, minX, minY, maxX)
            }
        }
    }

    private fun draw2DBox(canvas: Canvas, paint: Paint, minX: Double, minY: Double, maxX: Double, maxY: Double) {
        val padding = paint.strokeWidth / 2
        canvas.drawRect(
            minX.toFloat() + padding,
            minY.toFloat() + padding,
            maxX.toFloat() - padding,
            maxY.toFloat() - padding,
            paint
        )
    }

    private fun draw3DBox(canvas: Canvas, paint: Paint, screenPositions: List<Vector2f>) {
        if (screenPositions.size < 8) return

        val edges = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 0,
            4 to 5, 5 to 6, 6 to 7, 7 to 4,
            0 to 4, 1 to 5, 2 to 6, 3 to 7
        )

        edges.forEach { (start, end) ->
            val startPos = screenPositions[start]
            val endPos = screenPositions[end]
            if (isOnScreen(startPos, canvas) && isOnScreen(endPos, canvas)) {
                val padding = paint.strokeWidth / 2
                canvas.drawLine(
                    startPos.x.coerceIn(padding, canvas.width - padding),
                    startPos.y.coerceIn(padding, canvas.height - padding),
                    endPos.x.coerceIn(padding, canvas.width - padding),
                    endPos.y.coerceIn(padding, canvas.height - padding),
                    paint
                )
            }
        }
    }

    private fun isOnScreen(pos: Vector2f, canvas: Canvas): Boolean {
        return pos.x >= 0 && pos.x <= canvas.width &&
                pos.y >= 0 && pos.y <= canvas.height
    }

    private fun drawCornerBox(canvas: Canvas, paint: Paint, minX: Double, minY: Double, maxX: Double, maxY: Double) {
        val cornerLength = (maxX - minX).toFloat() / 4
        val corners = listOf(
            minX to minY,
            maxX to minY,
            maxX to maxY,
            minX to maxY
        )

        corners.forEachIndexed { i, (x, y) ->
            // Horizontal lines from corner
            canvas.drawLine(
                x.toFloat(),
                y.toFloat(),
                x.toFloat() + if (i % 2 == 0) cornerLength else -cornerLength,
                y.toFloat(),
                paint
            )
            // Vertical lines from corner
            canvas.drawLine(
                x.toFloat(),
                y.toFloat(),
                x.toFloat(),
                y.toFloat() + if (i < 2) cornerLength else -cornerLength,
                paint
            )
        }
    }

    @SuppressLint("DefaultLocale")
    private fun drawEntityInfo(canvas: Canvas, entity: Entity, minX: Double, minY: Double, maxX: Double) {
        val info = buildString {
            if (showNames && entity is Player) {
                append(entity.username)
            }
            if (showDistance) {
                if (isNotEmpty()) append(" | ")
                val distance = entity.vec3Position.distance(session.localPlayer.vec3Position)
                append("${String.format("%.1f", distance)}m")
            }
        }

        val textX = (minX + maxX).toFloat() / 2
        val textY = minY.toFloat() - 10

        // 使用复用对象
        textPaint.getTextBounds(info, 0, info.length, textBounds)

        val padding = 8f
        // 使用复用对象
        bgRect.set(
            textX - textBounds.width() / 2 - padding,
            textY - textBounds.height() - padding,
            textX + textBounds.width() / 2 + padding,
            textY + padding
        )
        canvas.drawRoundRect(bgRect, 4f, 4f, bgPaint)
        canvas.drawText(info, textX, textY, outlinePaint)
        canvas.drawText(info, textX, textY, textPaint)
    }
}