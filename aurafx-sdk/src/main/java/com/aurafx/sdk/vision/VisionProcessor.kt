package com.aurafx.sdk.vision

/**
 * Holds optional vision implementations. Step 1 registers none.
 *
 * Hosts may attach real implementations in later steps via [install]. Calling detect
 * methods without an implementation is not done — [process] short-circuits to
 * [TrackingData.unavailable].
 */
class VisionProcessor {
    @Volatile var faceDetector: FaceDetector? = null
        private set
    @Volatile var faceLandmarkTracker: FaceLandmarkTracker? = null
        private set
    @Volatile var faceTracker: FaceTracker? = null
        private set
    @Volatile var irisTracker: IrisTracker? = null
        private set
    @Volatile var segmenter: Segmenter? = null
        private set
    @Volatile var poseTracker: PoseTracker? = null
        private set

    fun install(
        faceDetector: FaceDetector? = this.faceDetector,
        faceLandmarkTracker: FaceLandmarkTracker? = this.faceLandmarkTracker,
        faceTracker: FaceTracker? = this.faceTracker,
        irisTracker: IrisTracker? = this.irisTracker,
        segmenter: Segmenter? = this.segmenter,
        poseTracker: PoseTracker? = this.poseTracker,
    ) {
        this.faceDetector = faceDetector
        this.faceLandmarkTracker = faceLandmarkTracker
        this.faceTracker = faceTracker
        this.irisTracker = irisTracker
        this.segmenter = segmenter
        this.poseTracker = poseTracker
    }

    fun hasAnyImplementation(): Boolean =
        faceDetector != null ||
            faceLandmarkTracker != null ||
            faceTracker != null ||
            irisTracker != null ||
            segmenter != null ||
            poseTracker != null

    /**
     * Runs only registered implementations. If none are registered, returns UNAVAILABLE
     * without inventing landmarks or detections.
     */
    fun process(frame: VisionFrame): TrackingData {
        if (!hasAnyImplementation()) {
            return TrackingData.unavailable(frame.timestampNs)
        }
        return TrackingData(
            status = VisionStatus.READY,
            frameTimestampNs = frame.timestampNs,
            faces = faceDetector?.detect(frame).orEmpty(),
            meshes = faceLandmarkTracker?.detect(frame).orEmpty(),
            tracks = faceTracker?.track(frame).orEmpty(),
            iris = irisTracker?.track(frame).orEmpty(),
            segmentation = segmenter?.segment(frame),
            pose = poseTracker?.detect(frame).orEmpty(),
        )
    }
}
