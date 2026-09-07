package com.aurafx.sdk.ar

object ARThumbnail {
    fun argb(def: AREffectDefinition, w: Int = 48, h: Int = 48): IntArray {
        val px = IntArray(w * h)
        for (y in 0 until h) {
            val v = y / (h - 1).toFloat()
            for (x in 0 until w) {
                val u = x / (w - 1).toFloat()
                val col = lookColor(def, u, v)
                val ri = (col[0] * 255f).toInt().coerceIn(0, 255)
                val gi = (col[1] * 255f).toInt().coerceIn(0, 255)
                val bi = (col[2] * 255f).toInt().coerceIn(0, 255)
                px[y * w + x] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }
        return px
    }

    private fun lookColor(def: AREffectDefinition, u: Float, v: Float): FloatArray {
        val t = (def.look % 7) / 7f
        return floatArrayOf(
            (def.paint[0] * (0.55f + 0.45f * u) + t * 0.08f).coerceIn(0f, 1f),
            (def.paint[1] * (0.55f + 0.45f * v)).coerceIn(0f, 1f),
            (def.paint[2] * (0.6f + 0.4f * (1f - u))).coerceIn(0f, 1f),
        )
    }
}
