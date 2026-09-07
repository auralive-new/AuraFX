package com.aurafx.sdk.vision

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VisionProcessorTest {
    @Test
    fun processWithoutImplementationsIsUnavailableNotZeroDetections() {
        val processor = VisionProcessor()
        val frame = VisionFrame(
            timestampNs = 42L,
            width = 1280,
            height = 720,
            oesTextureId = 1,
            texMatrix = FloatArray(16),
        )
        val data = processor.process(frame)
        assertThat(processor.hasAnyImplementation()).isFalse()
        assertThat(data.status).isEqualTo(VisionStatus.UNAVAILABLE)
        assertThat(data.faces).isEmpty()
        assertThat(data.meshes).isEmpty()
        assertThat(data.iris).isEmpty()
        assertThat(data.pose).isEmpty()
        assertThat(data.segmentation).isNull()
        assertThat(data.frameTimestampNs).isEqualTo(42L)
    }

    @Test
    fun processUsesOnlyRegisteredImplementation() {
        val processor = VisionProcessor()
        processor.install(
            faceDetector = FaceDetector { listOf(DetectedFace(trackingId = 7, boundsNormalized = floatArrayOf(0f, 0f, 1f, 1f))) },
        )
        val data = processor.process(
            VisionFrame(1L, 10, 10, 1, FloatArray(16)),
        )
        assertThat(data.status).isEqualTo(VisionStatus.READY)
        assertThat(data.faces).hasSize(1)
        assertThat(data.faces[0].trackingId).isEqualTo(7)
        assertThat(data.meshes).isEmpty()
    }
}
