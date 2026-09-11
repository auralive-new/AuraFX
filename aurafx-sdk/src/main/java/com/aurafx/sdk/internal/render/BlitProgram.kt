package com.aurafx.sdk.internal.render

import android.opengl.GLES11Ext
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class GlProgramException(message: String) : RuntimeException(message)

/**
 * Full-screen blit. Supports EXTERNAL_OES (camera) and TEXTURE_2D (processFrame uploads).
 * Vertices live in a VBO; client-side arrays are not used (invalid on many GLES 3 drivers).
 */
internal class BlitProgram {
    private var oesProgram = 0
    private var tex2dProgram = 0
    private var vao = 0
    private var vbo = 0
    private val tmpMatrix = FloatArray(16)

    fun init() {
        oesProgram = link(VERTEX, FRAGMENT_OES)
        tex2dProgram = link(VERTEX, FRAGMENT_2D)
        val verts = floatArrayOf(
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            -1f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f,
        )
        val buffer = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(verts)
        buffer.position(0)

        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        vao = vaos[0]
        vbo = vbos[0]
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        checkGl("blit vao")
    }

    fun drawOes(
        textureId: Int,
        texMatrix: FloatArray,
        mirrorX: Boolean,
        coverScaleX: Float = 1f,
        coverScaleY: Float = 1f,
    ) {
        draw(oesProgram, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId, texMatrix, mirrorX, coverScaleX, coverScaleY)
    }

    fun draw2d(
        textureId: Int,
        mirrorX: Boolean,
        coverScaleX: Float = 1f,
        coverScaleY: Float = 1f,
    ) {
        android.opengl.Matrix.setIdentityM(tmpMatrix, 0)
        draw(tex2dProgram, GLES30.GL_TEXTURE_2D, textureId, tmpMatrix, mirrorX, coverScaleX, coverScaleY)
    }

    private fun draw(
        program: Int,
        target: Int,
        textureId: Int,
        texMatrix: FloatArray,
        mirrorX: Boolean,
        coverScaleX: Float,
        coverScaleY: Float,
    ) {
        GLES30.glUseProgram(program)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(target, textureId)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "uTexture"), 0)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, "uTexMatrix"), 1, false, texMatrix, 0)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, "uMirror"), if (mirrorX) -1f else 1f)
        val coverLoc = GLES30.glGetUniformLocation(program, "uCoverScale")
        if (coverLoc >= 0) {
            GLES30.glUniform2f(coverLoc, coverScaleX.coerceAtLeast(1f), coverScaleY.coerceAtLeast(1f))
        }
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
        GLES30.glBindTexture(target, 0)
        checkGl("blit")
    }

    fun release() {
        if (vao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
        if (vbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
        if (oesProgram != 0) GLES30.glDeleteProgram(oesProgram)
        if (tex2dProgram != 0) GLES30.glDeleteProgram(tex2dProgram)
        vao = 0
        vbo = 0
        oesProgram = 0
        tex2dProgram = 0
    }

    private fun link(vertex: String, fragment: String): Int {
        val vs = compile(GLES30.GL_VERTEX_SHADER, vertex)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragment)
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glBindAttribLocation(program, 0, "aPos")
        GLES30.glBindAttribLocation(program, 1, "aTex")
        GLES30.glLinkProgram(program)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            throw GlProgramException("Program link failed: $log")
        }
        return program
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw GlProgramException("Shader compile failed: $log")
        }
        return shader
    }

    companion object {
        private const val VERTEX = """#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aTex;
uniform mat4 uTexMatrix;
uniform float uMirror;
uniform vec2 uCoverScale;
out vec2 vTex;
void main() {
  gl_Position = vec4(aPos.x * uMirror * uCoverScale.x, aPos.y * uCoverScale.y, 0.0, 1.0);
  vec4 t = uTexMatrix * vec4(aTex, 0.0, 1.0);
  vTex = t.xy;
}
"""
        private const val FRAGMENT_OES = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform samplerExternalOES uTexture;
in vec2 vTex;
out vec4 fragColor;
void main() {
  fragColor = texture(uTexture, vTex);
}
"""
        private const val FRAGMENT_2D = """#version 300 es
precision mediump float;
uniform sampler2D uTexture;
in vec2 vTex;
out vec4 fragColor;
void main() {
  fragColor = texture(uTexture, vTex);
}
"""
    }
}
