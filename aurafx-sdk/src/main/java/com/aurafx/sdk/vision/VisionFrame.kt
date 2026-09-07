package com.aurafx.sdk.vision

/**
 * GPU (or future CPU) frame view presented to vision implementations.
 * Step 1 does not run models; this type exists so later steps share one contract.
 */
data class VisionFrame(
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val oesTextureId: Int,
    val texMatrix: FloatArray,
)
