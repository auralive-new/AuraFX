package com.aurafx.sdk.api

import java.nio.ByteBuffer

/**
 * Host-supplied CPU frame for [com.aurafx.sdk.AuraFxSession.processFrame].
 *
 * Camera capture does **not** use this type — the live camera stays on the GPU OES path.
 *
 * [rgba8888] must be tightly packed `width * height * 4` bytes, bottom-left origin is not
 * assumed; origin is top-left (Android Bitmap convention). Uploading this buffer is an
 * explicit CPU→GPU copy.
 */
data class AuraFxInputFrame(
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val rgba8888: ByteBuffer,
)
