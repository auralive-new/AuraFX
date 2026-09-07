package com.aurafx.sdk.api

enum class FilterCategory {
    Natural,
    Warm,
    Cool,
    Glow,
    Soft,
    Portrait,
    Vibe,
    Mood,
    Classic,
    Lut,
    Orbit360,
    Live,
    Patterns,
    Blur,
    Signature,
    Anime,
    AnimalPrint,
    Nature,
    Scenery,
    Rooms,
}

fun FilterCategory.trayLabel(): String = when (this) {
    FilterCategory.Orbit360 -> "360°"
    FilterCategory.Live -> "LIVE"
    FilterCategory.Glow -> "GLOW"
    FilterCategory.Patterns -> "PATTERNS"
    FilterCategory.Blur -> "BLUR"
    FilterCategory.Signature -> "SIGNATURE"
    FilterCategory.Anime -> "ANIME"
    FilterCategory.AnimalPrint -> "ANIMAL PRINT"
    FilterCategory.Nature -> "NATURE"
    FilterCategory.Scenery -> "SCENERY"
    FilterCategory.Rooms -> "ROOMS"
    else -> name.uppercase()
}

enum class FilterFinish {
    None,
    Grain,
    Vignette,
    GrainVignette,
}

/**
 * Public filter session state. One primary color filter plus an optional
 * compatible finish (grain/vignette only). Two LUTs are never stacked.
 */
data class FilterParameters(
    var id: String? = null,
    var intensity: Float = 1f,
    var finish: FilterFinish = FilterFinish.None,
    var finishIntensity: Float = 0f,
) {
    fun clampInPlace() {
        intensity = intensity.finiteOr(0f).coerceIn(0f, 1f)
        finishIntensity = finishIntensity.finiteOr(0f).coerceIn(0f, 1f)
        if (id.isNullOrBlank()) id = null
        if (finish == FilterFinish.None) finishIntensity = 0f
    }

    fun isIdentity(): Boolean = id == null || intensity <= 0f

    fun reset() {
        id = null
        intensity = 1f
        finish = FilterFinish.None
        finishIntensity = 0f
    }

    operator fun invoke(block: FilterParameters.() -> Unit) {
        block()
        clampInPlace()
    }
}

internal fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback
