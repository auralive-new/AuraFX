package com.aurafx.sdk.vision

/**
 * Face bounding-box detection. No Step 1 implementation is shipped.
 */
fun interface FaceDetector {
    fun detect(frame: VisionFrame): List<DetectedFace>
}

/**
 * Dense face landmarks / mesh. No Step 1 implementation is shipped.
 */
fun interface FaceLandmarkTracker {
    fun detect(frame: VisionFrame): List<FaceMesh>
}

/**
 * Temporal face identity / lock. No Step 1 implementation is shipped.
 */
fun interface FaceTracker {
    fun track(frame: VisionFrame): List<FaceTrack>
}

/**
 * Iris / gaze. No Step 1 implementation is shipped.
 */
fun interface IrisTracker {
    fun track(frame: VisionFrame): List<IrisTrack>
}

/**
 * Person / hair / selfie segmentation. No Step 1 implementation is shipped.
 */
fun interface Segmenter {
    fun segment(frame: VisionFrame): SegmentationMask?
}

/**
 * Body pose. No Step 1 implementation is shipped.
 */
fun interface PoseTracker {
    fun detect(frame: VisionFrame): List<PoseBody>
}
