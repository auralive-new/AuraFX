package com.aurafx.sdk.api

/**
 * Face-mesh deformation. 0 is identity. Positive values apply the named transform.
 * [eyeDistance] is signed: negative brings eyes closer, positive apart.
 */
data class FaceShapeParameters(
    var vFace: Float = 0f,
    var cheekThin: Float = 0f,
    var cheekSmall: Float = 0f,
    var cheekNarrow: Float = 0f,
    var nose: Float = 0f,
    var eyeEnlarge: Float = 0f,
    var eyeDistance: Float = 0f,
    var mouth: Float = 0f,
) {
    fun clampInPlace() {
        vFace = clamp01(vFace)
        cheekThin = clamp01(cheekThin)
        cheekSmall = clamp01(cheekSmall)
        cheekNarrow = clamp01(cheekNarrow)
        nose = clamp01(nose)
        eyeEnlarge = clamp01(eyeEnlarge)
        eyeDistance = clampSigned(eyeDistance)
        mouth = clamp01(mouth)
    }

    fun reset() {
        vFace = 0f
        cheekThin = 0f
        cheekSmall = 0f
        cheekNarrow = 0f
        nose = 0f
        eyeEnlarge = 0f
        eyeDistance = 0f
        mouth = 0f
    }

    fun copyFrom(other: FaceShapeParameters) {
        vFace = other.vFace
        cheekThin = other.cheekThin
        cheekSmall = other.cheekSmall
        cheekNarrow = other.cheekNarrow
        nose = other.nose
        eyeEnlarge = other.eyeEnlarge
        eyeDistance = other.eyeDistance
        mouth = other.mouth
    }

    fun isIdentity(): Boolean =
        vFace == 0f && cheekThin == 0f && cheekSmall == 0f && cheekNarrow == 0f &&
            nose == 0f && eyeEnlarge == 0f && eyeDistance == 0f && mouth == 0f
}
