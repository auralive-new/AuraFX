package com.aurafx.sdk.gift

enum class GiftFxPhase {
    Start,
    Buildup,
    Main,
    Finale,
    Cleanup,
    Ended,
}

data class GiftFxTimelineSpec(
    val startEnd: Float = 0.12f,
    val buildupEnd: Float = 0.35f,
    val mainEnd: Float = 0.72f,
    val finaleEnd: Float = 0.90f,
) {
    fun valid(): Boolean =
        startEnd in 0.05f..0.2f &&
            buildupEnd > startEnd &&
            mainEnd > buildupEnd &&
            finaleEnd > mainEnd &&
            finaleEnd < 1f
}

/**
 * Deterministic 0..1 gift clock: start → buildup → main → finale → cleanup.
 */
class GiftFxTimeline(
    val spec: GiftFxTimelineSpec = GiftFxTimelineSpec(),
) {
    fun phase(normalized: Float): GiftFxPhase {
        val t = normalized.coerceIn(0f, 1.0001f)
        return when {
            t >= 1f -> GiftFxPhase.Ended
            t < spec.startEnd -> GiftFxPhase.Start
            t < spec.buildupEnd -> GiftFxPhase.Buildup
            t < spec.mainEnd -> GiftFxPhase.Main
            t < spec.finaleEnd -> GiftFxPhase.Finale
            else -> GiftFxPhase.Cleanup
        }
    }

    fun envelope(normalized: Float): Float {
        val t = normalized.coerceIn(0f, 1f)
        val rise = smoothstep(0f, spec.startEnd + 0.08f, t)
        val fall = 1f - smoothstep(spec.finaleEnd, 1f, t)
        return (rise * fall).coerceIn(0f, 1f)
    }

    fun phaseIndex(normalized: Float): Int = when (phase(normalized)) {
        GiftFxPhase.Start -> 0
        GiftFxPhase.Buildup -> 1
        GiftFxPhase.Main -> 2
        GiftFxPhase.Finale -> 3
        GiftFxPhase.Cleanup -> 4
        GiftFxPhase.Ended -> 5
    }

    private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0).coerceAtLeast(1e-5f)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
