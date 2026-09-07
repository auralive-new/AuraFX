package com.aurafx.sdk.makeup

import com.aurafx.sdk.api.MakeupParameters
import com.aurafx.sdk.beauty.FaceLandmarks
import com.aurafx.sdk.beauty.FaceTopology
import com.aurafx.sdk.beauty.RegionMaskBuilder
import kotlin.math.hypot

/**
 * Packed makeup masks, display UV:
 * A: R skin, G lips, B lids, A brows
 * B: R liner, G lashes, B iris ring, A concealer/under-eye
 */
object MakeupMaskBuilder {
    const val SIZE = 160

    fun build(lm: FaceLandmarks, geo: MakeupGeometry, params: MakeupParameters): Pair<ByteArray, ByteArray> {
        val a = ByteArray(SIZE * SIZE * 4)
        val b = ByteArray(SIZE * SIZE * 4)
        val skin = FloatArray(SIZE * SIZE)
        val lips = FloatArray(SIZE * SIZE)
        val lids = FloatArray(SIZE * SIZE)
        val brows = FloatArray(SIZE * SIZE)
        val liner = FloatArray(SIZE * SIZE)
        val lashes = FloatArray(SIZE * SIZE)
        val iris = FloatArray(SIZE * SIZE)
        val concealer = FloatArray(SIZE * SIZE)
        fill(skin, lm, FaceTopology.FACE_OVAL)
        fill(lips, lm, FaceTopology.LIPS_OUTER)
        val inner = FloatArray(SIZE * SIZE)
        fill(inner, lm, FaceTopology.LIPS_INNER)
        for (i in lips.indices) lips[i] = (lips[i] - inner[i]).coerceIn(0f, 1f)
        fill(lids, lm, LEFT_LID)
        fill(lids, lm, RIGHT_LID)
        fill(brows, lm, FaceTopology.LEFT_BROW)
        fill(brows, lm, FaceTopology.RIGHT_BROW)
        fill(concealer, lm, FaceTopology.LEFT_UNDER_EYE)
        fill(concealer, lm, FaceTopology.RIGHT_UNDER_EYE)
        val eyeball = FloatArray(SIZE * SIZE)
        fill(eyeball, lm, FaceTopology.LEFT_EYE)
        fill(eyeball, lm, FaceTopology.RIGHT_EYE)
        for (i in lids.indices) lids[i] = (lids[i] * (1f - eyeball[i] * 0.85f)).coerceIn(0f, 1f)
        stampIris(iris, geo.leftIris)
        stampIris(iris, geo.rightIris)
        for (pl in geo.liners) stampPolyline(liner, pl)
        for (st in geo.lashes) stampStrand(lashes, st)
        pack(a, skin, lips, lids, brows)
        pack(b, liner, lashes, iris, concealer)
        return a to b
    }

    fun channelOccupied(pixels: ByteArray, channel: Int): Boolean =
        RegionMaskBuilder.channelSum(pixels, channel) > 0

    private val LEFT_LID = intArrayOf(33, 246, 161, 160, 159, 158, 157, 173, 133, 155, 154, 153, 145, 144, 163, 7)
    private val RIGHT_LID = intArrayOf(263, 466, 388, 387, 386, 385, 384, 398, 362, 381, 380, 374, 373, 390, 249)

    private fun fill(target: FloatArray, lm: FaceLandmarks, indices: IntArray) {
        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        for (idx in indices) {
            if (!lm.has(idx)) continue
            xs.add(lm.x(idx))
            ys.add(lm.y(idx))
        }
        if (xs.size < 3) return
        val xa = xs.toFloatArray()
        val ya = ys.toFloatArray()
        val minX = (xa.min() * SIZE).toInt().coerceIn(0, SIZE - 1)
        val maxX = (xa.max() * SIZE).toInt().coerceIn(0, SIZE - 1)
        val minY = (ya.min() * SIZE).toInt().coerceIn(0, SIZE - 1)
        val maxY = (ya.max() * SIZE).toInt().coerceIn(0, SIZE - 1)
        for (py in minY..maxY) {
            val y = (py + 0.5f) / SIZE
            for (px in minX..maxX) {
                val x = (px + 0.5f) / SIZE
                if (RegionMaskBuilder.pointInPolygon(x, y, xa, ya)) {
                    target[py * SIZE + px] = 1f
                }
            }
        }
    }

    private fun stampIris(target: FloatArray, iris: FloatArray) {
        val cx = iris[0] * SIZE
        val cy = iris[1] * SIZE
        val r = iris[2] * SIZE
        val rp = iris[3] * SIZE
        val minX = (cx - r).toInt().coerceIn(0, SIZE - 1)
        val maxX = (cx + r).toInt().coerceIn(0, SIZE - 1)
        val minY = (cy - r).toInt().coerceIn(0, SIZE - 1)
        val maxY = (cy + r).toInt().coerceIn(0, SIZE - 1)
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val d = hypot(x - cx, y - cy)
                if (d <= r && d >= rp) {
                    val t = ((d - rp) / (r - rp + 1e-4f)).coerceIn(0f, 1f)
                    val v = (1f - kotlin.math.abs(t - 0.55f) * 1.6f).coerceIn(0f, 1f)
                    val i = y * SIZE + x
                    target[i] = maxOf(target[i], v)
                }
            }
        }
    }

    private fun stampPolyline(target: FloatArray, line: MakeupPolyline) = stampPolyline(target, line.points, line.widthStart, line.widthEnd)

    private fun stampPolyline(
        target: FloatArray,
        points: List<Pair<Float, Float>>,
        widthStart: Float,
        widthEnd: Float,
    ) {
        if (points.size < 2) return
        for (i in 0 until points.size - 1) {
            val t = i / (points.size - 1).toFloat()
            val w = widthStart * (1f - t) + widthEnd * t
            stampSegment(target, points[i].first, points[i].second, points[i + 1].first, points[i + 1].second, w)
        }
    }

    private fun stampStrand(target: FloatArray, s: Strand) {
        val pts = ArrayList<Pair<Float, Float>>(8)
        for (i in 0..7) {
            val t = i / 7f
            val u = 1f - t
            val x = u * u * s.ax + 2f * u * t * s.bx + t * t * s.cx
            val y = u * u * s.ay + 2f * u * t * s.by + t * t * s.cy
            pts.add(x to y)
        }
        stampPolyline(target, pts, s.widthRoot, s.widthTip)
    }

    private fun stampSegment(target: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float, width: Float) {
        val steps = (hypot(x1 - x0, y1 - y0) * SIZE * 3f).toInt().coerceIn(2, 48)
        val wr = (width * SIZE).coerceAtLeast(0.6f)
        for (s in 0..steps) {
            val t = s / steps.toFloat()
            val cx = (x0 + (x1 - x0) * t) * SIZE
            val cy = (y0 + (y1 - y0) * t) * SIZE
            val r = wr.toInt().coerceAtLeast(1)
            for (dy in -r..r) {
                for (dx in -r..r) {
                    if (dx * dx + dy * dy > r * r) continue
                    val x = cx.toInt() + dx
                    val y = cy.toInt() + dy
                    if (x in 0 until SIZE && y in 0 until SIZE) {
                        val i = y * SIZE + x
                        target[i] = 1f
                    }
                }
            }
        }
    }

    private fun pack(out: ByteArray, r: FloatArray, g: FloatArray, b: FloatArray, a: FloatArray) {
        for (i in r.indices) {
            val o = i * 4
            out[o] = u8(r[i])
            out[o + 1] = u8(g[i])
            out[o + 2] = u8(b[i])
            out[o + 3] = u8(a[i])
        }
    }

    private fun u8(v: Float): Byte = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
}

private typealias MakeupPolyline = Polyline
