package com.aurafx.sdk.api

/**
 * Point-in-time measurements. Fields that cannot be observed on this device/session are
 * null — they are never estimated.
 */
data class PerformanceSnapshot(
    val fps: Float,
    val frameProcessTimeMs: Float,
    val gpuTimeMs: Float?,
    val droppedFrames: Long,
    val presentedFrames: Long,
    val cameraStartupMs: Float?,
    val javaHeapUsedBytes: Long,
    val nativeHeapAllocatedBytes: Long?,
    val lastEffectLoadMs: Float?,
    val lastCameraFrameTimestampNs: Long?,
    val lastIngress: FrameIngress = FrameIngress.NONE,
    val pipelineAdmitted: Long = 0,
    val videoEncodeTimeMs: Float? = null,
    val lastExportMs: Float? = null,
    val lastPhotoCaptureMs: Float? = null,
    val encoderFrames: Long = 0,
    val encoderDroppedFrames: Long = 0,
)
