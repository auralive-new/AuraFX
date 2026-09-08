package com.aurafx.sdk.gift

import com.aurafx.sdk.api.GiftFxAnchor
import com.aurafx.sdk.api.GiftFxCategory
import com.aurafx.sdk.api.GiftFxCleanupBehavior
import com.aurafx.sdk.api.GiftFxLayer
import com.aurafx.sdk.api.GiftFxOcclusion
import com.aurafx.sdk.api.GiftFxRendererType
import com.aurafx.sdk.api.GiftFxTrackingNeed

data class GiftFxParticleConfig(
    val maxParticles: Int,
    val emitPerSec: Float,
    val gravity: Float,
    val trail: Boolean,
    val kind: Int,
) {
    fun valid(): Boolean =
        maxParticles in 8..256 &&
            emitPerSec.isFinite() &&
            emitPerSec >= 0f &&
            gravity.isFinite() &&
            kind >= 0
}

data class GiftFxDefinition(
    val giftId: String,
    val displayName: String,
    val durationMs: Int,
    val category: GiftFxCategory,
    val rendererType: GiftFxRendererType,
    val requiredTracking: GiftFxTrackingNeed,
    val layer: GiftFxLayer,
    val anchor: GiftFxAnchor,
    val occlusion: GiftFxOcclusion,
    val timeline: GiftFxTimelineSpec,
    val particles: GiftFxParticleConfig,
    val cleanup: GiftFxCleanupBehavior,
    val shaderLook: Int,
    val gpuRendered: Boolean = true,
) {
    fun valid(): Boolean {
        if (giftId.isBlank() || displayName.isBlank()) return false
        if (durationMs !in 8000..10000) return false
        if (!timeline.valid() || !particles.valid()) return false
        if (shaderLook < 0) return false
        if (!gpuRendered) return false
        return true
    }
}
