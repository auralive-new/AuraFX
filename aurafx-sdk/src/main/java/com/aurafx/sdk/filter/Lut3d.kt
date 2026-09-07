package com.aurafx.sdk.filter

import com.aurafx.sdk.api.finiteOr
import kotlin.math.floor

/**
 * Real 3D LUT. Lattice values are actual RGB transforms, not identity padding.
 */
class Lut3d(
    val size: Int,
    val rgb: FloatArray,
) {
    init {
        require(size >= 2) { "LUT size must be >= 2" }
        require(rgb.size == size * size * size * 3) { "LUT buffer size mismatch" }
    }

    fun validate(): LutValidation {
        if (size < 2 || size > 65) return LutValidation.Invalid("LUT size $size out of range 2..65")
        if (rgb.size != size * size * size * 3) return LutValidation.Invalid("LUT buffer mismatch")
        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        var delta = 0f
        for (i in rgb.indices) {
            val v = rgb[i]
            if (!v.isFinite()) return LutValidation.Invalid("NaN/Inf at $i")
            if (v < min) min = v
            if (v > max) max = v
        }
        val id = identity(size)
        for (i in rgb.indices) {
            delta = maxOf(delta, kotlin.math.abs(rgb[i] - id.rgb[i]))
        }
        return LutValidation.Ok(min, max, delta)
    }

    fun sample(r: Float, g: Float, b: Float): FloatArray {
        val s = (size - 1).toFloat()
        val x = r.finiteOr(0f).coerceIn(0f, 1f) * s
        val y = g.finiteOr(0f).coerceIn(0f, 1f) * s
        val z = b.finiteOr(0f).coerceIn(0f, 1f) * s
        val x0 = floor(x).toInt().coerceIn(0, size - 1)
        val y0 = floor(y).toInt().coerceIn(0, size - 1)
        val z0 = floor(z).toInt().coerceIn(0, size - 1)
        val x1 = (x0 + 1).coerceAtMost(size - 1)
        val y1 = (y0 + 1).coerceAtMost(size - 1)
        val z1 = (z0 + 1).coerceAtMost(size - 1)
        val tx = x - x0
        val ty = y - y0
        val tz = z - z0
        val c000 = texel(x0, y0, z0)
        val c100 = texel(x1, y0, z0)
        val c010 = texel(x0, y1, z0)
        val c110 = texel(x1, y1, z0)
        val c001 = texel(x0, y0, z1)
        val c101 = texel(x1, y0, z1)
        val c011 = texel(x0, y1, z1)
        val c111 = texel(x1, y1, z1)
        val c00 = lerp(c000, c100, tx)
        val c10 = lerp(c010, c110, tx)
        val c01 = lerp(c001, c101, tx)
        val c11 = lerp(c011, c111, tx)
        val c0 = lerp(c00, c10, ty)
        val c1 = lerp(c01, c11, ty)
        return lerp(c0, c1, tz)
    }

    fun toRgb8(): ByteArray {
        val out = ByteArray(size * size * size * 3)
        for (i in 0 until size * size * size) {
            val o = i * 3
            out[o] = toU8(rgb[o])
            out[o + 1] = toU8(rgb[o + 1])
            out[o + 2] = toU8(rgb[o + 2])
        }
        return out
    }

    fun isIdentity(epsilon: Float = 2f / size): Boolean {
        val id = identity(size)
        for (i in rgb.indices) {
            if (kotlin.math.abs(rgb[i] - id.rgb[i]) > epsilon) return false
        }
        return true
    }

    private fun texel(x: Int, y: Int, z: Int): FloatArray {
        val i = ((z * size * size) + (y * size) + x) * 3
        return floatArrayOf(rgb[i], rgb[i + 1], rgb[i + 2])
    }

    companion object {
        const val DEFAULT_SIZE = 32

        fun identity(size: Int = DEFAULT_SIZE): Lut3d {
            val rgb = FloatArray(size * size * size * 3)
            val den = (size - 1).toFloat()
            var i = 0
            for (z in 0 until size) {
                val b = z / den
                for (y in 0 until size) {
                    val g = y / den
                    for (x in 0 until size) {
                        val r = x / den
                        rgb[i++] = r
                        rgb[i++] = g
                        rgb[i++] = b
                    }
                }
            }
            return Lut3d(size, rgb)
        }

        fun fromGrade(grade: ColorGrade, size: Int = DEFAULT_SIZE): Lut3d {
            val rgb = FloatArray(size * size * size * 3)
            val den = (size - 1).toFloat()
            var i = 0
            for (z in 0 until size) {
                val b = z / den
                for (y in 0 until size) {
                    val g = y / den
                    for (x in 0 until size) {
                        val r = x / den
                        val out = ColorProcessor.applyGrade(r, g, b, grade)
                        rgb[i++] = out[0]
                        rgb[i++] = out[1]
                        rgb[i++] = out[2]
                    }
                }
            }
            return Lut3d(size, rgb)
        }

        private fun lerp(a: FloatArray, b: FloatArray, t: Float) = floatArrayOf(
            a[0] + (b[0] - a[0]) * t,
            a[1] + (b[1] - a[1]) * t,
            a[2] + (b[2] - a[2]) * t,
        )

        private fun toU8(v: Float): Byte =
            (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
    }
}

sealed class LutValidation {
    data class Ok(val min: Float, val max: Float, val identityDelta: Float) : LutValidation()
    data class Invalid(val reason: String) : LutValidation()
}

/**
 * Loads and validates `.cube` LUTs if a host supplies files later.
 * Catalog filters bake LUTs procedurally; they do not depend on assets.
 */
object LutAssetLoader {
    fun parseCube(text: String): Lut3d {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        var size = -1
        val values = ArrayList<Float>()
        for (line in lines) {
            val parts = line.split(Regex("\\s+"))
            when {
                parts[0].equals("LUT_3D_SIZE", ignoreCase = true) && parts.size >= 2 -> {
                    size = parts[1].toInt()
                }
                parts[0].equals("TITLE", ignoreCase = true) -> Unit
                parts[0].equals("DOMAIN_MIN", ignoreCase = true) ||
                    parts[0].equals("DOMAIN_MAX", ignoreCase = true) -> Unit
                parts.size >= 3 &&
                    (parts[0].first().isDigit() || parts[0].startsWith("-") || parts[0].startsWith(".")) -> {
                    values.add(parts[0].toFloat())
                    values.add(parts[1].toFloat())
                    values.add(parts[2].toFloat())
                }
            }
        }
        if (size < 2) throw IllegalArgumentException("Missing LUT_3D_SIZE")
        if (values.size != size * size * size * 3) {
            throw IllegalArgumentException("CUBE data count ${values.size} != ${size * size * size * 3}")
        }
        val lut = Lut3d(size, values.toFloatArray())
        when (val v = lut.validate()) {
            is LutValidation.Invalid -> throw IllegalArgumentException(v.reason)
            is LutValidation.Ok -> Unit
        }
        return lut
    }
}
