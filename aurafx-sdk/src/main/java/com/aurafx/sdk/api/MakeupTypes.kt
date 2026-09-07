package com.aurafx.sdk.api

data class MakeupColor(
    var r: Float = 0.8f,
    var g: Float = 0.6f,
    var b: Float = 0.5f,
) {
    fun clampInPlace() {
        r = clamp01(r)
        g = clamp01(g)
        b = clamp01(b)
    }

    fun set(rr: Float, gg: Float, bb: Float) {
        r = rr
        g = gg
        b = bb
        clampInPlace()
    }
}

enum class MakeupPreset { Classic, Bright, Extravagant }

enum class BlushStyle { SoftTouch, Airbrush, BlushBomb, SunKissed }

enum class BrowStyle { BoldArch, Natural, Feathered, Flat, SoftCurve, Angled, FullDefinition }

enum class EyelinerStyle {
    CatEye, Classic, Glam, Smokey, Goldie, Flick, None, Bold, Retro, Graphic, Winged,
}

enum class LashStyle { NaturalCurl, SoftVolume, Lifted, Defined, DollEyes, FullFan }

enum class LipLook { Velvet, GlossyPop, Lacquer, Ombre }

enum class EyeshadowStyle { Goldie, CatEye, Classic, Glam, Nude, Smokey }

enum class LensStyle {
    PureTone, GoldenGlint, SapphireInk, WarmGlint, KiwiPop, SilverMist, AmberGlow, BlueDew,
}
