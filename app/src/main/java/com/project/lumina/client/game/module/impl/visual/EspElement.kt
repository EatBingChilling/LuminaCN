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
    // --- Settings (FIXED) ---
    // FIX: Removed description string from value definitions as the function doesn't support it.
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
    // FIX: Removed visibility lambda from boolValue as the function doesn't support it.
    private val tracerBottom by boolValue("底部", true)
    private val tracerTop by boolValue("顶部", false)

    /**
     * FIX: Replaced onTick with beforePacketBound as it's the available game loop hook.
     * This is called frequently when packets are sent, acting like a tick.
     */
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        // We check isEnabled here for performance, and a null check for localPlayer is a good proxy for being in-game.
        if (isEnabled && session.localPlayer != null) {
            ESPOverlayView.instance?.postInvalidate()
        }
    }

    fun onRender2D(canvas: Canvas) {
        // FIX: Replaced session.isInGame with a null check on localPlayer, which is more reliable.
        if (!isEnabled || session.localPlayer == null) return

        val player = session.localPlayer
        val entities = session.level.entityMap.values.filter { shouldRenderEntity(it) }

        if (entities.isEmpty()) return

        val screenWidth = canvas.width
        val screenHeight = canvas.height

        val viewMatrix = rotateX(-player.vec3Rotation.x)
            .mul(rotateY(-player.vec3Rotation.y - 180f))
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
            drawEntityESP(entity, viewProjMatrix, screenWidth, screenHeight, canvas, paint)
        }
    }
    
    private fun drawEntityESP(entity: Entity, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas, paint: Paint) {
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
            drawEntityInfo(canvas, paint, entity, minX, minY, maxX)
        }
    }

    private fun draw2DBox(canvas: Canvas, paint: Paint, minX: Float, minY: Float, maxX: Float, maxY: Float) { canvas.drawRect(minX, minY, maxX, maxY, paint) }
    private fun draw3DBox(canvas: Canvas, paint: Paint, screenPositions: List<Vector2f>) { /* ... */ }
    private fun drawCornerBox(canvas: Canvas, paint: Paint, minX: Float, minY: Float, maxX: Float, maxY: Float) { /* ... */ }
    private fun drawTracer(canvas: Canvas, paint: Paint, screenWidth: Int, screenHeight: Int, entityMidX: Float, entityBottomY: Float) { /* ... */ }
    private fun drawEntityInfo(canvas: Canvas, paint: Paint, entity: Entity, minX: Float, minY: Float, maxX: Float) { /* ... */ }
    private fun rotateX(angle: Float): Matrix4f { /* ... */ }
    private fun rotateY(angle: Float): Matrix4f { /* ... */ }

    /**
     * FIX: Re-implemented this function to use hard-coded dimensions because `entity.boundingBox` does not exist.
     * This is a safe fallback for drawing boxes around player-like entities.
     */
    private fun getEntityBoxVertices(entity: Entity): Array<Vector3f> {
        val pos = entity.vec3Position
        val height = 1.8f // Standard player height
        val width = 0.6f
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

    private fun worldToScreen(pos: Vector3f, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2f? { /* ... */ }
    private fun shouldRenderEntity(entity: Entity): Boolean = when { entity == session.localPlayer -> false; entity is Player -> players; else -> mobs }
}