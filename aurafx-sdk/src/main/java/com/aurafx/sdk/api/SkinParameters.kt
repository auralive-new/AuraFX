package com.aurafx.sdk.api

/**
 * Skin / complexion controls. All values are processing intensities, not UI flags.
 * Range is 0..1 unless noted. 0 leaves that term inactive.
 */
data class SkinParameters(
    var fineSmooth: Float = 0f,
    var smoothness: Float = 0f,
    var texturePreserve: Float = 0.7f,
    var blemishReduction: Float = 0f,
    var evenness: Float = 0f,
    var brightness: Float = 0f,
    var whiten: Float = 0f,
    var ruddy: Float = 0f,
    var tone: Float = 0.5f,
    var naturalSkin: Float = 0.4f,
) {
    fun clampInPlace() {
        fineSmooth = clamp01(fineSmooth)
        smoothness = clamp01(smoothness)
        texturePreserve = clamp01(texturePreserve)
        blemishReduction = clamp01(blemishReduction)
        evenness = clamp01(evenness)
        brightness = clamp01(brightness)
        whiten = clamp01(whiten)
        ruddy = clamp01(ruddy)
        tone = clamp01(tone)
        naturalSkin = clamp01(naturalSkin)
    }

    fun reset() {
        fineSmooth = 0f
        smoothness = 0f
        texturePreserve = 0.7f
        blemishReduction = 0f
        evenness = 0f
        brightness = 0f
        whiten = 0f
        ruddy = 0f
        tone = 0.5f
        naturalSkin = 0.4f
    }

    fun copyFrom(other: SkinParameters) {
        fineSmooth = other.fineSmooth
        smoothness = other.smoothness
        texturePreserve = other.texturePreserve
        blemishReduction = other.blemishReduction
        evenness = other.evenness
        brightness = other.brightness
        whiten = other.whiten
        ruddy = other.ruddy
        tone = other.tone
        naturalSkin = other.naturalSkin
    }

    fun isIdentity(): Boolean =
        fineSmooth == 0f && smoothness == 0f && blemishReduction == 0f &&
            evenness == 0f && brightness == 0f && whiten == 0f && ruddy == 0f &&
            tone == 0.5f
}

internal fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)

internal fun clampSigned(v: Float): Float = v.coerceIn(-1f, 1f)
