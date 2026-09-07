package com.aurafx.sdk.vision

import com.aurafx.sdk.beauty.FaceLandmarks
import java.util.concurrent.atomic.AtomicReference

/**
 * Latest tracker output. ImageAnalysis publishes here; the GL thread only reads.
 * Empty READY means the model ran and found no face. UNAVAILABLE means no model.
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

    private val published = AtomicReference(TrackingData.unavailable(0L))

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

    fun publish(data: TrackingData) {
        published.set(data)
    }

    fun clear() {
        published.set(TrackingData.unavailable(0L))
    }

    fun latest(): TrackingData = published.get()

    fun hasAnyImplementation(): Boolean =
        faceDetector != null ||
            faceLandmarkTracker != null ||
            faceTracker != null ||
            irisTracker != null ||
            segmenter != null ||
            poseTracker != null ||
            latest().status == VisionStatus.READY

    fun process(frame: VisionFrame): TrackingData {
        val fromAnalyzer = published.get()
        if (fromAnalyzer.status == VisionStatus.READY) {
            return fromAnalyzer.copy(frameTimestampNs = frame.timestampNs)
        }
        if (faceDetector == null && faceLandmarkTracker == null && faceTracker == null &&
            irisTracker == null && segmenter == null && poseTracker == null
        ) {
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
