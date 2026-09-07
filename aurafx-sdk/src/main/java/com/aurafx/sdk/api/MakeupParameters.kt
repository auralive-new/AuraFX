package com.aurafx.sdk.api

data class FoundationMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var coverage: Float = 0.45f,
    var color: MakeupColor = MakeupColor(0.86f, 0.72f, 0.62f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        coverage = clamp01(coverage)
        color.clampInPlace()
        if (!enabled) intensity = 0f
    }

    operator fun invoke(block: FoundationMakeup.() -> Unit) {
        block()
        clampInPlace()
    }
}

data class ConcealerMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var color: MakeupColor = MakeupColor(0.90f, 0.78f, 0.70f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        color.clampInPlace()
        if (!enabled) intensity = 0f
    }

    operator fun invoke(block: ConcealerMakeup.() -> Unit) {
        block()
        clampInPlace()
    }
}

data class BlushMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var style: BlushStyle = BlushStyle.SoftTouch,
    var color: MakeupColor = MakeupColor(0.89f, 0.42f, 0.45f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        color.clampInPlace()
        if (!enabled) intensity = 0f
    }
    operator fun invoke(block: BlushMakeup.() -> Unit) { block(); clampInPlace() }
}

data class ContourMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var color: MakeupColor = MakeupColor(0.42f, 0.28f, 0.22f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        color.clampInPlace()
        if (!enabled) intensity = 0f
    }
}

data class HighlightMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var color: MakeupColor = MakeupColor(1f, 0.95f, 0.88f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        color.clampInPlace()
        if (!enabled) intensity = 0f
    }
}

data class EyebrowMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var style: BrowStyle = BrowStyle.Natural,
    var color: MakeupColor = MakeupColor(0.22f, 0.14f, 0.10f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        color.clampInPlace()
        if (!enabled) intensity = 0f
    }
}

data class EyeshadowMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var style: EyeshadowStyle = EyeshadowStyle.Classic,
    var lidColor: MakeupColor = MakeupColor(0.55f, 0.38f, 0.42f),
    var creaseColor: MakeupColor = MakeupColor(0.32f, 0.18f, 0.22f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        applyStylePalette()
        lidColor.clampInPlace()
        creaseColor.clampInPlace()
        if (!enabled) intensity = 0f
    }

    private fun applyStylePalette() {
        when (style) {
            EyeshadowStyle.Goldie -> {
                lidColor.set(0.88f, 0.68f, 0.22f); creaseColor.set(0.55f, 0.32f, 0.08f)
            }
            EyeshadowStyle.CatEye -> {
                lidColor.set(0.18f, 0.12f, 0.14f); creaseColor.set(0.06f, 0.04f, 0.05f)
            }
            EyeshadowStyle.Classic -> {
                lidColor.set(0.55f, 0.38f, 0.42f); creaseColor.set(0.32f, 0.18f, 0.22f)
            }
            EyeshadowStyle.Glam -> {
                lidColor.set(0.62f, 0.28f, 0.55f); creaseColor.set(0.28f, 0.08f, 0.32f)
            }
            EyeshadowStyle.Nude -> {
                lidColor.set(0.78f, 0.62f, 0.52f); creaseColor.set(0.52f, 0.36f, 0.28f)
            }
            EyeshadowStyle.Smokey -> {
                lidColor.set(0.28f, 0.26f, 0.30f); creaseColor.set(0.08f, 0.07f, 0.10f)
            }
        }
    }
}

data class EyelinerMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var style: EyelinerStyle = EyelinerStyle.Classic,
    var color: MakeupColor = MakeupColor(0.05f, 0.04f, 0.04f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        color.clampInPlace()
        if (!enabled || style == EyelinerStyle.None) intensity = 0f
    }
}

data class EyelashMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var style: LashStyle = LashStyle.NaturalCurl,
    var color: MakeupColor = MakeupColor(0.04f, 0.03f, 0.03f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        color.clampInPlace()
        if (!enabled) intensity = 0f
    }
}

data class LipstickMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var opacity: Float = 0.75f,
    var color: MakeupColor = MakeupColor(0.72f, 0.16f, 0.22f),
    var look: LipLook = LipLook.GlossyPop,
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        opacity = clamp01(opacity)
        color.clampInPlace()
        if (!enabled) intensity = 0f
    }
}

data class LipLinerMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var color: MakeupColor = MakeupColor(0.55f, 0.12f, 0.16f),
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        color.clampInPlace()
        if (!enabled) intensity = 0f
    }
}

data class LipGlossMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        if (!enabled) intensity = 0f
    }
}

data class LensMakeup(
    var enabled: Boolean = false,
    var intensity: Float = 0f,
    var style: LensStyle = LensStyle.PureTone,
) {
    fun clampInPlace() {
        intensity = clamp01(intensity)
        if (!enabled) intensity = 0f
    }
}

data class MakeupParameters(
    var foundation: FoundationMakeup = FoundationMakeup(),
    var concealer: ConcealerMakeup = ConcealerMakeup(),
    var blush: BlushMakeup = BlushMakeup(),
    var contour: ContourMakeup = ContourMakeup(),
    var highlight: HighlightMakeup = HighlightMakeup(),
    var eyebrow: EyebrowMakeup = EyebrowMakeup(),
    var eyeshadow: EyeshadowMakeup = EyeshadowMakeup(),
    var eyeliner: EyelinerMakeup = EyelinerMakeup(),
    var eyelashes: EyelashMakeup = EyelashMakeup(),
    var lipstick: LipstickMakeup = LipstickMakeup(),
    var lipLiner: LipLinerMakeup = LipLinerMakeup(),
    var lipGloss: LipGlossMakeup = LipGlossMakeup(),
    var lens: LensMakeup = LensMakeup(),
) {
    fun clampInPlace() {
        foundation.clampInPlace()
        concealer.clampInPlace()
        blush.clampInPlace()
        contour.clampInPlace()
        highlight.clampInPlace()
        eyebrow.clampInPlace()
        eyeshadow.clampInPlace()
        eyeliner.clampInPlace()
        eyelashes.clampInPlace()
        lipstick.clampInPlace()
        lipLiner.clampInPlace()
        lipGloss.clampInPlace()
        lens.clampInPlace()
    }

    fun reset() {
        foundation = FoundationMakeup()
        concealer = ConcealerMakeup()
        blush = BlushMakeup()
        contour = ContourMakeup()
        highlight = HighlightMakeup()
        eyebrow = EyebrowMakeup()
        eyeshadow = EyeshadowMakeup()
        eyeliner = EyelinerMakeup()
        eyelashes = EyelashMakeup()
        lipstick = LipstickMakeup()
        lipLiner = LipLinerMakeup()
        lipGloss = LipGlossMakeup()
        lens = LensMakeup()
    }

    fun isIdentity(): Boolean =
        foundation.intensity == 0f && concealer.intensity == 0f && blush.intensity == 0f &&
            contour.intensity == 0f && highlight.intensity == 0f && eyebrow.intensity == 0f &&
            eyeshadow.intensity == 0f && eyeliner.intensity == 0f && eyelashes.intensity == 0f &&
            lipstick.intensity == 0f && lipLiner.intensity == 0f && lipGloss.intensity == 0f &&
            lens.intensity == 0f

    fun applyPreset(preset: MakeupPreset) {
        reset()
        when (preset) {
            MakeupPreset.Classic -> {
                foundation.enabled = true; foundation.intensity = 0.35f; foundation.coverage = 0.4f
                concealer.enabled = true; concealer.intensity = 0.4f
                blush.enabled = true; blush.intensity = 0.38f; blush.style = BlushStyle.SoftTouch
                blush.color.set(0.86f, 0.40f, 0.44f)
                contour.enabled = true; contour.intensity = 0.22f
                highlight.enabled = true; highlight.intensity = 0.28f
                eyebrow.enabled = true; eyebrow.intensity = 0.45f; eyebrow.style = BrowStyle.Natural
                eyeshadow.enabled = true; eyeshadow.intensity = 0.32f; eyeshadow.style = EyeshadowStyle.Classic
                eyeliner.enabled = true; eyeliner.intensity = 0.55f; eyeliner.style = EyelinerStyle.Classic
                eyelashes.enabled = true; eyelashes.intensity = 0.4f; eyelashes.style = LashStyle.NaturalCurl
                lipstick.enabled = true; lipstick.intensity = 0.55f; lipstick.opacity = 0.7f
                lipstick.color.set(0.70f, 0.18f, 0.24f)
                lipstick.look = LipLook.GlossyPop
                lipLiner.enabled = true; lipLiner.intensity = 0.4f
                lipGloss.enabled = true; lipGloss.intensity = 0.25f
            }
            MakeupPreset.Bright -> {
                foundation.enabled = true; foundation.intensity = 0.42f; foundation.coverage = 0.5f
                concealer.enabled = true; concealer.intensity = 0.55f
                blush.enabled = true; blush.intensity = 0.55f; blush.style = BlushStyle.BlushBomb
                blush.color.set(0.95f, 0.38f, 0.48f)
                contour.enabled = true; contour.intensity = 0.18f
                highlight.enabled = true; highlight.intensity = 0.45f
                eyebrow.enabled = true; eyebrow.intensity = 0.4f; eyebrow.style = BrowStyle.SoftCurve
                eyeshadow.enabled = true; eyeshadow.intensity = 0.5f; eyeshadow.style = EyeshadowStyle.Glam
                eyeliner.enabled = true; eyeliner.intensity = 0.45f; eyeliner.style = EyelinerStyle.Flick
                eyelashes.enabled = true; eyelashes.intensity = 0.55f; eyelashes.style = LashStyle.Lifted
                lipstick.enabled = true; lipstick.intensity = 0.62f
                lipstick.color.set(0.92f, 0.28f, 0.40f)
                lipstick.look = LipLook.Ombre
                lipGloss.enabled = true; lipGloss.intensity = 0.55f
                lens.enabled = true; lens.intensity = 0.25f; lens.style = LensStyle.WarmGlint
            }
            MakeupPreset.Extravagant -> {
                foundation.enabled = true; foundation.intensity = 0.5f; foundation.coverage = 0.62f
                concealer.enabled = true; concealer.intensity = 0.6f
                blush.enabled = true; blush.intensity = 0.48f; blush.style = BlushStyle.Airbrush
                contour.enabled = true; contour.intensity = 0.48f
                highlight.enabled = true; highlight.intensity = 0.58f
                eyebrow.enabled = true; eyebrow.intensity = 0.7f; eyebrow.style = BrowStyle.BoldArch
                eyeshadow.enabled = true; eyeshadow.intensity = 0.72f; eyeshadow.style = EyeshadowStyle.Smokey
                eyeliner.enabled = true; eyeliner.intensity = 0.85f; eyeliner.style = EyelinerStyle.CatEye
                eyelashes.enabled = true; eyelashes.intensity = 0.85f; eyelashes.style = LashStyle.FullFan
                lipstick.enabled = true; lipstick.intensity = 0.8f; lipstick.opacity = 0.88f
                lipstick.color.set(0.45f, 0.04f, 0.10f)
                lipstick.look = LipLook.Lacquer
                lipLiner.enabled = true; lipLiner.intensity = 0.7f
                lipGloss.enabled = true; lipGloss.intensity = 0.7f
                lens.enabled = true; lens.intensity = 0.45f; lens.style = LensStyle.SapphireInk
            }
        }
        clampInPlace()
    }
}
