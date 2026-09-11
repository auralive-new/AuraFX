package com.aurafx.sdk.vision.mediapipe

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aurafx.sdk.beauty.FaceLandmarks
import com.aurafx.sdk.beauty.LandmarkSmoother
import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.vision.DetectedFace
import com.aurafx.sdk.vision.FaceMesh
import com.aurafx.sdk.vision.FaceTrack
import com.aurafx.sdk.vision.IrisTrack
import com.aurafx.sdk.vision.PoseBody
import com.aurafx.sdk.vision.PoseIndex
import com.aurafx.sdk.vision.SceneMaskPacker
import com.aurafx.sdk.vision.SegmentationMask
import com.aurafx.sdk.vision.TrackingData
import com.aurafx.sdk.vision.VisionStatus
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One CameraX ImageAnalysis callback: face + multiclass selfie + pose.
 * Does not open a second camera.
 */
class MediaPipeSceneAnalyzer(
    context: Context,
    private val mirrorX: () -> Boolean,
    private val onResult: (TrackingData) -> Unit,
) : ImageAnalysis.Analyzer, AutoCloseable {
    private val app = context.applicationContext
    private val ready = AtomicBoolean(false)
    private var landmarker: FaceLandmarker? = null
    private var segmenter: ImageSegmenter? = null
    private var pose: PoseLandmarker? = null
    private var smootherPrev: FaceLandmarks? = null
    private var reusable: Bitmap? = null
    private var prevPacked: ByteArray? = null
    private val videoClock = MediaPipeVideoClock()

    @Volatile var loadError: String? = null
        private set
    @Volatile var segmenterReady: Boolean = false
        private set
    @Volatile var poseReady: Boolean = false
        private set
    @Volatile var faceReady: Boolean = false
        private set

    init {
        try {
            landmarker = FaceLandmarker.createFromOptions(
                app,
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath(FACE_MODEL).build())
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumFaces(1)
                    .setMinFaceDetectionConfidence(0.3f)
                    .setMinFacePresenceConfidence(0.3f)
                    .setMinTrackingConfidence(0.3f)
                    .setOutputFaceBlendshapes(false)
                    .setOutputFacialTransformationMatrixes(false)
                    .build(),
            )
            faceReady = true
        } catch (t: Throwable) {
            loadError = t.message
            AuraFxLog.e("FaceLandmarker failed", t)
        }
        try {
            segmenter = ImageSegmenter.createFromOptions(
                app,
                ImageSegmenter.ImageSegmenterOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath(SEG_MODEL).build())
                    .setRunningMode(RunningMode.VIDEO)
                    .setOutputCategoryMask(true)
                    .setOutputConfidenceMasks(false)
                    .build(),
            )
            segmenterReady = true
            AuraFxLog.i("ImageSegmenter ready ($SEG_MODEL)")
        } catch (t: Throwable) {
            AuraFxLog.e("ImageSegmenter failed", t)
        }
        try {
            pose = PoseLandmarker.createFromOptions(
                app,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath(POSE_MODEL).build())
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.3f)
                    .setMinPosePresenceConfidence(0.3f)
                    .setMinTrackingConfidence(0.3f)
                    .build(),
            )
            poseReady = true
            AuraFxLog.i("PoseLandmarker ready ($POSE_MODEL)")
        } catch (t: Throwable) {
            AuraFxLog.e("PoseLandmarker failed", t)
        }
        ready.set(faceReady || segmenterReady || poseReady)
        AuraFxLog.i(
            "MediaPipe scene face=$faceReady seg=$segmenterReady pose=$poseReady " +
                "loadError=${loadError ?: "none"}",
        )
        if (!ready.get()) {
            onResult(TrackingData.unavailable(0L).copy(visionProvider = "mediapipe-scene:load-failed"))
        }
    }

    override fun analyze(image: ImageProxy) {
        if (!ready.get()) {
            image.close()
            return
        }
        try {
            val bitmap = ImageProxyBitmaps.copy(image, reusable)
            if (bitmap !== reusable && reusable?.isRecycled != false) {
                reusable = bitmap
            }
            val mpImage = BitmapImageBuilder(bitmap).build()
            val tsMs = videoClock.nextMs(image.imageInfo.timestamp)
            val mirror = mirrorX()
            val facePart = runFace(mpImage, tsMs, image.imageInfo.timestamp, mirror)
            val seg = runSeg(mpImage, tsMs, mirror)
            val poses = runPose(mpImage, tsMs, mirror)
            onResult(
                TrackingData(
                    status = VisionStatus.READY,
                    frameTimestampNs = image.imageInfo.timestamp,
                    faces = facePart.faces,
                    meshes = facePart.meshes,
                    tracks = facePart.tracks,
                    iris = facePart.iris,
                    landmarks = facePart.landmarks,
                    segmentation = seg,
                    pose = poses,
                    visionProvider = providerLabel(),
                ),
            )
        } catch (t: Throwable) {
            AuraFxLog.w("Scene analyze failed", t)
        } finally {
            image.close()
        }
    }

    override fun close() {
        ready.set(false)
        try { landmarker?.close() } catch (_: Throwable) {}
        try { segmenter?.close() } catch (_: Throwable) {}
        try { pose?.close() } catch (_: Throwable) {}
        landmarker = null
        segmenter = null
        pose = null
        reusable?.recycle()
        reusable = null
        smootherPrev = null
        prevPacked = null
    }

    private fun runFace(mpImage: com.google.mediapipe.framework.image.MPImage, tsMs: Long, tsNs: Long, mirror: Boolean): TrackingData {
        val marker = landmarker ?: return TrackingData(status = VisionStatus.READY, frameTimestampNs = tsNs)
        val result = marker.detectForVideo(mpImage, tsMs)
        val lists = result.faceLandmarks()
        if (lists.isNullOrEmpty()) {
            smootherPrev = null
            return TrackingData(status = VisionStatus.READY, frameTimestampNs = tsNs)
        }
        val face = lists[0]
        val count = face.size
        val xy = FloatArray(count * 2)
        var minX = 1f
        var minY = 1f
        var maxX = 0f
        var maxY = 0f
        for (i in 0 until count) {
            var x = face[i].x()
            val y = face[i].y()
            if (mirror) x = 1f - x
            xy[i * 2] = x.coerceIn(0f, 1f)
            xy[i * 2 + 1] = y.coerceIn(0f, 1f)
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
        }
        val raw = FaceLandmarks(1, xy, count, tsNs, false)
        val smoothed = LandmarkSmoother.smooth(smootherPrev, raw)
        smootherPrev = smoothed
        val iris = if (count >= 478) {
            listOf(
                IrisTrack(1, floatArrayOf(smoothed.x(468), smoothed.y(468)), floatArrayOf(smoothed.x(473), smoothed.y(473))),
            )
        } else emptyList()
        return TrackingData(
            status = VisionStatus.READY,
            frameTimestampNs = tsNs,
            faces = listOf(DetectedFace(1, floatArrayOf(minX, minY, maxX - minX, maxY - minY))),
            meshes = listOf(FaceMesh(1, smoothed.xy, null)),
            tracks = listOf(FaceTrack(1, smoothed.stable)),
            iris = iris,
            landmarks = smoothed,
        )
    }

    private fun runSeg(mpImage: com.google.mediapipe.framework.image.MPImage, tsMs: Long, mirror: Boolean): SegmentationMask? {
        val seg = segmenter ?: return null
        val result = seg.segmentForVideo(mpImage, tsMs)
        val cat = result.categoryMask().orElse(null) ?: return null
        val bytes = mpImageToCategory(cat) ?: return null
        val packed = SceneMaskPacker.packCategory(cat.width, cat.height, bytes, mirror, prevPacked)
        prevPacked = packed.packedRgba
        return packed
    }

    private fun runPose(mpImage: com.google.mediapipe.framework.image.MPImage, tsMs: Long, mirror: Boolean): List<PoseBody> {
        val marker = pose ?: return emptyList()
        val result = marker.detectForVideo(mpImage, tsMs)
        val lists = result.landmarks()
        if (lists.isNullOrEmpty()) return emptyList()
        val lm = lists[0]
        val xy = FloatArray(PoseIndex.COUNT * 2)
        val n = minOf(lm.size, PoseIndex.COUNT)
        for (i in 0 until n) {
            var x = lm[i].x()
            val y = lm[i].y()
            if (mirror) x = 1f - x
            xy[i * 2] = x
            xy[i * 2 + 1] = y
        }
        val body = PoseBody(1, xy)
        return if (body.isValid()) listOf(body) else emptyList()
    }

    private fun mpImageToCategory(image: MPImage): ByteArray? {
        return try {
            val buf: ByteBuffer = ByteBufferExtractor.extract(image)
            buf.rewind()
            val n = image.width * image.height
            val out = ByteArray(n)
            when {
                buf.remaining() >= n -> {
                    buf.get(out)
                    out
                }
                buf.remaining() >= n * 4 -> {
                    val fb = buf.asFloatBuffer()
                    for (i in 0 until n) out[i] = fb.get().toInt().toByte()
                    out
                }
                else -> null
            }
        } catch (t: Throwable) {
            AuraFxLog.w("category extract failed", t)
            null
        }
    }

    private fun providerLabel(): String = buildString {
        append("mediapipe-scene")
        if (faceReady) append("+face")
        if (segmenterReady) append("+multiclass")
        if (poseReady) append("+pose")
    }

    companion object {
        const val FACE_MODEL = "models/face_landmarker.task"
        const val SEG_MODEL = "models/selfie_multiclass_256x256.tflite"
        const val POSE_MODEL = "models/pose_landmarker_lite.task"
    }
}
