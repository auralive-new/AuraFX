package com.aurafx.sdk.filter

import com.aurafx.sdk.api.FilterCategory

data class FilterDefinition(
    val id: String,
    val category: FilterCategory,
    val displayName: String,
    val grade: ColorGrade,
    val usesLut: Boolean,
    val sceneMode: Int = 0,
    val intensityMin: Float = 0f,
    val intensityMax: Float = 1f,
    val defaultIntensity: Float = 0.65f,
    val skinAware: Boolean = true,
    val lutSize: Int = Lut3d.DEFAULT_SIZE,
) {
    fun capabilities(): Set<String> = buildSet {
        add("parametric")
        if (usesLut) add("lut3d")
        if (skinAware) add("skin-aware")
        if (grade.bloom > 0f) add("bloom")
        if (grade.grain > 0f) add("grain")
        if (grade.vignette > 0f) add("vignette")
        if (sceneMode != 0) add("scene-gpu")
        add("intensity")
    }

    fun bakedLut(): Lut3d = Lut3d.fromGrade(grade, lutSize)

    fun thumbnailArgb(width: Int = 48, height: Int = 48): IntArray =
        FilterThumbnail.render(this, width, height)
}

/**
 * Thumbnails are generated from the same [ColorGrade] / LUT used on the GPU path.
 * They are not unrelated placeholder art.
 */
object FilterThumbnail {
    fun render(def: FilterDefinition, width: Int, height: Int): IntArray {
        val lut = if (def.usesLut) def.bakedLut() else null
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val v = y / (height - 1).toFloat()
            for (x in 0 until width) {
                val u = x / (width - 1).toFloat()
                val src = probeColor(u, v)
                val graded = ColorProcessor.applyGrade(src[0], src[1], src[2], def.grade)
                val mapped = if (lut != null) lut.sample(src[0], src[1], src[2]) else graded
                val mixed = ColorProcessor.mix(src, mapped, def.defaultIntensity)
                val vr = 1f - def.grade.vignette * vignette(u, v)
                val r = (mixed[0] * vr).coerceIn(0f, 1f)
                val g = (mixed[1] * vr).coerceIn(0f, 1f)
                val b = (mixed[2] * vr).coerceIn(0f, 1f)
                val ri = (r * 255f + 0.5f).toInt().coerceIn(0, 255)
                val gi = (g * 255f + 0.5f).toInt().coerceIn(0, 255)
                val bi = (b * 255f + 0.5f).toInt().coerceIn(0, 255)
                pixels[y * width + x] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }
        return pixels
    }

    /**
     * Synthetic color-chart + skin patches so each filter's thumbnail shows its own grade.
     */
    private fun probeColor(u: Float, v: Float): FloatArray {
        return when {
            v < 0.22f -> floatArrayOf(u, u * 0.92f, u * 0.85f)
            v < 0.44f -> {
                val skin = 0.55f + 0.25f * u
                floatArrayOf(skin, skin * 0.72f, skin * 0.58f)
            }
            v < 0.66f -> floatArrayOf(0.15f + 0.7f * u, 0.35f, 0.75f - 0.4f * u)
            v < 0.83f -> floatArrayOf(0.82f, 0.78f - 0.3f * u, 0.22f + 0.2f * u)
            else -> floatArrayOf(0.08f + 0.12f * u, 0.09f, 0.11f + 0.08f * (1f - u))
        }
    }

    private fun vignette(u: Float, v: Float): Float {
        val dx = u - 0.5f
        val dy = v - 0.5f
        val r = kotlin.math.sqrt(dx * dx + dy * dy)
        return ((r - 0.28f) / 0.55f).coerceIn(0f, 1f)
    }
}
