package com.project.lumina.client.game.module.impl.visual

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.entity.Entity
import com.project.lumina.client.game.entity.Player
import com.project.lumina.client.ui.ESPOverlayView
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.math.matrix.Matrix4f
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.math.vector.Vector3f
import kotlin.math.*

class EspElement(iconResId: Int = AssetManager.getAsset("ic_eye_black_24dp")) : Element(
    name = "ESP",
    category = CheatCategory.Visual,
    iconResId,
    displayNameResId = AssetManager.getString("module_esp_display_name")
) {
    // --- Settings ---
    private val fov by floatValue("FOV", 90f, 40f..120f)
    private val strokeWidth by floatValue("线条宽度", 2f, 1f..10f)
    private val colorRed by intValue("红 (R)", 255, 0..255)
    private val colorGreen by intValue("绿 (G)", 0, 0..255)
    private val colorBlue by intValue("蓝 (B)", 0, 0..255)
    private val players by boolValue("玩家", true)
    private val mobs by boolValue("生物", false)
    private val showDistance by boolValue("距离", true)
    private val showNames by boolValue("名称", true)
    private val use2DBox by boolValue("2D方框", true)
    private val use3DBox by boolValue("3D方框", false)
    private val useCornerBox by boolValue("角框", false)
    private val tracers by boolValue("射线", false)
    private val tracerBottom by boolValue("底部", true)
    private val tracerTop by boolValue("顶部", false)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (isEnabled && session.localPlayer != null) {
            ESPOverlayView.instance?.postInvalidate()
        }
    }

    fun onRender2D(canvas: Canvas) {
        // [!!] 崩溃修复点:
        // 使用 `?: return` 安全地获取 localPlayer。
        // 如果 session.localPlayer 为 null，函数会在此处直接返回，避免后续的空指针异常。
        val player = session.localPlayer ?: return
        if (!isEnabled) return

        val entities = session.level.entityMap.values.filter { shouldRenderEntity(it) }
        if (entities.isEmpty()) return

        val screenWidth = canvas.width
        val screenHeight = canvas.height

        // [!!] 视角修复点: 移除了 Y 轴旋转中多余的 "- 180f"。
        val viewMatrix = rotateX(-player.vec3Rotation.x)
            .mul(rotateY(-player.vec3Rotation.y))
            .mul(Matrix4f.createTranslation(player.vec3Position.add(0f, 1.62f, 0f).mul(-1f)))

        val projectionMatrix = Matrix4f.createPerspective(fov, screenWidth.toFloat() / screenHeight, 0.1f, 100f)
        val viewProjMatrix = projectionMatrix.mul(viewMatrix)

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = this@EspElement.strokeWidth
            color = Color.rgb(colorRed, colorGreen, colorBlue)
            isAntiAlias = true
        }

        entities.forEach { entity ->
            // 现在可以安全地将 non-null 的 player 传递给需要它的函数
            drawEntityESP(entity, player, viewProjMatrix, screenWidth, screenHeight, canvas, paint)
        }
    }

    private fun drawEntityESP(entity: Entity, localPlayer: Player, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas, paint: Paint) {
        val boxVertices = getEntityBoxVertices(entity)
        val screenPositions = boxVertices.mapNotNull { worldToScreen(it, viewProjMatrix, screenWidth, screenHeight) }

        if (screenPositions.isEmpty()) return

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        screenPositions.forEach {
            minX = min(minX, it.x)
            minY = min(minY, it.y)
            maxX = max(maxX, it.x)
            maxY = max(maxY, it.y)
        }

        if (maxX < 0 || minX > screenWidth || maxY < 0 || minY > screenHeight) return

        when {
            use2DBox -> draw2DBox(canvas, paint, minX, minY, maxX, maxY)
            use3DBox -> if (screenPositions.size >= 8) draw3DBox(canvas, paint, screenPositions)
            useCornerBox -> drawCornerBox(canvas, paint, minX, minY, maxX, maxY)
        }

        if (tracers) {
            drawTracer(canvas, paint, screenWidth, screenHeight, (minX + maxX) / 2, maxY)
        }

        if (showNames || showDistance) {
            drawEntityInfo(canvas, paint, entity, localPlayer, minX, minY, maxX)
        }
    }

    private fun draw2DBox(canvas: Canvas, paint: Paint, minX: Float, minY: Float, maxX: Float, maxY: Float) {
        canvas.drawRect(minX, minY, maxX, maxY, paint)
    }

    private fun draw3DBox(canvas: Canvas, paint: Paint, screenPositions: List<Vector2f>) {
        val edges = intArrayOf(0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7)
        for (i in edges.indices step 2) {
            val p1 = screenPositions[edges[i]]
            val p2 = screenPositions[edges[i + 1]]
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
        }
    }

    private fun drawCornerBox(canvas: Canvas, paint: Paint, minX: Float, minY: Float, maxX: Float, maxY: Float) {
        val width = maxX - minX
        val height = maxY - minY
        val lineLength = min(width, height) / 4f

        canvas.drawLine(minX, minY, minX + lineLength, minY, paint)
        canvas.drawLine(minX, minY, minX, minY + lineLength, paint)
        canvas.drawLine(maxX, minY, maxX - lineLength, minY, paint)
        canvas.drawLine(maxX, minY, maxX, minY + lineLength, paint)
        canvas.drawLine(minX, maxY, minX + lineLength, maxY, paint)
        canvas.drawLine(minX, maxY, minX, maxY - lineLength, paint)
        canvas.drawLine(maxX, maxY, maxX - lineLength, maxY, paint)
        canvas.drawLine(maxX, maxY, maxX, maxY - lineLength, paint)
    }

    private fun drawTracer(canvas: Canvas, paint: Paint, screenWidth: Int, screenHeight: Int, entityMidX: Float, entityBottomY: Float) {
        val startX = screenWidth / 2f
        val startY = when {
            tracerBottom -> screenHeight.toFloat()
            tracerTop -> 0f
            else -> screenHeight.toFloat()
        }
        canvas.drawLine(startX, startY, entityMidX, entityBottomY, paint)
    }

    private fun drawEntityInfo(canvas: Canvas, paint: Paint, entity: Entity, localPlayer: Player, minX: Float, minY: Float, maxX: Float) {
        val textPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        val info = buildString {
            if (showNames && entity is Player) append(entity.username)
            if (showDistance) {
                if (isNotEmpty()) append(" ")
                append("[${String.format("%.1f", entity.distance(localPlayer))}m]")
            }
        }
        if (info.isEmpty()) return

        val textX = (minX + maxX) / 2
        val textY = minY - 10
        textPaint.color = Color.BLACK
        canvas.drawText(info, textX + 2, textY + 2, textPaint)
        textPaint.color = paint.color
        canvas.drawText(info, textX, textY, textPaint)
    }

    private fun rotateX(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val c = cos(rad)
        val s = sin(rad)
        return Matrix4f.from(
            1f, 0f, 0f, 0f,
            0f, c, -s, 0f,
            0f, s, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun rotateY(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val c = cos(rad)
        val s = sin(rad)
        return Matrix4f.from(
            c, 0f, s, 0f,
            0f, 1f, 0f, 0f,
            -s, 0f, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun getEntityBoxVertices(entity: Entity): Array<Vector3f> {
        val pos = entity.vec3Position
        val height = 1.8f
        val width = 0.6f
        val halfWidth = width / 2f
        val yPos = pos.y

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
        val clipSpacePos = viewProjMatrix.transform(pos.x, pos.y, pos.z, 1.0f)
        if (clipSpacePos.w < 0.1f) return null
        val ndc = Vector3f.from(clipSpacePos.x / clipSpacePos.w, clipSpacePos.y / clipSpacePos.w, clipSpacePos.z / clipSpacePos.w)
        val screenX = (ndc.x + 1.0f) / 2.0f * screenWidth
        val screenY = (1.0f - ndc.y) / 2.0f * screenHeight
        return Vector2f.from(screenX, screenY)
    }

    private fun shouldRenderEntity(entity: Entity): Boolean {
        if (entity == session.localPlayer) {
            return false
        }
        return when (entity) {
            is Player -> players
            else -> mobs
        }
    }
}