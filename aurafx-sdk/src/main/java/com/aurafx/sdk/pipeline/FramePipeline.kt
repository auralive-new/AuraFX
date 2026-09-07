package com.aurafx.sdk.pipeline

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Single-slot, latest-only admission control for the camera/GL path.
 *
 * If the renderer is still presenting the previous frame, a newly arrived camera frame is
 * dropped (counted) rather than queued. This is the KEEP_ONLY_LATEST analogue for the
 * GPU SurfaceTexture path, which has no CameraX analyzer queue.
 */
class FramePipeline {
    private val busy = AtomicBoolean(false)
    private val dropCount = AtomicLong(0)
    private val admitCount = AtomicLong(0)

    fun tryBegin(): Boolean {
        val started = busy.compareAndSet(false, true)
        if (started) {
            admitCount.incrementAndGet()
        } else {
            dropCount.incrementAndGet()
        }
        return started
    }

    fun end() {
        busy.set(false)
    }

    fun isBusy(): Boolean = busy.get()

    fun droppedCount(): Long = dropCount.get()

    fun admittedCount(): Long = admitCount.get()

    fun reset() {
        busy.set(false)
        dropCount.set(0)
        admitCount.set(0)
    }
}
