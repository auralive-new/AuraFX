package com.aurafx.sdk.beauty

/**
 * Named engines required by Step 2. They share [BeautyRig] so API groups cannot
 * silently diverge from the GPU snapshot.
 */
class SkinEngine(private val rig: BeautyRig) {
    fun apply(block: com.aurafx.sdk.api.SkinParameters.() -> Unit) = rig.applySkin(block)
    fun snapshot() = rig.snapshot()
}

class BeautyEngine(private val rig: BeautyRig) {
    fun apply(block: com.aurafx.sdk.api.BeautyParameters.() -> Unit) = rig.applyBeauty(block)
    fun snapshot() = rig.snapshot()
}

class FaceShapeEngine(private val rig: BeautyRig) {
    fun apply(block: com.aurafx.sdk.api.FaceShapeParameters.() -> Unit) = rig.applyFaceShape(block)
    fun snapshot() = rig.snapshot()
}

class SkinMaskProvider {
    fun build(landmarks: FaceLandmarks?) = RegionMaskBuilder.build(landmarks)
}

class TeethMaskProvider {
    fun teethCoverage(landmarks: FaceLandmarks?): Int =
        RegionMaskBuilder.channelSum(RegionMaskBuilder.build(landmarks), channel = 1)
}

class FaceMeshProvider {
    fun fromTracking(landmarks: FaceLandmarks?): FaceLandmarks? = landmarks
}
