package com.project.lumina.client.ui.opengl

import android.opengl.GLES20
import android.opengl.Matrix
import org.cloudburstmc.math.vector.Vector3f
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.properties.Delegates

object OpenGLESPRenderer {

    fun renderESPBoxes(
        playerPos: Vector3f,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        entities: List<Vector3f>
    ) {
        for (entity in entities) {
            drawBoxAroundEntity(entity, playerPos, viewMatrix, projectionMatrix)
        }
    }

    private fun drawBoxAroundEntity(
        entity: Vector3f,
        player: Vector3f,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ) {
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, entity.x - player.x, entity.y - player.y, entity.z - player.z)

        val mvpMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        val vertices = floatArrayOf(

            -0.3f, 0f, -0.3f,
            0.3f, 0f, -0.3f,
            0.3f, 0f,  0.3f,
            -0.3f, 0f,  0.3f,

            -0.3f, 1.8f, -0.3f,
            0.3f, 1.8f, -0.3f,
            0.3f, 1.8f,  0.3f,
            -0.3f, 1.8f,  0.3f
        )

        val indices = shortArrayOf(
            0, 1, 1, 2, 2, 3, 3, 0,
            4, 5, 5, 6, 6, 7, 7, 4,
            0, 4, 1, 5, 2, 6, 3, 7
        )

        GLES20.glUseProgram(SimpleShader.program)

        GLES20.glUniformMatrix4fv(SimpleShader.uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glEnableVertexAttribArray(SimpleShader.aPositionHandle)
        GLES20.glVertexAttribPointer(SimpleShader.aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, SimpleShader.createFloatBuffer(vertices))
        GLES20.glDrawElements(GLES20.GL_LINES, indices.size, GLES20.GL_UNSIGNED_SHORT, SimpleShader.createShortBuffer(indices))
        GLES20.glDisableVertexAttribArray(SimpleShader.aPositionHandle)
    }
}



object SimpleShader {
    var program by Delegates.notNull<Int>()
    var aPositionHandle = 0
    var uMVPMatrixHandle = 0

    fun init() {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
            }
        """.trimIndent())

        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, """
            precision mediump float;
            void main() {
                gl_FragColor = vec4(0.0, 1.0, 1.0, 1.0); 
            }
        """.trimIndent())

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)

            aPositionHandle = GLES20.glGetAttribLocation(it, "aPosition")
            uMVPMatrixHandle = GLES20.glGetUniformLocation(it, "uMVPMatrix")
        }
    }

    fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun createFloatBuffer(data: FloatArray): FloatBuffer = java.nio.ByteBuffer
        .allocateDirect(data.size * 4)
        .order(java.nio.ByteOrder.nativeOrder())
        .asFloatBuffer().apply {
            put(data)
            position(0)
        }

    fun createShortBuffer(data: ShortArray): ShortBuffer = java.nio.ByteBuffer
        .allocateDirect(data.size * 2)
        .order(java.nio.ByteOrder.nativeOrder())
        .asShortBuffer().apply {
            put(data)
            position(0)
        }
}
