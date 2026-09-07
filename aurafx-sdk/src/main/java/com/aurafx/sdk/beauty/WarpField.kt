package com.aurafx.sdk.beauty

import kotlin.math.exp
import kotlin.math.hypot

/**
 * Localized UV displacement field on a regular grid covering the full frame.
 * Identity (all zeros) when shape parameters are 0. Background vertices stay near
 * zero via oval falloff — the full image is never uniformly scaled.
 */
object WarpField {
    const val GRID = 36

    fun vertexCount(): Int = GRID * GRID

    fun indexCount(): Int = (GRID - 1) * (GRID - 1) * 6

    fun buildIndices(): IntArray {
        val indices = IntArray(indexCount())
        var p = 0
        for (y in 0 until GRID - 1) {
            for (x in 0 until GRID - 1) {
                val i = y * GRID + x
                indices[p++] = i
                indices[p++] = i + 1
                indices[p++] = i + GRID
                indices[p++] = i + 1
                indices[p++] = i + GRID + 1
                indices[p++] = i + GRID
            }
        }
        return indices
    }

    /**
     * Interleaved x,y NDC (-1..1), u,v (0..1), dx,dy in UV space.
     */
    fun buildMesh(lm: FaceLandmarks?, snap: BeautySnapshot, out: FloatArray): FloatArray {
        val stride = 6
        val needed = vertexCount() * stride
        val mesh = if (out.size >= needed) out else FloatArray(needed)
        val scale = if (lm != null) LandmarkSmoother.interOcular(lm) else 0.12f
        val cx = if (lm != null && lm.has(1)) lm.x(1) else 0.5f
        val cy = if (lm != null && lm.has(1)) (lm.y(1) + lm.y(FaceTopology.CHIN)) * 0.5f else 0.5f
        val ovalR = scale * 2.6f
        for (gy in 0 until GRID) {
            for (gx in 0 until GRID) {
                val u = gx / (GRID - 1).toFloat()
                val v = gy / (GRID - 1).toFloat()
                val ndcX = u * 2f - 1f
                val ndcY = (1f - v) * 2f - 1f
                var dx = 0f
                var dy = 0f
                if (lm != null && !snap.shapeIdentity()) {
                    val falloff = ovalFalloff(u, v, cx, cy, ovalR)
                    if (falloff > 0.001f) {
                        val disp = displacementAt(u, v, lm, snap, scale)
                        dx = disp[0] * falloff
                        dy = disp[1] * falloff
                    }
                }
                if (!dx.isFinite()) dx = 0f
                if (!dy.isFinite()) dy = 0f
                dx = dx.coerceIn(-0.12f, 0.12f)
                dy = dy.coerceIn(-0.12f, 0.12f)
                val i = (gy * GRID + gx) * stride
                mesh[i] = ndcX
                mesh[i + 1] = ndcY
                mesh[i + 2] = u
                mesh[i + 3] = v
                mesh[i + 4] = dx
                mesh[i + 5] = dy
            }
        }
        return mesh
    }

    fun isIdentityMesh(mesh: FloatArray, epsilon: Float = 1e-6f): Boolean {
        val stride = 6
        var i = 4
        while (i < mesh.size) {
            if (kotlin.math.abs(mesh[i]) > epsilon || kotlin.math.abs(mesh[i + 1]) > epsilon) return false
            i += stride
        }
        return true
    }

    internal fun ovalFalloff(x: Float, y: Float, cx: Float, cy: Float, radius: Float): Float {
        val d = hypot(x - cx, (y - cy) * 0.92f)
        val inner = radius * 0.72f
        val outer = radius
        return when {
            d <= inner -> 1f
            d >= outer -> 0f
            else -> {
                val t = (d - inner) / (outer - inner)
                (1f - t) * (1f - t)
            }
        }
    }

    private fun displacementAt(
        u: Float,
        v: Float,
        lm: FaceLandmarks,
        snap: BeautySnapshot,
        scale: Float,
    ): FloatArray {
        var dx = 0f
        var dy = 0f
        val midX = if (lm.has(1)) lm.x(1) else 0.5f
        val chinY = if (lm.has(FaceTopology.CHIN)) lm.y(FaceTopology.CHIN) else 0.85f

        if (snap.vFace > 0f) {
            val w = clusterWeight(u, v, lm, concat(FaceTopology.JAW_LEFT, FaceTopology.JAW_RIGHT), scale * 0.55f)
            dx += (midX - u) * 0.22f * snap.vFace * w
            dy += (chinY - v) * 0.08f * snap.vFace * w
        }
        if (snap.cheekThin > 0f) {
            val w = clusterWeight(u, v, lm, concat(FaceTopology.LEFT_CHEEK, FaceTopology.RIGHT_CHEEK), scale * 0.5f)
            dx += (midX - u) * 0.18f * snap.cheekThin * w
        }
        if (snap.cheekSmall > 0f) {
            val w = clusterWeight(u, v, lm, concat(FaceTopology.LEFT_CHEEK, FaceTopology.RIGHT_CHEEK), scale * 0.55f)
            val fy = if (lm.has(1)) lm.y(1) else 0.45f
            dx += (midX - u) * 0.12f * snap.cheekSmall * w
            dy += (fy - v) * 0.08f * snap.cheekSmall * w
        }
        if (snap.cheekNarrow > 0f) {
            val w = clusterWeight(u, v, lm, concat(FaceTopology.LEFT_CHEEK, FaceTopology.RIGHT_CHEEK), scale * 0.45f)
            dx += (midX - u) * 0.26f * snap.cheekNarrow * w
        }
        if (snap.nose > 0f && lm.has(FaceTopology.LEFT_ALA) && lm.has(FaceTopology.RIGHT_ALA)) {
            val nx = lm.x(FaceTopology.NOSE_TIP)
            val ny = lm.y(FaceTopology.NOSE_TIP)
            val w = gauss(u - nx, v - ny, scale * 0.35f)
            dx += (nx - u) * 0.35f * snap.nose * w
            dy += (ny - v) * 0.08f * snap.nose * w
        }
        if (snap.eyeEnlarge > 0f) {
            dx += eyeScale(u, v, lm, left = true, amount = snap.eyeEnlarge, scale = scale)
            dy += eyeScaleY(u, v, lm, left = true, amount = snap.eyeEnlarge, scale = scale)
            dx += eyeScale(u, v, lm, left = false, amount = snap.eyeEnlarge, scale = scale)
            dy += eyeScaleY(u, v, lm, left = false, amount = snap.eyeEnlarge, scale = scale)
        }
        if (snap.eyeDistance != 0f) {
            val left = eyeCenter(lm, true)
            val right = eyeCenter(lm, false)
            val wL = gauss(u - left[0], v - left[1], scale * 0.42f)
            val wR = gauss(u - right[0], v - right[1], scale * 0.42f)
            dx += -0.12f * snap.eyeDistance * wL
            dx += 0.12f * snap.eyeDistance * wR
        }
        if (snap.mouth > 0f && lm.has(FaceTopology.MOUTH_CENTER)) {
            val mx = lm.x(FaceTopology.MOUTH_CENTER)
            val my = lm.y(FaceTopology.MOUTH_CENTER)
            val w = gauss(u - mx, v - my, scale * 0.4f)
            dx += (u - mx) * 0.22f * snap.mouth * w
            dy += (v - my) * 0.12f * snap.mouth * w
        }
        return floatArrayOf(dx, dy)
    }

    private fun eyeCenter(lm: FaceLandmarks, left: Boolean): FloatArray {
        val inner = if (left) FaceTopology.LEFT_EYE_INNER else FaceTopology.RIGHT_EYE_INNER
        val outer = if (left) FaceTopology.LEFT_EYE_OUTER else FaceTopology.RIGHT_EYE_OUTER
        if (lm.has(inner) && lm.has(outer)) {
            return floatArrayOf((lm.x(inner) + lm.x(outer)) * 0.5f, (lm.y(inner) + lm.y(outer)) * 0.5f)
        }
        return floatArrayOf(if (left) 0.35f else 0.65f, 0.4f)
    }

    private fun eyeScale(u: Float, v: Float, lm: FaceLandmarks, left: Boolean, amount: Float, scale: Float): Float {
        val c = eyeCenter(lm, left)
        val w = gauss(u - c[0], v - c[1], scale * 0.28f)
        return (u - c[0]) * 0.28f * amount * w
    }

    private fun eyeScaleY(u: Float, v: Float, lm: FaceLandmarks, left: Boolean, amount: Float, scale: Float): Float {
        val c = eyeCenter(lm, left)
        val w = gauss(u - c[0], v - c[1], scale * 0.28f)
        return (v - c[1]) * 0.22f * amount * w
    }

    private fun clusterWeight(u: Float, v: Float, lm: FaceLandmarks, indices: IntArray, radius: Float): Float {
        var best = 0f
        for (idx in indices) {
            if (!lm.has(idx)) continue
            val g = gauss(u - lm.x(idx), v - lm.y(idx), radius)
            if (g > best) best = g
        }
        return best
    }

    private fun gauss(dx: Float, dy: Float, radius: Float): Float {
        val r = radius.coerceAtLeast(0.01f)
        val n = (dx * dx + dy * dy) / (2f * r * r)
        return exp(-n).toFloat()
    }
}

internal fun concat(a: IntArray, b: IntArray): IntArray {
    val out = IntArray(a.size + b.size)
    a.copyInto(out, 0)
    b.copyInto(out, a.size)
    return out
}
