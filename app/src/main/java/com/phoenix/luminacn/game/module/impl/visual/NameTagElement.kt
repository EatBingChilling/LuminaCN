package com.phoenix.luminacn.game.module.impl.visual

import android.graphics.Canvas
import android.graphics.Point
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
    private val originalSizeValue by boolValue("OriginalSize", true)
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
            val realSize = Point()
            val screenWidth = if(originalSizeValue) realSize.x else canvas.width
            val screenHeight = if(originalSizeValue) realSize.y else canvas.height

            val viewProjMatrix =  Matrix4f.createPerspective(fovValue.toFloat()+10, screenWidth.toFloat() / screenHeight, 0.1f, 128f)
                .mul(Matrix4f.createTranslation(player.vec3Position)
                    .mul(rotY(-player.rotationYaw-180))
                    .mul(rotX(-player.rotationPitch))
                    .invert())

            val entities = if (showAllEntities) {
                event.session.level.entityMap.values
            } else {
                event.session.level.entityMap.values.filterIsInstance<Player>()
            }

            entities.forEach { entity ->
                if (entity == player) return@forEach

                if (entity is Player) {
                    drawEntityBox(entity, viewProjMatrix, screenWidth, screenHeight, canvas)
                }
            }
        }
    }

    private fun drawEntityBox(entity: Player, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas) {
        if(displayList[entity]==null){
            displayList[entity]= EntityNameTag()
        }
        displayList[entity]!!.draw(entity,viewProjMatrix,screenWidth,screenHeight,canvas,this)
    }

    fun worldToScreen(posX: Double, posY: Double, posZ: Double, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2d? {
        val w = viewProjMatrix.get(3, 0) * posX +
                viewProjMatrix.get(3, 1) * posY +
                viewProjMatrix.get(3, 2) * posZ +
                viewProjMatrix.get(3, 3)
        if (w < 0.01f) return null
        val inverseW = 1 / w

        val screenX = screenWidth / 2f + (0.5f * ((viewProjMatrix.get(0, 0) * posX + viewProjMatrix.get(0, 1) * posY +
                viewProjMatrix.get(0, 2) * posZ + viewProjMatrix.get(0, 3)) * inverseW) * screenWidth + 0.5f)
        val screenY = screenHeight / 2f - (0.5f * ((viewProjMatrix.get(1, 0) * posX + viewProjMatrix.get(1, 1) * posY +
                viewProjMatrix.get(1, 2) * posZ + viewProjMatrix.get(1, 3)) * inverseW) * screenHeight + 0.5f)
        return Vector2d.from(screenX, screenY)
    }

    private fun rotX(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(1f, 0f, 0f, 0f,
            0f, c, -s, 0f,
            0f, s, c, 0f,
            0f, 0f, 0f, 1f)
    }

    private fun rotY(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(c, 0f, s, 0f,
            0f, 1f, 0f, 0f,
            -s, 0f, c, 0f,
            0f, 0f, 0f, 1f)
    }
}