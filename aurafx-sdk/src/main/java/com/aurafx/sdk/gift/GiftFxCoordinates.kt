package com.aurafx.sdk.gift

import com.aurafx.sdk.api.GiftFxAnchor
import com.aurafx.sdk.ar.ARTrackingContext
import com.aurafx.sdk.vision.PoseBody
import com.aurafx.sdk.vision.PoseIndex
import com.aurafx.sdk.vision.TrackingData

/**
 * Gift space is portrait 9:16. UVs stay in 0..1 of the camera frame; helpers
 * keep composition letterboxed so landscape buffers do not stretch gifts.
 */
object GiftFxCoordinates {
    const val PORTRAIT_ASPECT = 9f / 16f

    fun frameAspect(width: Int, height: Int): Float {
        if (width <= 0 || height <= 0) return PORTRAIT_ASPECT
        return width.toFloat() / height.toFloat()
    }

    /**
     * Maps a frame UV into a 9:16 content box centered in the buffer.
     * Outside the box, returned coordinates fall outside 0..1 (letterbox).
     */
    fun toPortraitUv(u: Float, v: Float, width: Int, height: Int): FloatArray {
        val aspect = frameAspect(width, height)
        return if (aspect > PORTRAIT_ASPECT) {
            val visW = PORTRAIT_ASPECT / aspect
            val x0 = (1f - visW) * 0.5f
            floatArrayOf((u - x0) / visW.coerceAtLeast(1e-5f), v)
        } else {
            val visH = aspect / PORTRAIT_ASPECT
            val y0 = (1f - visH) * 0.5f
            floatArrayOf(u, (v - y0) / visH.coerceAtLeast(1e-5f))
        }
    }

    fun fromPortraitUv(pu: Float, pv: Float, width: Int, height: Int): FloatArray {
        val aspect = frameAspect(width, height)
        return if (aspect > PORTRAIT_ASPECT) {
            val visW = PORTRAIT_ASPECT / aspect
            val x0 = (1f - visW) * 0.5f
            floatArrayOf(x0 + pu * visW, pv)
        } else {
            val visH = aspect / PORTRAIT_ASPECT
            val y0 = (1f - visH) * 0.5f
            floatArrayOf(pu, y0 + pv * visH)
        }
    }

    fun inPortraitSafeFrame(u: Float, v: Float, width: Int, height: Int): Boolean {
        val p = toPortraitUv(u, v, width, height)
        return p[0] in 0f..1f && p[1] in 0f..1f
    }

    fun portraitCenter(width: Int, height: Int): FloatArray =
        fromPortraitUv(0.5f, 0.5f, width, height)

    fun subjectUv(
        tracking: TrackingData,
        anchor: GiftFxAnchor,
        width: Int,
        height: Int,
        out: FloatArray,
    ) {
        val face = ARTrackingContext.from(tracking.landmarks)
        val pose = tracking.pose.firstOrNull()?.takeIf { it.isValid() }
        when (anchor) {
            GiftFxAnchor.ScreenCenter -> {
                val c = portraitCenter(width, height)
                out[0] = c[0]
                out[1] = c[1]
                out[2] = 0.18f
            }
            GiftFxAnchor.Face, GiftFxAnchor.Head -> {
                if (face != null) {
                    out[0] = if (anchor == GiftFxAnchor.Head) face.forehead[0] else face.pose.faceCx
                    out[1] = if (anchor == GiftFxAnchor.Head) face.forehead[1] else face.pose.faceCy
                    out[2] = face.pose.iod.coerceIn(0.06f, 0.45f)
                } else if (pose != null) {
                    fillFromPose(pose, GiftFxAnchor.Head, out)
                } else {
                    fallbackHead(width, height, out)
                }
            }
            GiftFxAnchor.Body, GiftFxAnchor.Shoulder -> {
                if (pose != null) {
                    fillFromPose(pose, anchor, out)
                } else if (face != null) {
                    out[0] = face.pose.faceCx
                    out[1] = (face.pose.faceCy + 0.22f).coerceIn(0f, 1f)
                    out[2] = (face.pose.iod * 1.6f).coerceIn(0.08f, 0.5f)
                } else {
                    fallbackHead(width, height, out)
                    out[1] = (out[1] + 0.18f).coerceIn(0f, 1f)
                }
            }
        }
        if (!out[0].isFinite()) out[0] = 0.5f
        if (!out[1].isFinite()) out[1] = 0.38f
        if (!out[2].isFinite() || out[2] <= 0f) out[2] = 0.16f
    }

    private fun fillFromPose(pose: PoseBody, anchor: GiftFxAnchor, out: FloatArray) {
        val lsx = lx(pose, PoseIndex.LEFT_SHOULDER)
        val rsx = lx(pose, PoseIndex.RIGHT_SHOULDER)
        val lsy = ly(pose, PoseIndex.LEFT_SHOULDER)
        val rsy = ly(pose, PoseIndex.RIGHT_SHOULDER)
        val lhx = lx(pose, PoseIndex.LEFT_HIP)
        val rhx = lx(pose, PoseIndex.RIGHT_HIP)
        val lhy = ly(pose, PoseIndex.LEFT_HIP)
        val rhy = ly(pose, PoseIndex.RIGHT_HIP)
        val nx = lx(pose, PoseIndex.NOSE)
        val ny = ly(pose, PoseIndex.NOSE)
        when (anchor) {
            GiftFxAnchor.Shoulder -> {
                out[0] = (lsx + rsx) * 0.5f
                out[1] = (lsy + rsy) * 0.5f
                out[2] = kotlin.math.abs(rsx - lsx).coerceIn(0.08f, 0.5f)
            }
            GiftFxAnchor.Head -> {
                out[0] = nx
                out[1] = ny
                out[2] = kotlin.math.abs(rsx - lsx).coerceIn(0.08f, 0.45f) * 0.55f
            }
            else -> {
                out[0] = (lhx + rhx + lsx + rsx) * 0.25f
                out[1] = (lhy + rhy + lsy + rsy) * 0.25f
                out[2] = kotlin.math.abs(rhx - lhx).coerceIn(0.1f, 0.55f)
            }
        }
    }

    private fun fallbackHead(width: Int, height: Int, out: FloatArray) {
        val c = fromPortraitUv(0.5f, 0.38f, width, height)
        out[0] = c[0]
        out[1] = c[1]
        out[2] = 0.16f
    }

    private fun lx(pose: PoseBody, i: Int) = pose.landmarksNormalized[i * 2]
    private fun ly(pose: PoseBody, i: Int) = pose.landmarksNormalized[i * 2 + 1]
}
