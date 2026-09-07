package com.aurafx.sdk.reference

import com.aurafx.sdk.ar.AREffectCatalog
import com.aurafx.sdk.filter.FilterCatalog
import com.aurafx.sdk.scene.BackgroundCatalog

/**
 * Tango parity is blocked until recordings/icon sheets exist in the workspace.
 * Cited strings below are task starting evidence only. They are not a frame-by-frame
 * inventory and must not be treated as Tango totals.
 */
object TangoReferenceGate {
    const val SOURCE_AVAILABLE = false
    const val TANGO_COMPLETE = false
    const val STATUS = "NEEDS SOURCE MATERIAL"

    val missingSourceKinds: List<String> = listOf(
        "Tango screen recordings (.mp4/.mov/.webm/.mkv)",
        "Filter / AR / background icon sheets or tray screenshots",
        "LUT packs or Tango asset bundles",
        "AuraLive / Tango APK UI dumps",
    )

    /** Previously cited filter tray labels. Not frame-verified in this run. */
    val citedFilterCategories: List<String> = listOf(
        "360°", "LIVE", "GLOW", "PATTERNS", "BLUR", "TANGO STYLE",
        "ANIME", "ANIMAL PRINT", "NATURE", "SCENERY", "ROOMS",
    )

    val citedFilterExamples: List<String> = listOf(
        "Rainy Street", "Neon Clouds", "Neon Pattern", "City Sunset",
        "Desert", "Canopy Bed Interior", "Day Light",
    )

    val citedArAuraFxExamples: List<String> = listOf(
        "Desert Sun", "Purrfect Match", "Black Cat", "Party Hop", "Pride Paint",
        "Summer Vibes", "Red Hero", "Blush Pop", "Cupid", "Moonlit Glow",
    )

    fun tangoFilterTotal(): Int? = null
    fun tangoArTotal(): Int? = null
    fun tangoBackgroundTotal(): Int? = null

    fun auraFxFilterTotal(): Int = FilterCatalog.filters.size
    fun auraFxArTotal(): Int = AREffectCatalog.effects.size
    fun auraFxBackgroundTotal(): Int = BackgroundCatalog.items.size

    fun missingFilters(): Int? = null
    fun missingAr(): Int? = null
    fun missingBackgrounds(): Int? = null

    fun citedFilterExamplesPresentInAuraFx(): List<String> {
        val names = FilterCatalog.filters.map { it.displayName.lowercase() }.toSet()
        return citedFilterExamples.filter { names.contains(it.lowercase()) }
    }
}
