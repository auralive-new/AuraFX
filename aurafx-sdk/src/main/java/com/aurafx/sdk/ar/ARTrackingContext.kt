package com.aurafx.sdk.ar

import com.aurafx.sdk.beauty.FaceLandmarks
import com.aurafx.sdk.beauty.FaceTopology
import com.aurafx.sdk.beauty.LandmarkSmoother
import kotlin.math.atan2
import kotlin.math.hypot

data class HeadPose(
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val iod: Float,
    val faceCx: Float,
    val faceCy: Float,
) {
    fun allFinite(): Boolean =
        yaw.isFinite() && pitch.isFinite() && roll.isFinite() && iod.isFinite() &&
            faceCx.isFinite() && faceCy.isFinite()
}

data class FaceExpression(
    val blinkLeft: Float,
    val blinkRight: Float,
    val smile: Float,
    val mouthOpen: Float,
    val browRaise: Float,
) {
    fun allFinite(): Boolean =
        blinkLeft.isFinite() && blinkRight.isFinite() && smile.isFinite() &&
            mouthOpen.isFinite() && browRaise.isFinite()
}

/**
 * Landmark-derived pose and expression. Not a fake slider. Values are 0..1 or radians.
 */
class ARTrackingContext(
    val pose: HeadPose,
    val expression: FaceExpression,
    val leftEye: FloatArray,
    val rightEye: FloatArray,
    val leftIris: FloatArray,
    val rightIris: FloatArray,
    val mouth: FloatArray,
    val forehead: FloatArray,
    val leftCheek: FloatArray,
    val rightCheek: FloatArray,
    val leftBrow: FloatArray,
    val rightBrow: FloatArray,
    val chin: FloatArray,
    val timestampNs: Long,
    val trackingId: Int,
) {
    fun allFinite(): Boolean =
        pose.allFinite() && expression.allFinite() &&
            leftEye.all { it.isFinite() } && rightEye.all { it.isFinite() }

    companion object {
        fun from(lm: FaceLandmarks?): ARTrackingContext? {
            if (lm == null || lm.count < 100 || !lm.has(FaceTopology.LEFT_EYE_OUTER)) return null
            val iod = LandmarkSmoother.interOcular(lm)
            val lx = mid(lm, FaceTopology.LEFT_EYE_INNER, FaceTopology.LEFT_EYE_OUTER)
            val rx = mid(lm, FaceTopology.RIGHT_EYE_INNER, FaceTopology.RIGHT_EYE_OUTER)
            val roll = atan2(rx[1] - lx[1], rx[0] - lx[0])
            val midX = (lx[0] + rx[0]) * 0.5f
            val midY = (lx[1] + rx[1]) * 0.5f
            val noseX = if (lm.has(FaceTopology.NOSE_TIP)) lm.x(FaceTopology.NOSE_TIP) else midX
            val noseY = if (lm.has(FaceTopology.NOSE_TIP)) lm.y(FaceTopology.NOSE_TIP) else midY
            val yaw = ((noseX - midX) / iod).coerceIn(-1.2f, 1.2f)
            val pitch = ((noseY - midY) / iod).coerceIn(-1.2f, 1.2f)
            val pose = HeadPose(yaw, pitch, roll, iod, midX, midY)
            val blinkL = (1f - eyeOpen(lm, 159, 145, FaceTopology.LEFT_EYE_INNER, FaceTopology.LEFT_EYE_OUTER)).coerceIn(0f, 1f)
            val blinkR = (1f - eyeOpen(lm, 386, 374, FaceTopology.RIGHT_EYE_INNER, FaceTopology.RIGHT_EYE_OUTER)).coerceIn(0f, 1f)
            val mouthOpen = lipGap(lm) / iod
            val smile = ((mouthWidth(lm) / iod) - 0.35f).coerceIn(0f, 1f)
            val brow = browRaise(lm, iod)
            val expr = FaceExpression(blinkL, blinkR, smile.coerceIn(0f, 1f), mouthOpen.coerceIn(0f, 1f), brow)
            val irisL = if (lm.has(468)) floatArrayOf(lm.x(468), lm.y(468)) else lx
            val irisR = if (lm.has(473)) floatArrayOf(lm.x(473), lm.y(473)) else rx
            return ARTrackingContext(
                pose = pose,
                expression = expr,
                leftEye = lx,
                rightEye = rx,
                leftIris = irisL,
                rightIris = irisR,
                mouth = if (lm.has(13)) floatArrayOf(lm.x(13), lm.y(13)) else floatArrayOf(midX, midY + iod * 0.9f),
                forehead = if (lm.has(10)) floatArrayOf(lm.x(10), lm.y(10)) else floatArrayOf(midX, midY - iod * 1.1f),
                leftCheek = if (lm.has(50)) floatArrayOf(lm.x(50), lm.y(50)) else floatArrayOf(lx[0], midY + iod * 0.4f),
                rightCheek = if (lm.has(280)) floatArrayOf(lm.x(280), lm.y(280)) else floatArrayOf(rx[0], midY + iod * 0.4f),
                leftBrow = if (lm.has(105)) floatArrayOf(lm.x(105), lm.y(105)) else floatArrayOf(lx[0], lx[1] - iod * 0.35f),
                rightBrow = if (lm.has(334)) floatArrayOf(lm.x(334), lm.y(334)) else floatArrayOf(rx[0], rx[1] - iod * 0.35f),
                chin = if (lm.has(152)) floatArrayOf(lm.x(152), lm.y(152)) else floatArrayOf(midX, midY + iod * 1.6f),
                timestampNs = lm.timestampNs,
                trackingId = lm.trackingId,
            )
        }

        private fun mid(lm: FaceLandmarks, a: Int, b: Int): FloatArray {
            if (!lm.has(a) || !lm.has(b)) return floatArrayOf(0.5f, 0.4f)
            return floatArrayOf((lm.x(a) + lm.x(b)) * 0.5f, (lm.y(a) + lm.y(b)) * 0.5f)
        }

        private fun eyeOpen(lm: FaceLandmarks, up: Int, down: Int, inner: Int, outer: Int): Float {
            if (!lm.has(up) || !lm.has(down) || !lm.has(inner) || !lm.has(outer)) return 0.35f
            val h = hypot(lm.x(up) - lm.x(down), lm.y(up) - lm.y(down))
            val w = hypot(lm.x(inner) - lm.x(outer), lm.y(inner) - lm.y(outer)).coerceAtLeast(1e-4f)
            return (h / w / 0.38f).coerceIn(0f, 1f)
        }

        private fun lipGap(lm: FaceLandmarks): Float {
            if (!lm.has(13) || !lm.has(14)) return 0f
            return hypot(lm.x(13) - lm.x(14), lm.y(13) - lm.y(14))
        }

        private fun mouthWidth(lm: FaceLandmarks): Float {
            if (!lm.has(61) || !lm.has(291)) return 0.12f
            return hypot(lm.x(61) - lm.x(291), lm.y(61) - lm.y(291))
        }

        private fun browRaise(lm: FaceLandmarks, iod: Float): Float {
            if (!lm.has(105) || !lm.has(159)) return 0f
            val d = (lm.y(159) - lm.y(105)) / iod
            return d.coerceIn(0f, 1.2f) / 1.2f
        }
    }
}
