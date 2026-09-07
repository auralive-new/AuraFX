package com.aurafx.sdk.vision

/**
 * Aggregated tracker output for a single pipeline frame.
 *
 * When [status] is [VisionStatus.UNAVAILABLE], lists are empty because **no model ran**,
 * not because the camera saw zero people.
 */
data class TrackingData(
    val status: VisionStatus,
    val frameTimestampNs: Long,
    val faces: List<DetectedFace> = emptyList(),
    val meshes: List<FaceMesh> = emptyList(),
    val tracks: List<FaceTrack> = emptyList(),
    val iris: List<IrisTrack> = emptyList(),
    val segmentation: SegmentationMask? = null,
    val pose: List<PoseBody> = emptyList(),
) {
    companion object {
        fun unavailable(timestampNs: Long): TrackingData =
            TrackingData(status = VisionStatus.UNAVAILABLE, frameTimestampNs = timestampNs)
    }
}
