package com.aurafx.sdk.vision.mediapipe

/**
 * MediaPipe VIDEO mode requires strictly increasing millisecond timestamps.
 * CameraX can repeat or rewind ImageProxy timestamps; that throws and drops
 * the whole face / selfie / pose result for the frame.
 */
internal class MediaPipeVideoClock {
    private var lastMs = -1L

    fun nextMs(timestampNs: Long): Long {
        val raw = (timestampNs / 1_000_000L).coerceAtLeast(0L)
        val ms = if (lastMs < 0L) raw else maxOf(raw, lastMs + 1L)
        lastMs = ms
        return ms
    }
}
