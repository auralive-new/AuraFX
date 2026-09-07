package com.aurafx.sdk.performance

import com.aurafx.sdk.api.FrameIngress
import com.aurafx.sdk.api.PerformanceSnapshot

fun interface MemoryProbe {
    fun javaHeapUsedBytes(): Long
}

internal object DefaultMemoryProbe : MemoryProbe {
    override fun javaHeapUsedBytes(): Long {
        val rt = Runtime.getRuntime()
        return rt.totalMemory() - rt.freeMemory()
    }
}

/**
 * Records only observed events. FPS is computed from presented-frame timestamps, not a
 * hardcoded target. GPU time is stored only when a GL query returns a value.
 */
class PerformanceManager(
    private val enabled: Boolean = true,
    private val memoryProbe: MemoryProbe = DefaultMemoryProbe,
    private val nativeHeapProbe: () -> Long? = { null },
) {
    private val lock = Any()
    private var presentedFrames = 0L
    private var droppedFrames = 0L
    private var windowStartNs = 0L
    private var windowFrames = 0
    private var fps = 0f
    private var emaProcessNs = 0f
    private var lastGpuNs: Long? = null
    private var cameraStartNs = 0L
    private var cameraReadyNs = 0L
    private var lastEffectLoadNs: Long? = null
    private var lastTimestampNs: Long? = null
    private var lastIngress: FrameIngress = FrameIngress.NONE
    private var pipelineAdmitted: Long = 0

    fun markCameraStart() {
        if (!enabled) return
        synchronized(lock) {
            cameraStartNs = System.nanoTime()
            cameraReadyNs = 0L
        }
    }

    fun markCameraReady() {
        if (!enabled) return
        synchronized(lock) {
            if (cameraReadyNs == 0L) {
                cameraReadyNs = System.nanoTime()
            }
        }
    }

    fun onFramePresented(
        timestampNs: Long,
        processNs: Long,
        gpuNs: Long?,
        ingress: FrameIngress = FrameIngress.CAMERA_OES,
        admitted: Long = 0,
    ) {
        if (!enabled) return
        val now = System.nanoTime()
        synchronized(lock) {
            presentedFrames += 1
            lastIngress = ingress
            pipelineAdmitted = admitted
            lastTimestampNs = timestampNs
            if (gpuNs != null) lastGpuNs = gpuNs
            if (emaProcessNs == 0f) {
                emaProcessNs = processNs.toFloat()
            } else {
                emaProcessNs = emaProcessNs * 0.9f + processNs.toFloat() * 0.1f
            }
            if (windowStartNs == 0L) {
                windowStartNs = now
            }
            windowFrames += 1
            val elapsed = now - windowStartNs
            if (elapsed >= 1_000_000_000L) {
                fps = windowFrames * 1_000_000_000f / elapsed.toFloat()
                windowStartNs = now
                windowFrames = 0
            }
        }
    }

    fun onDropped() {
        if (!enabled) return
        synchronized(lock) { droppedFrames += 1 }
    }

    fun markEffectLoadNs(durationNs: Long) {
        if (!enabled) return
        synchronized(lock) { lastEffectLoadNs = durationNs }
    }

    fun snapshot(): PerformanceSnapshot {
        synchronized(lock) {
            val startup = if (cameraStartNs != 0L && cameraReadyNs != 0L) {
                (cameraReadyNs - cameraStartNs) / 1_000_000f
            } else {
                null
            }
            return PerformanceSnapshot(
                fps = fps,
                frameProcessTimeMs = emaProcessNs / 1_000_000f,
                gpuTimeMs = lastGpuNs?.let { it / 1_000_000f },
                droppedFrames = droppedFrames,
                presentedFrames = presentedFrames,
                cameraStartupMs = startup,
                javaHeapUsedBytes = memoryProbe.javaHeapUsedBytes(),
                nativeHeapAllocatedBytes = nativeHeapProbe(),
                lastEffectLoadMs = lastEffectLoadNs?.let { it / 1_000_000f },
                lastCameraFrameTimestampNs = lastTimestampNs,
                lastIngress = lastIngress,
                pipelineAdmitted = pipelineAdmitted,
            )
        }
    }
}
