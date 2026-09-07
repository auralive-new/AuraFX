package com.aurafx.sdk.beauty

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Packed xy landmarks in display-normalized [0,1] space (origin top-left).
 * [count] is the number of points; array size is at least count*2.
 */
data class FaceLandmarks(
    val trackingId: Int,
    val xy: FloatArray,
    val count: Int,
    val timestampNs: Long,
    val stable: Boolean,
) {
    fun x(i: Int): Float = xy[i * 2]
    fun y(i: Int): Float = xy[i * 2 + 1]

    fun has(i: Int): Boolean = i >= 0 && i < count
}

object LandmarkSmoother {
    fun smooth(
        previous: FaceLandmarks?,
        incoming: FaceLandmarks,
        alpha: Float = 0.4f,
    ): FaceLandmarks {
        if (previous == null || previous.trackingId != incoming.trackingId || previous.count != incoming.count) {
            return incoming.copy(stable = false)
        }
        val jump = meanDelta(previous, incoming)
        val a = if (jump > 0.08f) 0.85f else alpha.coerceIn(0.05f, 0.95f)
        val out = FloatArray(incoming.count * 2)
        for (i in 0 until incoming.count * 2) {
            val v = previous.xy[i] * (1f - a) + incoming.xy[i] * a
            out[i] = if (v.isFinite()) v else incoming.xy[i]
        }
        val stable = jump < 0.02f
        return FaceLandmarks(incoming.trackingId, out, incoming.count, incoming.timestampNs, stable)
    }

    fun meanDelta(a: FaceLandmarks, b: FaceLandmarks): Float {
        val n = minOf(a.count, b.count)
        if (n == 0) return Float.POSITIVE_INFINITY
        var sum = 0f
        for (i in 0 until n) {
            sum += hypot(a.x(i) - b.x(i), a.y(i) - b.y(i))
        }
        return sum / n
    }

    fun interOcular(lm: FaceLandmarks): Float {
        if (!lm.has(FaceTopology.LEFT_EYE_OUTER) || !lm.has(FaceTopology.RIGHT_EYE_OUTER)) return 0.12f
        return hypot(
            lm.x(FaceTopology.LEFT_EYE_OUTER) - lm.x(FaceTopology.RIGHT_EYE_OUTER),
            lm.y(FaceTopology.LEFT_EYE_OUTER) - lm.y(FaceTopology.RIGHT_EYE_OUTER),
        ).coerceAtLeast(0.04f)
    }
}

fun FaceLandmarks.allFinite(): Boolean {
    for (i in 0 until count * 2) {
        if (!xy[i].isFinite()) return false
    }
    return true
}

fun FaceLandmarks.clampedToUnit(): FaceLandmarks {
    val out = FloatArray(count * 2)
    for (i in 0 until count * 2) {
        out[i] = xy[i].coerceIn(0f, 1f)
    }
    return copy(xy = out)
}

fun absMax(a: Float, b: Float): Float = if (abs(a) >= abs(b)) a else b
