package com.aurafx.sdk.vision

import com.aurafx.sdk.beauty.FaceLandmarks

data class TrackingData(
    val status: VisionStatus,
    val frameTimestampNs: Long,
    val faces: List<DetectedFace> = emptyList(),
    val meshes: List<FaceMesh> = emptyList(),
    val tracks: List<FaceTrack> = emptyList(),
    val iris: List<IrisTrack> = emptyList(),
    val segmentation: SegmentationMask? = null,
    val pose: List<PoseBody> = emptyList(),
    val landmarks: FaceLandmarks? = null,
    val visionProvider: String? = null,
) {
    companion object {
        fun unavailable(timestampNs: Long): TrackingData =
            TrackingData(status = VisionStatus.UNAVAILABLE, frameTimestampNs = timestampNs)
    }
}
