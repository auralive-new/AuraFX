package com.aurafx.sdk.api

enum class BackgroundType {
    Blur,
    Solid,
    Gradient,
    ProceduralEnvironment,
    Image,
}

enum class HairColorId {
    Black, Brown, DarkBrown, LightBrown, Blonde, Platinum, Red, Auburn, Pink, Purple, Blue, Custom,
}

enum class HairStyleCapability {
    ProductionReady,
    RequiresGroomAsset,
}

enum class LightingMode {
    Soft, Directional, Warm, Cool, Natural, DayLight, NeonLight, TheatricalLight,
}

fun LightingMode.label(): String = when (this) {
    LightingMode.DayLight -> "Day Light"
    LightingMode.NeonLight -> "Neon Light"
    LightingMode.TheatricalLight -> "Theatrical Light"
    else -> name
}

enum class BackgroundTray {
    Studio,
    Orbit360,
    Glow,
    Patterns,
    Blur,
    Signature,
    Anime,
    AnimalPrint,
    Nature,
    Scenery,
    Rooms,
}

fun BackgroundTray.trayLabel(): String = when (this) {
    BackgroundTray.Orbit360 -> "360°"
    BackgroundTray.Glow -> "GLOW"
    BackgroundTray.Patterns -> "PATTERNS"
    BackgroundTray.Blur -> "BLUR"
    BackgroundTray.Signature -> "SIGNATURE"
    BackgroundTray.Anime -> "ANIME"
    BackgroundTray.AnimalPrint -> "ANIMAL PRINT"
    BackgroundTray.Nature -> "NATURE"
    BackgroundTray.Scenery -> "SCENERY"
    BackgroundTray.Rooms -> "ROOMS"
    BackgroundTray.Studio -> "STUDIO"
}

data class BackgroundParameters(
    var enabled: Boolean = false,
    var id: String? = null,
    var intensity: Float = 0.7f,
) {
    fun clampInPlace() {
        intensity = clamp01(intensity.finiteOr(0f))
        if (id.isNullOrBlank()) id = null
        if (!enabled) {
            // keep id so toggling enable restores, but identity when disabled
        }
    }

    fun isIdentity(): Boolean = !enabled || id == null || intensity <= 0f

    fun reset() {
        enabled = false
        id = null
        intensity = 0.7f
    }

    operator fun invoke(block: BackgroundParameters.() -> Unit) {
        block(); clampInPlace()
    }
}

data class HairParameters(
    var enabled: Boolean = false,
    var color: HairColorId = HairColorId.Brown,
    var intensity: Float = 0.55f,
    var styleId: String = "hair.style.natural",
    var customR: Float = 0.35f,
    var customG: Float = 0.18f,
    var customB: Float = 0.08f,
) {
    fun clampInPlace() {
        intensity = clamp01(intensity.finiteOr(0f))
        customR = clamp01(customR.finiteOr(0f))
        customG = clamp01(customG.finiteOr(0f))
        customB = clamp01(customB.finiteOr(0f))
        if (styleId.isBlank()) styleId = "hair.style.natural"
    }

    fun wantsStyle(): Boolean = enabled && styleId.isNotBlank() && styleId != "hair.style.natural"

    fun wantsColor(): Boolean = enabled && intensity > 0f

    fun isIdentity(): Boolean = !enabled || (!wantsStyle() && intensity <= 0f)

    fun reset() {
        enabled = false
        color = HairColorId.Brown
        intensity = 0.55f
        styleId = "hair.style.natural"
        customR = 0.35f
        customG = 0.18f
        customB = 0.08f
    }

    operator fun invoke(block: HairParameters.() -> Unit) {
        block(); clampInPlace()
    }
}

data class BodyParameters(
    var enabled: Boolean = false,
    var slim: Float = 0f,
    var waist: Float = 0f,
    var hips: Float = 0f,
    var shoulders: Float = 0f,
    var legs: Float = 0f,
    var arms: Float = 0f,
    var torso: Float = 0f,
) {
    fun clampInPlace() {
        slim = clamp01(slim.finiteOr(0f))
        waist = clamp01(waist.finiteOr(0f))
        hips = clamp01(hips.finiteOr(0f))
        shoulders = clamp01(shoulders.finiteOr(0f))
        legs = clamp01(legs.finiteOr(0f))
        arms = clamp01(arms.finiteOr(0f))
        torso = clamp01(torso.finiteOr(0f))
    }

    fun isIdentity(): Boolean =
        !enabled || (slim == 0f && waist == 0f && hips == 0f && shoulders == 0f &&
            legs == 0f && arms == 0f && torso == 0f)

    fun reset() {
        enabled = false
        slim = 0f
        waist = 0f
        hips = 0f
        shoulders = 0f
        legs = 0f
        arms = 0f
        torso = 0f
    }

    operator fun invoke(block: BodyParameters.() -> Unit) {
        block(); clampInPlace()
    }
}

data class LightingParameters(
    var enabled: Boolean = false,
    var mode: LightingMode = LightingMode.Soft,
    var intensity: Float = 0.45f,
    var brightness: Float = 0.12f,
    var warmth: Float = 0f,
    var shadowLift: Float = 0.35f,
    var highlightControl: Float = 0.2f,
    var azimuth: Float = 0.15f,
) {
    fun clampInPlace() {
        intensity = clamp01(intensity.finiteOr(0f))
        brightness = clamp01(brightness.finiteOr(0f))
        warmth = warmth.finiteOr(0f).coerceIn(-1f, 1f)
        shadowLift = clamp01(shadowLift.finiteOr(0f))
        highlightControl = clamp01(highlightControl.finiteOr(0f))
        azimuth = azimuth.finiteOr(0f).coerceIn(-1f, 1f)
    }

    fun isIdentity(): Boolean = !enabled || intensity <= 0f

    fun reset() {
        enabled = false
        mode = LightingMode.Soft
        intensity = 0.45f
        brightness = 0.12f
        warmth = 0f
        shadowLift = 0.35f
        highlightControl = 0.2f
        azimuth = 0.15f
    }

    operator fun invoke(block: LightingParameters.() -> Unit) {
        block(); clampInPlace()
    }
}
