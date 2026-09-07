package com.aurafx.sdk.ar

object ARThumbnail {
    fun argb(def: AREffectDefinition, w: Int = 48, h: Int = 48): IntArray {
        val px = IntArray(w * h)
        for (y in 0 until h) {
            val v = y / (h - 1).toFloat()
            for (x in 0 until w) {
                val u = x / (w - 1).toFloat()
                val col = lookColor(def.look, u, v)
                val ri = (col[0] * 255f).toInt().coerceIn(0, 255)
                val gi = (col[1] * 255f).toInt().coerceIn(0, 255)
                val bi = (col[2] * 255f).toInt().coerceIn(0, 255)
                px[y * w + x] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }
        return px
    }

    private fun lookColor(look: Int, u: Float, v: Float): FloatArray = when (look) {
        0 -> floatArrayOf(0.95f, 0.55f + 0.2f * u, 0.18f)
        1 -> floatArrayOf(0.82f, 0.52f, 0.32f + 0.2f * v)
        2 -> floatArrayOf(0.12f, 0.1f, 0.1f + 0.15f * u)
        3 -> floatArrayOf(0.75f, 0.25f + 0.5f * u, 0.85f)
        4 -> floatArrayOf(u, 0.35f, 1f - u)
        5 -> floatArrayOf(0.95f, 0.82f, 0.35f)
        6 -> floatArrayOf(0.75f, 0.12f, 0.12f)
        7 -> floatArrayOf(1f, 0.45f + 0.2f * v, 0.55f)
        8 -> floatArrayOf(1f, 0.5f, 0.62f)
        else -> floatArrayOf(0.45f, 0.55f, 0.9f)
    }
}
