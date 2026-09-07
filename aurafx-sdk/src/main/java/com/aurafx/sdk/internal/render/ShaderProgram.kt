package com.aurafx.sdk.internal.render

import android.opengl.GLES30

internal class ShaderProgram(vertex: String, fragment: String) {
    val id: Int = link(vertex, fragment)

    fun use() = GLES30.glUseProgram(id)

    fun loc(name: String): Int = GLES30.glGetUniformLocation(id, name)

    fun release() {
        if (id != 0) GLES30.glDeleteProgram(id)
    }

    companion object {
        fun compile(type: Int, source: String): Int {
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

        fun link(vertex: String, fragment: String): Int {
            val vs = compile(GLES30.GL_VERTEX_SHADER, vertex)
            val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragment)
            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, vs)
            GLES30.glAttachShader(program, fs)
            GLES30.glBindAttribLocation(program, 0, "aPos")
            GLES30.glBindAttribLocation(program, 1, "aUv")
            GLES30.glBindAttribLocation(program, 2, "aDisp")
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
    }
}

internal class GlFramebuffer {
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    var tex: Int = 0
        private set
    private var fbo: Int = 0

    fun ensure(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        if (tex != 0 && w == width && h == height) return
        release()
        width = w
        height = h
        val t = IntArray(1)
        val f = IntArray(1)
        GLES30.glGenTextures(1, t, 0)
        GLES30.glGenFramebuffers(1, f, 0)
        tex = t[0]
        fbo = f[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, w, h, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null,
        )
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, tex, 0,
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw EglException("FBO incomplete 0x${Integer.toHexString(status)}")
        }
    }

    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glViewport(0, 0, width, height)
    }

    fun release() {
        if (fbo != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
        if (tex != 0) GLES30.glDeleteTextures(1, intArrayOf(tex), 0)
        fbo = 0
        tex = 0
        width = 0
        height = 0
    }
}
