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

    @Test
    fun hydratesLandmarksFromPublishedMesh() {
        val processor = VisionProcessor()
        processor.publish(
            TrackingData(
                status = VisionStatus.READY,
                frameTimestampNs = 1L,
                meshes = listOf(
                    FaceMesh(trackingId = 3, vertices = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f), indices = null),
                ),
            ),
        )
        val data = processor.process(VisionFrame(9L, 10, 10, 1, FloatArray(16)))
        assertThat(data.landmarks).isNotNull()
        assertThat(data.landmarks!!.count).isEqualTo(4)
        assertThat(data.landmarks!!.trackingId).isEqualTo(3)
        assertThat(data.frameTimestampNs).isEqualTo(9L)
    }
}
