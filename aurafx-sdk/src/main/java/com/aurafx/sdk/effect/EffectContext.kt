package com.aurafx.sdk.effect

import android.opengl.EGLContext
import com.aurafx.sdk.performance.PerformanceManager

/**
 * GL-thread resources shared with effects. Created once the EGL context is current.
 */
class EffectContext internal constructor(
    val eglContext: EGLContext,
    val performance: PerformanceManager,
)
