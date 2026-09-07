package com.aurafx.sdk.beauty

/**
 * Low-resolution RGBA region mask, display UV space:
 * R = skin (face oval minus protect)
 * G = teeth (inner-lip cavity)
 * B = under-eye
 * A = protect (eyes, brows, lips, nostrils) — smoothing/whitening skip
 */
object RegionMaskBuilder {
    const val SIZE = 144

    fun build(lm: FaceLandmarks?): ByteArray {
        val pixels = ByteArray(SIZE * SIZE * 4)
        if (lm == null || lm.count < 100) return pixels
        val skin = FloatArray(SIZE * SIZE)
        val teeth = FloatArray(SIZE * SIZE)
        val circles = FloatArray(SIZE * SIZE)
        val protect = FloatArray(SIZE * SIZE)
        fillPolygon(skin, lm, FaceTopology.FACE_OVAL, 1f)
        fillPolygon(protect, lm, FaceTopology.LEFT_EYE, 1f)
        fillPolygon(protect, lm, FaceTopology.RIGHT_EYE, 1f)
        fillPolygon(protect, lm, FaceTopology.LEFT_BROW, 1f)
        fillPolygon(protect, lm, FaceTopology.RIGHT_BROW, 1f)
        fillPolygon(protect, lm, FaceTopology.LIPS_OUTER, 1f)
        fillPolygon(teeth, lm, FaceTopology.LIPS_INNER, 1f)
        fillPolygon(circles, lm, FaceTopology.LEFT_UNDER_EYE, 1f)
        fillPolygon(circles, lm, FaceTopology.RIGHT_UNDER_EYE, 1f)
        if (lm.has(FaceTopology.NOSTRIL_LEFT)) stamp(protect, lm.x(FaceTopology.NOSTRIL_LEFT), lm.y(FaceTopology.NOSTRIL_LEFT), 2)
        if (lm.has(FaceTopology.NOSTRIL_RIGHT)) stamp(protect, lm.x(FaceTopology.NOSTRIL_RIGHT), lm.y(FaceTopology.NOSTRIL_RIGHT), 2)
        if (lm.count > 472) {
            fillPolygon(protect, lm, FaceTopology.LEFT_IRIS, 1f)
            fillPolygon(protect, lm, FaceTopology.RIGHT_IRIS, 1f)
        }
        dilate(protect, 1)
        for (i in skin.indices) {
            val p = protect[i].coerceIn(0f, 1f)
            val s = (skin[i] * (1f - p)).coerceIn(0f, 1f)
            val t = (teeth[i] * (1f - p * 0.35f)).coerceIn(0f, 1f)
            val c = (circles[i] * (1f - p)).coerceIn(0f, 1f)
            val o = i * 4
            pixels[o] = toByte(s)
            pixels[o + 1] = toByte(t)
            pixels[o + 2] = toByte(c)
            pixels[o + 3] = toByte(p)
        }
        return pixels
    }

    fun channelSum(pixels: ByteArray, channel: Int): Int {
        var sum = 0
        var i = channel
        while (i < pixels.size) {
            sum += pixels[i].toInt() and 0xff
            i += 4
        }
        return sum
    }

    private fun fillPolygon(target: FloatArray, lm: FaceLandmarks, indices: IntArray, value: Float) {
        val pts = ArrayList<Pair<Float, Float>>(indices.size)
        for (idx in indices) {
            if (!lm.has(idx)) continue
            pts.add(lm.x(idx) to lm.y(idx))
        }
        if (pts.size < 3) return
        val xs = FloatArray(pts.size) { pts[it].first }
        val ys = FloatArray(pts.size) { pts[it].second }
        val minX = (xs.min() * SIZE).toInt().coerceIn(0, SIZE - 1)
        val maxX = (xs.max() * SIZE).toInt().coerceIn(0, SIZE - 1)
        val minY = (ys.min() * SIZE).toInt().coerceIn(0, SIZE - 1)
        val maxY = (ys.max() * SIZE).toInt().coerceIn(0, SIZE - 1)
        for (py in minY..maxY) {
            val y = (py + 0.5f) / SIZE
            for (px in minX..maxX) {
                val x = (px + 0.5f) / SIZE
                if (pointInPolygon(x, y, xs, ys)) {
                    target[py * SIZE + px] = value
                }
            }
        }
    }

    internal fun pointInPolygon(x: Float, y: Float, xs: FloatArray, ys: FloatArray): Boolean {
        var inside = false
        var j = xs.size - 1
        for (i in xs.indices) {
            val yi = ys[i]
            val yj = ys[j]
            val xi = xs[i]
            val xj = xs[j]
            val intersect = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).let { if (it == 0f) 1e-6f else it }) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    private fun stamp(target: FloatArray, nx: Float, ny: Float, radius: Int) {
        val cx = (nx * SIZE).toInt()
        val cy = (ny * SIZE).toInt()
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = cx + dx
                val y = cy + dy
                if (x in 0 until SIZE && y in 0 until SIZE) {
                    target[y * SIZE + x] = 1f
                }
            }
        }
    }

    private fun dilate(target: FloatArray, radius: Int) {
        if (radius <= 0) return
        val copy = target.copyOf()
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                if (copy[y * SIZE + x] <= 0f) continue
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val xx = x + dx
                        val yy = y + dy
                        if (xx in 0 until SIZE && yy in 0 until SIZE) {
                            target[yy * SIZE + xx] = 1f
                        }
                    }
                }
            }
        }
    }

    private fun toByte(v: Float): Byte = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
}
