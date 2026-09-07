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
    val packedRgba: ByteArray = ByteArray(0),
    val source: String = "none",
    val personCoverage: Float = 0f,
    val hairCoverage: Float = 0f,
    val bodyCoverage: Float = 0f,
    val faceCoverage: Float = 0f,
    val textureId: Int? = null,
) {
    fun inBounds(): Boolean =
        width > 0 && height > 0 && packedRgba.size == width * height * 4
}

data class PoseBody(
    val trackingId: Int,
    val landmarksNormalized: FloatArray,
) {
    fun isValid(): Boolean {
        if (landmarksNormalized.size < PoseIndex.COUNT * 2) return false
        return landmarksNormalized.all { it.isFinite() && it in -0.25f..1.25f }
    }
}
