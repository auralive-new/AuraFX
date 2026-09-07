package com.aurafx.sdk.scene

import com.aurafx.sdk.api.BodyParameters
import com.aurafx.sdk.vision.PoseBody
import com.aurafx.sdk.vision.PoseIndex
import kotlin.math.exp
import kotlin.math.hypot

/**
 * Localized body UV warp. Face, hair, and background stay near identity
 * via mask weights applied in the fragment stage; this field is already
 * clustered on pose joints rather than a full-frame scale.
 */
object BodyWarpField {
    const val GRID = 28

    fun vertexCount() = GRID * GRID
    fun indexCount() = (GRID - 1) * (GRID - 1) * 6

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

    fun buildMesh(pose: PoseBody?, snap: BodyParameters, out: FloatArray): FloatArray {
        val stride = 6
        val needed = vertexCount() * stride
        val mesh = if (out.size >= needed) out else FloatArray(needed)
        val p = pose?.takeIf { it.isValid() }
        val midX = if (p != null) {
            (lx(p, PoseIndex.LEFT_HIP) + lx(p, PoseIndex.RIGHT_HIP)) * 0.5f
        } else 0.5f
        for (gy in 0 until GRID) {
            for (gx in 0 until GRID) {
                val u = gx / (GRID - 1).toFloat()
                val v = gy / (GRID - 1).toFloat()
                val ndcX = u * 2f - 1f
                val ndcY = (1f - v) * 2f - 1f
                var dx = 0f
                var dy = 0f
                if (p != null && !snap.isIdentity()) {
                    dx += inward(u, v, p, PoseIndex.LEFT_HIP, PoseIndex.RIGHT_HIP, snap.slim * 0.10f, midX, 0.12f)
                    dx += inward(u, v, p, PoseIndex.LEFT_HIP, PoseIndex.RIGHT_HIP, snap.waist * 0.14f, midX, 0.08f)
                    val sh = (lx(p, PoseIndex.LEFT_SHOULDER) + lx(p, PoseIndex.RIGHT_SHOULDER)) * 0.5f
                    dx += inward(u, v, p, PoseIndex.LEFT_SHOULDER, PoseIndex.RIGHT_SHOULDER, snap.shoulders * 0.10f, sh, 0.10f)
                    dx += inward(u, v, p, PoseIndex.LEFT_KNEE, PoseIndex.RIGHT_KNEE, snap.legs * 0.09f, midX, 0.14f)
                    dx += armIn(u, v, p, true, snap.arms * 0.06f)
                    dx += armIn(u, v, p, false, snap.arms * 0.06f)
                    val hipY = (ly(p, PoseIndex.LEFT_HIP) + ly(p, PoseIndex.RIGHT_HIP)) * 0.5f
                    val shY = (ly(p, PoseIndex.LEFT_SHOULDER) + ly(p, PoseIndex.RIGHT_SHOULDER)) * 0.5f
                    val torsoW = gauss(u - midX, v - (shY + hipY) * 0.5f, 0.16f)
                    dy += (hipY - v) * 0.06f * snap.torso * torsoW
                }
                if (!dx.isFinite()) dx = 0f
                if (!dy.isFinite()) dy = 0f
                dx = dx.coerceIn(-0.08f, 0.08f)
                dy = dy.coerceIn(-0.06f, 0.06f)
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

    private fun inward(
        u: Float,
        v: Float,
        p: PoseBody,
        left: Int,
        right: Int,
        amount: Float,
        centerX: Float,
        radius: Float,
    ): Float {
        val w = gauss(u - lx(p, left), v - ly(p, left), radius) +
            gauss(u - lx(p, right), v - ly(p, right), radius)
        return (centerX - u) * amount * w.coerceAtMost(1f)
    }

    private fun armIn(u: Float, v: Float, p: PoseBody, left: Boolean, amount: Float): Float {
        val sh = if (left) PoseIndex.LEFT_SHOULDER else PoseIndex.RIGHT_SHOULDER
        val el = if (left) PoseIndex.LEFT_ELBOW else PoseIndex.RIGHT_ELBOW
        val wr = if (left) PoseIndex.LEFT_WRIST else PoseIndex.RIGHT_WRIST
        val hand = gauss(u - lx(p, wr), v - ly(p, wr), 0.06f)
        val w = gauss(u - lx(p, sh), v - ly(p, sh), 0.07f) +
            gauss(u - lx(p, el), v - ly(p, el), 0.07f)
        val protectHands = (1f - hand).coerceIn(0f, 1f)
        val mid = (lx(p, sh) + lx(p, el)) * 0.5f
        return (mid - u) * amount * w * protectHands
    }

    private fun lx(p: PoseBody, i: Int) = p.landmarksNormalized[i * 2]
    private fun ly(p: PoseBody, i: Int) = p.landmarksNormalized[i * 2 + 1]

    private fun gauss(dx: Float, dy: Float, radius: Float): Float {
        val r = radius.coerceAtLeast(0.02f)
        return exp(-(dx * dx + dy * dy) / (2f * r * r)).toFloat()
    }

    fun maxAbs(mesh: FloatArray): Float {
        var m = 0f
        var i = 4
        while (i < mesh.size) {
            m = maxOf(m, hypot(mesh[i], mesh[i + 1]))
            i += 6
        }
        return m
    }
}
