package com.aurafx.sdk.vision

data class DetectedFace(
    val trackingId: Int,
    val boundsNormalized: FloatArray,
)

data class FaceMesh(
    val trackingId: Int,
    val vertices: FloatArray,
    val indices: IntArray?,
)

data class FaceTrack(
    val trackingId: Int,
    val stable: Boolean,
)

data class IrisTrack(
    val trackingId: Int,
    val leftIrisNormalized: FloatArray?,
    val rightIrisNormalized: FloatArray?,
)

data class SegmentationMask(
    val width: Int,
    val height: Int,
    /** GPU texture containing the mask, or null if the implementation is CPU-only later. */
    val textureId: Int?,
)

data class PoseBody(
    val trackingId: Int,
    val landmarksNormalized: FloatArray,
)
