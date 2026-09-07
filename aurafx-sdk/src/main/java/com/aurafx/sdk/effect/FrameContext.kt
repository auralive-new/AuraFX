package com.aurafx.sdk.effect

import com.aurafx.sdk.api.LensFacing

/**
 * Per-frame GPU state. Effects in later steps may read the input texture and write
 * intermediate targets. Step 1 ships no effect that mutates the frame.
 */
data class FrameContext(
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val inputTextureId: Int,
    val inputIsOes: Boolean,
    val texMatrix: FloatArray,
    val lensFacing: LensFacing,
)
