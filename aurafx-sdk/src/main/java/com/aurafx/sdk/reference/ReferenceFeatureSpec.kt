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
    val requiredLipLookNames: List<String> = listOf("Velvet", "Glossy Pop", "Lacquer", "Ombre")
    val requiredBrowNames: List<String> = listOf(
        "Bold Arch", "Natural", "Feathered", "Flat", "Soft Curve", "Angled", "Full Definition",
    )
    val requiredEyeshadowNames: List<String> = listOf(
        "Goldie", "Cat Eye", "Classic", "Glam", "Nude", "Smokey",
    )
    val requiredEyelinerCoreNames: List<String> = listOf(
        "Flick", "None", "Bold", "Graphic", "Winged", "Retro",
    )
    val requiredLashNames: List<String> = listOf(
        "Natural Curl", "Soft Volume", "Lifted", "Defined", "Doll Eyes", "Full Fan",
    )
    val requiredLensNames: List<String> = listOf(
        "Pure Tone", "Golden Glint", "Sapphire Ink", "Warm Glint", "Kiwi Pop", "Silver Mist",
        "Amber Glow", "Blue Dew",
    )
    val requiredBodyNames: List<String> = listOf("Hips", "Waist")
    val requiredLightingNames: List<String> = listOf("Day Light", "Neon Light", "Theatrical Light")
    val requiredMaskNames: List<String> = listOf(
        "Desert Sun", "Solar Flare", "Burning Man",
        "Aqua Grace",
        "Golden Link", "Sketchy Beat", "Catwoman", "Spring Cat", "Black Cat", "Snow Kitty",
        "Meow Mode", "Easter Hop", "Fluffy Hop", "Spring Hop", "Party Hop", "Foxy Mode",
        "Groovy Cat", "Party Bear",
        "India Glow", "Pride Paint", "Ultra Fan", "Goal Rush", "Bad Santa", "England", "France",
        "Patriot Pop", "Freedom Fun", "Mega Mason", "Summer Vibes", "Shell Belle", "Moonlit Glow",
        "Haji Glow", "PSG Fever", "Arsenal Vibe", "Starman", "Rock King", "Disco Diva",
        "Fire Princess", "Island Cheers", "Farm Crush", "Spring Aura", "Floral Fantasy",
        "Blush Pop", "Rave", "Hot Devil", "Pumpkin Doll", "Deadly Bloom", "Sweet Feels",
        "T-Rex", "Love Lens", "Retro",
        "Brainy", "Showstopper", "Lucky Charm", "Get Lucky", "Emerald Veil", "Daisy Daze",
        "Desert Lace", "Winter Bloom", "Masked Meow", "Hippie",
    )
    val requiredBackgroundNames: List<String> = listOf(
        "Aurora View", "Sun Drift", "Rainy Street",
        "Cosmic Light",
        "Cherry Pop", "Love Clouds", "Neon Clouds", "Neon Sweet",
        "Blur", "Rainy Blur",
        "Neon Room", "Neon Pattern", "Rhythm", "Golden Pattern", "Comics Kiss", "Stage Lights", "Pink Swirls",
        "City Sunset", "Ocean Sunset",
        "Cartoonish Leopard", "Animal Love", "Snake Skin",
        "Forest", "Jungle", "Desert", "Sea",
        "Sunset Beach",
        "White Canopy", "Wardrobe", "Canopy Bed Interior", "Gothic Interior", "Luxurious Bathroom",
        "Neon Lounge", "Romantic Velvet Corner", "Satin Bed with Rose Petals", "Blue Armchair",
    )

    val requiredMakeupPresets: List<String> = listOf("Classic", "Bright", "Extravagant")

    fun makeupEnumsPresent(): Boolean =
        MakeupPreset.entries.size == 3 &&
            BlushStyle.entries.size == 4 &&
            LipLook.entries.size >= 4 &&
            BrowStyle.entries.size == 7 &&
            EyelinerStyle.entries.size >= 6 &&
            LashStyle.entries.size == 6 &&
            LensStyle.entries.size >= 8

    fun blushLabel(style: BlushStyle): String = when (style) {
        BlushStyle.SoftTouch -> "Soft Touch"
        BlushStyle.Airbrush -> "Airbrush"
        BlushStyle.BlushBomb -> "Blush Bomb"
        BlushStyle.SunKissed -> "Sun-Kissed"
    }

    fun lipLookLabel(look: LipLook): String = when (look) {
        LipLook.Velvet -> "Velvet"
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
        BrowStyle.FullDefinition -> "Full Definition"
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
        LensStyle.AmberGlow -> "Amber Glow"
        LensStyle.BlueDew -> "Blue Dew"
    }

    fun eyeshadowLabel(style: com.aurafx.sdk.api.EyeshadowStyle): String = when (style) {
        com.aurafx.sdk.api.EyeshadowStyle.Goldie -> "Goldie"
        com.aurafx.sdk.api.EyeshadowStyle.CatEye -> "Cat Eye"
        com.aurafx.sdk.api.EyeshadowStyle.Classic -> "Classic"
        com.aurafx.sdk.api.EyeshadowStyle.Glam -> "Glam"
        com.aurafx.sdk.api.EyeshadowStyle.Nude -> "Nude"
        com.aurafx.sdk.api.EyeshadowStyle.Smokey -> "Smokey"
    }

    fun duplicateIds(): List<String> {
        val ids = FilterCatalog.filters.map { it.id } +
            AREffectCatalog.effects.map { it.id } +
            BackgroundCatalog.items.map { it.id }
        return ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.toList()
    }
}
