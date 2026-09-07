package com.aurafx.sdk.effect

import com.aurafx.sdk.api.LensFacing

/**
 * Per-frame GPU state. An effect may write [processedTextureId] for the presenter.
 */
class FrameContext(
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val inputTextureId: Int,
    val inputIsOes: Boolean,
    val texMatrix: FloatArray,
    val lensFacing: LensFacing,
) {
    var processedTextureId: Int = 0
    var processedIsOes: Boolean = false

    fun outputTextureId(): Int = if (processedTextureId != 0) processedTextureId else inputTextureId

    fun outputIsOes(): Boolean = if (processedTextureId != 0) processedIsOes else inputIsOes
}
