package com.aurafx.sdk.reference

import com.aurafx.sdk.api.BlushStyle
import com.aurafx.sdk.api.BrowStyle
import com.aurafx.sdk.api.EyelinerStyle
import com.aurafx.sdk.api.FilterCategory
import com.aurafx.sdk.api.LashStyle
import com.aurafx.sdk.api.LensStyle
import com.aurafx.sdk.api.LipLook
import com.aurafx.sdk.api.MakeupPreset
import com.aurafx.sdk.api.trayLabel
import com.aurafx.sdk.ar.AREffectCatalog
import com.aurafx.sdk.filter.FilterCatalog
import com.aurafx.sdk.scene.BackgroundCatalog

/**
 * Required inventory from the reviewed specification. Original AuraFX names only.
 */
object ReferenceFeatureSpec {
    val requiredFilterTrayLabels: List<String> = listOf(
        "360°", "LIVE", "GLOW", "PATTERNS", "BLUR", "SIGNATURE",
        "ANIME", "ANIMAL PRINT", "NATURE", "SCENERY", "ROOMS",
    )

    val requiredFilterDisplayNames: List<String> = listOf(
        "Rainy Street", "Neon Clouds", "Neon Pattern", "City Sunset",
        "Desert", "Canopy Bed Interior", "Day Light",
    )

    val requiredBeautyKeys: List<String> = listOf(
        "fineSmooth", "toothWhiten", "whiten", "ruddy", "vFace", "cheekThin",
        "cheekSmall", "cheekNarrow", "nose", "eyeEnlarge", "eyeDistance", "mouth", "circles",
    )

    val requiredArDisplayNames: List<String> = listOf(
        "Desert Sun", "Purrfect Match", "Black Cat", "Party Hop", "Pride Paint",
        "Summer Vibes", "Red Hero", "Blush Pop", "Cupid", "Moonlit Glow",
    )

    fun missingRequiredFilters(): List<String> {
        val names = FilterCatalog.filters.map { it.displayName }.toSet()
        return requiredFilterDisplayNames.filter { it !in names }
    }

    fun missingRequiredTrays(): List<String> {
        val labels = FilterCategory.entries.map { it.trayLabel() }.toSet()
        return requiredFilterTrayLabels.filter { it !in labels }
    }

    fun filterCount(): Int = FilterCatalog.filters.size
    fun arCount(): Int = AREffectCatalog.effects.size
    fun backgroundCount(): Int = BackgroundCatalog.items.size

    val requiredBlushNames: List<String> = listOf("Soft Touch", "Airbrush", "Blush Bomb", "Sun-Kissed")
    val requiredLipLookNames: List<String> = listOf("Glossy Pop", "Lacquer", "Ombre")
    val requiredBrowNames: List<String> = listOf(
        "Bold Arch", "Natural", "Feathered", "Flat", "Soft Curve", "Angled", "Full",
    )
    val requiredEyelinerNames: List<String> = listOf(
        "Cat Eye", "Classic", "Glam", "Smokey", "Goldie", "Flick", "None", "Bold", "Retro", "Graphic", "Winged",
    )
    val requiredLashNames: List<String> = listOf(
        "Natural Curl", "Soft Volume", "Lifted", "Defined", "Doll Eyes", "Full Fan",
    )
    val requiredLensNames: List<String> = listOf(
        "Pure Tone", "Golden Glint", "Sapphire Ink", "Warm Glint", "Kiwi Pop", "Silver Mist",
    )
    val requiredMakeupPresets: List<String> = listOf("Classic", "Bright", "Extravagant")

    fun makeupEnumsPresent(): Boolean =
        MakeupPreset.entries.size == 3 &&
            BlushStyle.entries.size == 4 &&
            LipLook.entries.size == 3 &&
            BrowStyle.entries.size == 7 &&
            EyelinerStyle.entries.size == 11 &&
            LashStyle.entries.size == 6 &&
            LensStyle.entries.size == 6

    fun blushLabel(style: BlushStyle): String = when (style) {
        BlushStyle.SoftTouch -> "Soft Touch"
        BlushStyle.Airbrush -> "Airbrush"
        BlushStyle.BlushBomb -> "Blush Bomb"
        BlushStyle.SunKissed -> "Sun-Kissed"
    }

    fun lipLookLabel(look: LipLook): String = when (look) {
        LipLook.GlossyPop -> "Glossy Pop"
        LipLook.Lacquer -> "Lacquer"
        LipLook.Ombre -> "Ombre"
    }

    fun browLabel(style: BrowStyle): String = when (style) {
        BrowStyle.BoldArch -> "Bold Arch"
        BrowStyle.Natural -> "Natural"
        BrowStyle.Feathered -> "Feathered"
        BrowStyle.Flat -> "Flat"
        BrowStyle.SoftCurve -> "Soft Curve"
        BrowStyle.Angled -> "Angled"
        BrowStyle.Full -> "Full"
    }

    fun eyelinerLabel(style: EyelinerStyle): String = when (style) {
        EyelinerStyle.CatEye -> "Cat Eye"
        EyelinerStyle.Classic -> "Classic"
        EyelinerStyle.Glam -> "Glam"
        EyelinerStyle.Smokey -> "Smokey"
        EyelinerStyle.Goldie -> "Goldie"
        EyelinerStyle.Flick -> "Flick"
        EyelinerStyle.None -> "None"
        EyelinerStyle.Bold -> "Bold"
        EyelinerStyle.Retro -> "Retro"
        EyelinerStyle.Graphic -> "Graphic"
        EyelinerStyle.Winged -> "Winged"
    }

    fun lashLabel(style: LashStyle): String = when (style) {
        LashStyle.NaturalCurl -> "Natural Curl"
        LashStyle.SoftVolume -> "Soft Volume"
        LashStyle.Lifted -> "Lifted"
        LashStyle.Defined -> "Defined"
        LashStyle.DollEyes -> "Doll Eyes"
        LashStyle.FullFan -> "Full Fan"
    }

    fun lensLabel(style: LensStyle): String = when (style) {
        LensStyle.PureTone -> "Pure Tone"
        LensStyle.GoldenGlint -> "Golden Glint"
        LensStyle.SapphireInk -> "Sapphire Ink"
        LensStyle.WarmGlint -> "Warm Glint"
        LensStyle.KiwiPop -> "Kiwi Pop"
        LensStyle.SilverMist -> "Silver Mist"
    }

    fun duplicateIds(): List<String> {
        val ids = FilterCatalog.filters.map { it.id } +
            AREffectCatalog.effects.map { it.id } +
            BackgroundCatalog.items.map { it.id }
        return ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.toList()
    }
}
