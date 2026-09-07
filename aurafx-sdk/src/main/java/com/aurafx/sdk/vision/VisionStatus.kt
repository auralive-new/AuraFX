package com.aurafx.sdk.vision

/**
 * Whether a vision capability has a real implementation attached.
 *
 * [UNAVAILABLE] means no detector is registered — it is **not** "zero detections".
 */
enum class VisionStatus {
    UNAVAILABLE,
    READY,
}
