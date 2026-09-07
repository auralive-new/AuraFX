package com.aurafx.sdk.vision.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aurafx.sdk.beauty.FaceLandmarks
import com.aurafx.sdk.beauty.LandmarkSmoother
import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.vision.DetectedFace
import com.aurafx.sdk.vision.FaceMesh
import com.aurafx.sdk.vision.FaceTrack
import com.aurafx.sdk.vision.IrisTrack
import com.aurafx.sdk.vision.TrackingData
import com.aurafx.sdk.vision.VisionStatus
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real MediaPipe Face Landmarker (468/478 points). Runs on the CameraX analysis
 * executor, not the GL thread. Does not invent faces when the model is missing.
 */
class MediaPipeFaceLandmarkerAnalyzer(
    context: Context,
    private val mirrorX: () -> Boolean,
    private val onResult: (TrackingData) -> Unit,
) : ImageAnalysis.Analyzer, AutoCloseable {
    private val app = context.applicationContext
    private val ready = AtomicBoolean(false)
    private var landmarker: FaceLandmarker? = null
    private var smootherPrev: FaceLandmarks? = null
    private var reusable: Bitmap? = null

    @Volatile var loadError: String? = null
        private set

    init {
        try {
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath(MODEL_ASSET)
                        .build(),
                )
                .setRunningMode(RunningMode.VIDEO)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setOutputFaceBlendshapes(false)
                .setOutputFacialTransformationMatrixes(false)
                .build()
            landmarker = FaceLandmarker.createFromOptions(app, options)
            ready.set(true)
            AuraFxLog.i("MediaPipe FaceLandmarker ready ($MODEL_ASSET)")
        } catch (t: Throwable) {
            loadError = t.message ?: t.javaClass.simpleName
            ready.set(false)
            AuraFxLog.e("MediaPipe FaceLandmarker failed to load", t)
            onResult(TrackingData.unavailable(0L).copy(visionProvider = "mediapipe-face-landmarker:load-failed"))
        }
    }

    override fun analyze(image: ImageProxy) {
        val marker = landmarker
        if (!ready.get() || marker == null) {
            image.close()
            return
        }
        try {
            val bitmap = imageProxyToBitmap(image)
            val mpImage = BitmapImageBuilder(bitmap).build()
            val tsMs = image.imageInfo.timestamp / 1_000_000L
            val result = marker.detectForVideo(mpImage, tsMs)
            val lists = result.faceLandmarks()
            if (lists.isNullOrEmpty()) {
                smootherPrev = null
                onResult(
                    TrackingData(
                        status = VisionStatus.READY,
                        frameTimestampNs = image.imageInfo.timestamp,
                        visionProvider = PROVIDER,
                    ),
                )
                return
            }
            val face = lists[0]
            val count = face.size
            val xy = FloatArray(count * 2)
            val mirror = mirrorX()
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
            val raw = FaceLandmarks(
                trackingId = 1,
                xy = xy,
                count = count,
                timestampNs = image.imageInfo.timestamp,
                stable = false,
            )
            val smoothed = LandmarkSmoother.smooth(smootherPrev, raw)
            smootherPrev = smoothed
            val iris = if (count >= 478) {
                listOf(
                    IrisTrack(
                        trackingId = 1,
                        leftIrisNormalized = floatArrayOf(smoothed.x(468), smoothed.y(468)),
                        rightIrisNormalized = floatArrayOf(smoothed.x(473), smoothed.y(473)),
                    ),
                )
            } else {
                emptyList()
            }
            onResult(
                TrackingData(
                    status = VisionStatus.READY,
                    frameTimestampNs = image.imageInfo.timestamp,
                    faces = listOf(
                        DetectedFace(1, floatArrayOf(minX, minY, maxX - minX, maxY - minY)),
                    ),
                    meshes = listOf(FaceMesh(1, smoothed.xy, indices = null)),
                    tracks = listOf(FaceTrack(1, smoothed.stable)),
                    iris = iris,
                    landmarks = smoothed,
                    visionProvider = PROVIDER,
                ),
            )
        } catch (t: Throwable) {
            AuraFxLog.w("MediaPipe detect failed", t)
        } finally {
            image.close()
        }
    }

    override fun close() {
        ready.set(false)
        try {
            landmarker?.close()
        } catch (_: Throwable) {
        }
        landmarker = null
        reusable?.recycle()
        reusable = null
        smootherPrev = null
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val yPlane = image.planes[0]
        val w = image.width
        val h = image.height
        val bmp = reusable?.takeIf { it.width == w && it.height == h }
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { reusable = it }
        if (image.format == android.graphics.ImageFormat.FLEX_RGBA_8888 || image.planes.size == 1) {
            val buffer = yPlane.buffer.duplicate()
            buffer.rewind()
            if (yPlane.pixelStride == 4 && yPlane.rowStride == w * 4) {
                bmp.copyPixelsFromBuffer(buffer)
            } else {
                val row = ByteArray(yPlane.rowStride)
                val argb = IntArray(w * h)
                for (rowI in 0 until h) {
                    buffer.position(rowI * yPlane.rowStride)
                    buffer.get(row, 0, minOf(row.size, yPlane.rowStride))
                    for (col in 0 until w) {
                        val o = col * yPlane.pixelStride
                        val r = row[o].toInt() and 0xff
                        val g = row[o + 1].toInt() and 0xff
                        val b = row[o + 2].toInt() and 0xff
                        val a = if (yPlane.pixelStride > 3) row[o + 3].toInt() and 0xff else 255
                        argb[rowI * w + col] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }
                bmp.setPixels(argb, 0, w, 0, 0, w, h)
            }
        } else {
            yuv420ToBitmap(image, bmp)
        }
        val rotation = image.imageInfo.rotationDegrees
        if (rotation == 0) return bmp
        val m = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    private fun yuv420ToBitmap(image: ImageProxy, out: Bitmap) {
        val y = image.planes[0]
        val u = image.planes[1]
        val v = image.planes[2]
        val w = image.width
        val h = image.height
        val pixels = IntArray(w * h)
        val yBuf = y.buffer
        val uBuf = u.buffer
        val vBuf = v.buffer
        val yRow = y.rowStride
        val yPix = y.pixelStride
        val uRow = u.rowStride
        val uPix = u.pixelStride
        val vRow = v.rowStride
        val vPix = v.pixelStride
        for (row in 0 until h) {
            for (col in 0 until w) {
                val yi = yBuf.get(row * yRow + col * yPix).toInt() and 0xff
                val ui = uBuf.get((row / 2) * uRow + (col / 2) * uPix).toInt() and 0xff
                val vi = vBuf.get((row / 2) * vRow + (col / 2) * vPix).toInt() and 0xff
                val yp = yi - 16
                val up = ui - 128
                val vp = vi - 128
                val r = (1.164f * yp + 1.596f * vp).toInt().coerceIn(0, 255)
                val g = (1.164f * yp - 0.392f * up - 0.813f * vp).toInt().coerceIn(0, 255)
                val b = (1.164f * yp + 2.017f * up).toInt().coerceIn(0, 255)
                pixels[row * w + col] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    companion object {
        const val MODEL_ASSET = "models/face_landmarker.task"
        const val PROVIDER = "mediapipe-face-landmarker"
    }
}
