package com.aurafx.sdk.api

/**
 * Step-2 beauty sliders. Shape and skin fields here are the same backing values
 * exposed by [SkinParameters] / [FaceShapeParameters] so a control cannot be
 * UI-only.
 */
data class BeautyParameters(
    var fineSmooth: Float = 0f,
    var toothWhiten: Float = 0f,
    var whiten: Float = 0f,
    var ruddy: Float = 0f,
    var vFace: Float = 0f,
    var cheekThin: Float = 0f,
    var cheekSmall: Float = 0f,
    var cheekNarrow: Float = 0f,
    var nose: Float = 0f,
    var eyeEnlarge: Float = 0f,
    var eyeDistance: Float = 0f,
    var mouth: Float = 0f,
    var circles: Float = 0f,
) {
    fun clampInPlace() {
        fineSmooth = clamp01(fineSmooth)
        toothWhiten = clamp01(toothWhiten)
        whiten = clamp01(whiten)
        ruddy = clamp01(ruddy)
        vFace = clamp01(vFace)
        cheekThin = clamp01(cheekThin)
        cheekSmall = clamp01(cheekSmall)
        cheekNarrow = clamp01(cheekNarrow)
        nose = clamp01(nose)
        eyeEnlarge = clamp01(eyeEnlarge)
        eyeDistance = clampSigned(eyeDistance)
        mouth = clamp01(mouth)
        circles = clamp01(circles)
    }

    fun reset() {
        fineSmooth = 0f
        toothWhiten = 0f
        whiten = 0f
        ruddy = 0f
        vFace = 0f
        cheekThin = 0f
        cheekSmall = 0f
        cheekNarrow = 0f
        nose = 0f
        eyeEnlarge = 0f
        eyeDistance = 0f
        mouth = 0f
        circles = 0f
    }

    fun copyFrom(other: BeautyParameters) {
        fineSmooth = other.fineSmooth
        toothWhiten = other.toothWhiten
        whiten = other.whiten
        ruddy = other.ruddy
        vFace = other.vFace
        cheekThin = other.cheekThin
        cheekSmall = other.cheekSmall
        cheekNarrow = other.cheekNarrow
        nose = other.nose
        eyeEnlarge = other.eyeEnlarge
        eyeDistance = other.eyeDistance
        mouth = other.mouth
        circles = other.circles
    }
}
