package com.aurafx.sdk.internal.render

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.view.Surface

internal class EglException(message: String) : RuntimeException(message)

/**
 * EGL 1.4 display + ES 3 context. Not thread-safe; owned by [AuraFxRenderThread].
 */
internal class EglCore {
    val display: EGLDisplay
    val context: EGLContext
    val config: EGLConfig
    private var released = false

    init {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) {
            throw EglException("eglGetDisplay failed")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            throw EglException("eglInitialize failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
        config = chooseConfig(recordable = true)

        val attribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attribs, 0)
        if (context == EGL14.EGL_NO_CONTEXT) {
            throw EglException("eglCreateContext ES3 failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
    }

    private fun chooseConfig(recordable: Boolean): EGLConfig {
        val recordableAttr = if (recordable) intArrayOf(0x3142, 1) else intArrayOf()
        val attribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, 0x0040, // EGL_OPENGL_ES3_BIT_KHR
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
        ) + recordableAttr + intArrayOf(EGL14.EGL_NONE)
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        val ok = EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, num, 0) && num[0] > 0
        if (ok) {
            return configs[0] ?: throw EglException("EGLConfig was null")
        }
        if (recordable) return chooseConfig(recordable = false)
        throw EglException("eglChooseConfig failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
    }

    fun createWindowSurface(surface: Surface): EGLSurface {
        val attribs = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreateWindowSurface(display, config, surface, attribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw EglException("eglCreateWindowSurface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
        return eglSurface
    }

    fun createPbufferSurface(width: Int, height: Int): EGLSurface {
        val attribs = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE,
        )
        val eglSurface = EGL14.eglCreatePbufferSurface(display, config, attribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw EglException("eglCreatePbufferSurface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
        return eglSurface
    }

    fun makeCurrent(surface: EGLSurface) {
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
            throw EglException("eglMakeCurrent failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
    }

    fun makeNothingCurrent() {
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
    }

    fun swapBuffers(surface: EGLSurface): Boolean = EGL14.eglSwapBuffers(display, surface)

    fun destroySurface(surface: EGLSurface) {
        if (surface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(display, surface)
        }
    }

    fun release() {
        if (released) return
        released = true
        makeNothingCurrent()
        EGL14.eglDestroyContext(display, context)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(display)
    }
}

internal fun checkGl(op: String) {
    val err = GLES30.glGetError()
    if (err != GLES30.GL_NO_ERROR) {
        throw EglException("$op: glError 0x${Integer.toHexString(err)}")
    }
}

/** Drain GL errors without aborting the camera frame. */
internal fun checkGlSoft(op: String) {
    var err = GLES30.glGetError()
    while (err != GLES30.GL_NO_ERROR) {
        com.aurafx.sdk.internal.AuraFxLog.w("$op: glError 0x${Integer.toHexString(err)}")
        err = GLES30.glGetError()
    }
}
