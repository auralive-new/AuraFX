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
    private var emaEncodeNs = 0f
    private var lastExportNs: Long? = null
    private var lastPhotoNs: Long? = null
    private var encoderFrames = 0L
    private var encoderDropped = 0L
    @Volatile private var gpuRenderer: String? = null
    @Volatile private var cameraWidth: Int = 0
    @Volatile private var cameraHeight: Int = 0

    fun setGpuRenderer(name: String?) {
        gpuRenderer = name
    }

    fun setCameraResolution(width: Int, height: Int) {
        cameraWidth = width
        cameraHeight = height
    }

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

    fun onEncoderFrame(encodeNs: Long) {
        if (!enabled) return
        synchronized(lock) {
            encoderFrames += 1
            emaEncodeNs = if (emaEncodeNs == 0f) encodeNs.toFloat() else emaEncodeNs * 0.9f + encodeNs.toFloat() * 0.1f
        }
    }

    fun onEncoderDropped() {
        if (!enabled) return
        synchronized(lock) { encoderDropped += 1 }
    }

    fun markExportNs(durationNs: Long) {
        if (!enabled) return
        synchronized(lock) { lastExportNs = durationNs }
    }

    fun markPhotoNs(durationNs: Long) {
        if (!enabled) return
        synchronized(lock) { lastPhotoNs = durationNs }
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
                videoEncodeTimeMs = emaEncodeNs.takeIf { encoderFrames > 0L }?.div(1_000_000f),
                lastExportMs = lastExportNs?.let { it / 1_000_000f },
                lastPhotoCaptureMs = lastPhotoNs?.let { it / 1_000_000f },
                encoderFrames = encoderFrames,
                encoderDroppedFrames = encoderDropped,
                gpuRenderer = gpuRenderer,
                cameraWidth = cameraWidth,
                cameraHeight = cameraHeight,
            )
        }
    }
}
