package com.aurafx.sdk.api

import com.aurafx.sdk.api.finiteOr

enum class ARCategory {
    FaceAccessory,
    FaceMask,
    EyeEffect,
    Particle,
    FacePaint,
    Environment,
    Animated,
}

enum class AROcclusionMode {
    None,
    HairOccludes,
    PersonOnly,
    HairAndBody,
}

data class ARParameters(
    var effectId: String? = null,
    var intensity: Float = 0.85f,
) {
    fun clampInPlace() {
        intensity = intensity.finiteOr(0f).coerceIn(0f, 1f)
        if (effectId.isNullOrBlank()) effectId = null
    }

    fun isIdentity(): Boolean = effectId == null || intensity <= 0f

    fun reset() {
        effectId = null
        intensity = 0.85f
    }

    operator fun invoke(block: ARParameters.() -> Unit) {
        block()
        clampInPlace()
    }
}
