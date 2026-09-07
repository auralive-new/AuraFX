package com.aurafx.sdk.filter

import com.aurafx.sdk.api.finiteOr
import kotlin.math.pow

/**
 * Internal grade used to build presets and bake 3D LUTs.
 * Public UI exposes filter id + intensity, not every knob.
 */
data class ColorGrade(
    val exposure: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 1f,
    val vibrance: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val blacks: Float = 0f,
    val whites: Float = 0f,
    val gamma: Float = 1f,
    val lift: Float = 0f,
    val gain: Float = 0f,
    val balanceShadowR: Float = 0f,
    val balanceShadowB: Float = 0f,
    val balanceHighR: Float = 0f,
    val balanceHighB: Float = 0f,
    val selectiveSat: Float = 1f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val bloom: Float = 0f,
    val skinProtect: Float = 0.72f,
    val featureProtect: Float = 0.82f,
) {
    fun clamp(): ColorGrade = copy(
        exposure = exposure.finiteOr(0f).coerceIn(-2f, 2f),
        brightness = brightness.finiteOr(0f).coerceIn(-1f, 1f),
        contrast = contrast.finiteOr(0f).coerceIn(-1f, 1f),
        saturation = saturation.finiteOr(1f).coerceIn(0f, 2.2f),
        vibrance = vibrance.finiteOr(0f).coerceIn(-1f, 1f),
        temperature = temperature.finiteOr(0f).coerceIn(-1f, 1f),
        tint = tint.finiteOr(0f).coerceIn(-1f, 1f),
        highlights = highlights.finiteOr(0f).coerceIn(-1f, 1f),
        shadows = shadows.finiteOr(0f).coerceIn(-1f, 1f),
        blacks = blacks.finiteOr(0f).coerceIn(-1f, 1f),
        whites = whites.finiteOr(0f).coerceIn(-1f, 1f),
        gamma = gamma.finiteOr(1f).coerceIn(0.4f, 2.4f),
        lift = lift.finiteOr(0f).coerceIn(-0.3f, 0.3f),
        gain = gain.finiteOr(0f).coerceIn(-0.4f, 0.4f),
        balanceShadowR = balanceShadowR.finiteOr(0f).coerceIn(-0.4f, 0.4f),
        balanceShadowB = balanceShadowB.finiteOr(0f).coerceIn(-0.4f, 0.4f),
        balanceHighR = balanceHighR.finiteOr(0f).coerceIn(-0.4f, 0.4f),
        balanceHighB = balanceHighB.finiteOr(0f).coerceIn(-0.4f, 0.4f),
        selectiveSat = selectiveSat.finiteOr(1f).coerceIn(0.4f, 1.4f),
        vignette = vignette.finiteOr(0f).coerceIn(0f, 1f),
        grain = grain.finiteOr(0f).coerceIn(0f, 1f),
        bloom = bloom.finiteOr(0f).coerceIn(0f, 1f),
        skinProtect = skinProtect.finiteOr(0.72f).coerceIn(0f, 1f),
        featureProtect = featureProtect.finiteOr(0.82f).coerceIn(0f, 1f),
    )

    fun isIdentity(): Boolean {
        val c = clamp()
        return c.exposure == 0f && c.brightness == 0f && c.contrast == 0f &&
            c.saturation == 1f && c.vibrance == 0f && c.temperature == 0f &&
            c.tint == 0f && c.highlights == 0f && c.shadows == 0f &&
            c.blacks == 0f && c.whites == 0f && c.gamma == 1f &&
            c.lift == 0f && c.gain == 0f &&
            c.balanceShadowR == 0f && c.balanceShadowB == 0f &&
            c.balanceHighR == 0f && c.balanceHighB == 0f &&
            c.selectiveSat == 1f && c.vignette == 0f && c.grain == 0f && c.bloom == 0f
    }

    fun allFinite(): Boolean = listOf(
        exposure, brightness, contrast, saturation, vibrance, temperature, tint,
        highlights, shadows, blacks, whites, gamma, lift, gain,
        balanceShadowR, balanceShadowB, balanceHighR, balanceHighB,
        selectiveSat, vignette, grain, bloom, skinProtect, featureProtect,
    ).all { it.isFinite() }
}

object ColorProcessor {
    fun applyGrade(r: Float, g: Float, b: Float, grade: ColorGrade): FloatArray {
        val p = grade.clamp()
        var cr = r.finiteOr(0f)
        var cg = g.finiteOr(0f)
        var cb = b.finiteOr(0f)
        val exp = 2f.pow(p.exposure)
        cr *= exp
        cg *= exp
        cb *= exp
        cr *= 1f + 0.20f * p.temperature - 0.07f * p.tint
        cg *= 1f + 0.12f * p.tint
        cb *= 1f - 0.20f * p.temperature - 0.07f * p.tint
        cr += p.brightness * 0.12f
        cg += p.brightness * 0.12f
        cb += p.brightness * 0.12f
        cr = contrastPivot(cr, p.contrast)
        cg = contrastPivot(cg, p.contrast)
        cb = contrastPivot(cb, p.contrast)
        var luma = luma(cr, cg, cb)
        val shadowW = (1f - smooth(luma, 0.0f, 0.45f)).coerceIn(0f, 1f)
        val highW = smooth(luma, 0.55f, 1f)
        val midLift = p.shadows * 0.18f * shadowW
        val midGain = p.highlights * 0.18f * highW
        cr += midLift + p.blacks * 0.10f * (1f - luma) + p.whites * 0.10f * luma
        cg += midLift + p.blacks * 0.10f * (1f - luma) + p.whites * 0.10f * luma
        cb += midLift + p.blacks * 0.10f * (1f - luma) + p.whites * 0.10f * luma
        cr += midGain
        cg += midGain
        cb += midGain
        cr = (cr + p.lift).coerceAtLeast(0f) * (1f + p.gain)
        cg = (cg + p.lift).coerceAtLeast(0f) * (1f + p.gain)
        cb = (cb + p.lift).coerceAtLeast(0f) * (1f + p.gain)
        val gma = p.gamma
        cr = cr.coerceAtLeast(0f).pow(1f / gma)
        cg = cg.coerceAtLeast(0f).pow(1f / gma)
        cb = cb.coerceAtLeast(0f).pow(1f / gma)
        luma = luma(cr, cg, cb)
        cr += p.balanceShadowR * shadowW * 0.12f + p.balanceHighR * highW * 0.12f
        cb += p.balanceShadowB * shadowW * 0.12f + p.balanceHighB * highW * 0.12f
        val satRgb = saturate(cr, cg, cb, p.saturation)
        cr = satRgb[0]; cg = satRgb[1]; cb = satRgb[2]
        val vib = vibrance(cr, cg, cb, p.vibrance)
        cr = vib[0]; cg = vib[1]; cb = vib[2]
        val sel = saturate(cr, cg, cb, p.selectiveSat)
        return floatArrayOf(
            sel[0].coerceIn(0f, 1.25f),
            sel[1].coerceIn(0f, 1.25f),
            sel[2].coerceIn(0f, 1.25f),
        )
    }

    fun mix(src: FloatArray, dst: FloatArray, t: Float): FloatArray {
        val u = t.finiteOr(0f).coerceIn(0f, 1f)
        return floatArrayOf(
            src[0] + (dst[0] - src[0]) * u,
            src[1] + (dst[1] - src[1]) * u,
            src[2] + (dst[2] - src[2]) * u,
        )
    }

    fun luma(r: Float, g: Float, b: Float): Float =
        0.2126f * r + 0.7152f * g + 0.0722f * b

    private fun contrastPivot(x: Float, c: Float): Float =
        ((x - 0.5f) * (1f + c * 0.85f) + 0.5f)

    private fun saturate(r: Float, g: Float, b: Float, sat: Float): FloatArray {
        val l = luma(r, g, b)
        return floatArrayOf(
            l + (r - l) * sat,
            l + (g - l) * sat,
            l + (b - l) * sat,
        )
    }

    private fun vibrance(r: Float, g: Float, b: Float, v: Float): FloatArray {
        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        val sat = if (mx > 1e-5f) (mx - mn) / mx else 0f
        val amount = v * (1f - sat)
        return saturate(r, g, b, 1f + amount)
    }

    private fun smooth(x: Float, e0: Float, e1: Float): Float {
        val t = ((x - e0) / (e1 - e0 + 1e-6f)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
