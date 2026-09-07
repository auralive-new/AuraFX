package com.aurafx.sdk.filter

import com.aurafx.sdk.api.FilterCategory

/**
 * Data-driven catalog. New looks are added here without changing the GPU core.
 */
object FilterCatalog {
    val filters: List<FilterDefinition> = listOf(
        // NATURAL
        def("natural.true", FilterCategory.Natural, "True", ColorGrade(
            exposure = 0.06f, contrast = 0.08f, saturation = 1.04f, temperature = 0.04f,
            shadows = 0.06f, highlights = -0.04f, skinProtect = 0.86f,
        )),
        def("natural.balanced", FilterCategory.Natural, "Balanced", ColorGrade(
            contrast = 0.12f, saturation = 1.06f, gamma = 0.97f, whites = 0.04f,
            blacks = -0.04f, skinProtect = 0.84f,
        )),
        def("natural.daylight", FilterCategory.Natural, "Daylight", ColorGrade(
            temperature = -0.06f, exposure = 0.08f, contrast = 0.06f, saturation = 1.03f,
            tint = -0.02f, skinProtect = 0.82f,
        )),
        def("natural.even", FilterCategory.Natural, "Even", ColorGrade(
            shadows = 0.10f, highlights = -0.08f, contrast = 0.04f, vibrance = 0.08f,
            selectiveSat = 0.92f, skinProtect = 0.90f,
        )),

        // WARM
        def("warm.golden", FilterCategory.Warm, "Golden", ColorGrade(
            temperature = 0.28f, tint = 0.04f, exposure = 0.06f, saturation = 1.08f,
            highlights = 0.06f, selectiveSat = 0.88f, skinProtect = 0.88f,
        )),
        def("warm.amber", FilterCategory.Warm, "Amber", ColorGrade(
            temperature = 0.36f, tint = 0.08f, contrast = 0.10f, saturation = 1.10f,
            shadows = 0.04f, balanceHighR = 0.12f, skinProtect = 0.90f,
        )),
        def("warm.honey", FilterCategory.Warm, "Honey", ColorGrade(
            temperature = 0.22f, gain = 0.06f, gamma = 0.96f, saturation = 1.05f,
            shadows = 0.08f, vignette = 0.12f, skinProtect = 0.86f,
        )),
        def("warm.sunset", FilterCategory.Warm, "Sunset", ColorGrade(
            temperature = 0.42f, tint = 0.10f, highlights = 0.12f, shadows = -0.06f,
            saturation = 1.14f, balanceHighR = 0.18f, balanceShadowB = 0.08f,
            vignette = 0.18f, skinProtect = 0.92f,
        )),

        // COOL
        def("cool.arctic", FilterCategory.Cool, "Arctic", ColorGrade(
            temperature = -0.32f, tint = -0.04f, contrast = 0.10f, saturation = 0.96f,
            highlights = 0.06f, whites = 0.06f, skinProtect = 0.88f,
        )),
        def("cool.steel", FilterCategory.Cool, "Steel", ColorGrade(
            temperature = -0.22f, contrast = 0.16f, saturation = 0.92f, blacks = -0.08f,
            balanceShadowB = 0.10f, skinProtect = 0.84f,
        )),
        def("cool.moonlight", FilterCategory.Cool, "Moonlight", ColorGrade(
            temperature = -0.38f, exposure = -0.08f, gamma = 1.06f, saturation = 0.90f,
            shadows = 0.10f, bloom = 0.08f, vignette = 0.22f, skinProtect = 0.86f,
        )),
        def("cool.cyan_shadow", FilterCategory.Cool, "Cyan Shadow", ColorGrade(
            temperature = -0.18f, balanceShadowB = 0.22f, contrast = 0.12f,
            saturation = 1.04f, highlights = -0.04f, skinProtect = 0.88f,
        )),

        // GLOW
        def("glow.pearl", FilterCategory.Glow, "Pearl", ColorGrade(
            bloom = 0.42f, highlights = 0.16f, whites = 0.08f, exposure = 0.08f,
            contrast = -0.04f, saturation = 1.02f, skinProtect = 0.80f, featureProtect = 0.88f,
        )),
        def("glow.halo", FilterCategory.Glow, "Halo", ColorGrade(
            bloom = 0.55f, exposure = 0.10f, highlights = 0.12f, gamma = 0.94f,
            grain = 0.04f, skinProtect = 0.82f,
        )),
        def("glow.backlight", FilterCategory.Glow, "Backlight", ColorGrade(
            bloom = 0.38f, whites = 0.12f, contrast = 0.08f, shadows = -0.06f,
            temperature = 0.08f, vignette = 0.10f, skinProtect = 0.84f,
        )),

        // SOFT
        def("soft.matte", FilterCategory.Soft, "Matte", ColorGrade(
            contrast = -0.18f, blacks = 0.12f, whites = -0.10f, highlights = -0.10f,
            saturation = 0.94f, grain = 0.06f, skinProtect = 0.88f,
        )),
        def("soft.haze", FilterCategory.Soft, "Haze", ColorGrade(
            lift = 0.08f, contrast = -0.12f, bloom = 0.16f, saturation = 0.90f,
            highlights = -0.06f, skinProtect = 0.86f,
        )),
        def("soft.pastel", FilterCategory.Soft, "Pastel", ColorGrade(
            saturation = 0.78f, contrast = -0.08f, lift = 0.05f, temperature = 0.06f,
            vibrance = 0.12f, skinProtect = 0.90f,
        )),

        // PORTRAIT
        def("portrait.studio", FilterCategory.Portrait, "Studio", ColorGrade(
            exposure = 0.10f, contrast = 0.14f, shadows = 0.08f, highlights = -0.06f,
            saturation = 1.04f, selectiveSat = 0.90f, skinProtect = 0.92f,
        )),
        def("portrait.rembrandt", FilterCategory.Portrait, "Rembrandt", ColorGrade(
            contrast = 0.22f, shadows = -0.16f, blacks = -0.10f, vignette = 0.28f,
            temperature = 0.12f, saturation = 0.98f, skinProtect = 0.90f,
        )),
        def("portrait.editorial", FilterCategory.Portrait, "Editorial", ColorGrade(
            contrast = 0.18f, saturation = 0.88f, highlights = 0.08f, whites = 0.06f,
            temperature = -0.06f, skinProtect = 0.88f,
        )),
        def("portrait.key", FilterCategory.Portrait, "Key Light", ColorGrade(
            exposure = 0.12f, highlights = 0.10f, bloom = 0.12f, shadows = 0.06f,
            selectiveSat = 0.86f, skinProtect = 0.94f, featureProtect = 0.90f,
        )),

        // VIBE
        def("vibe.punch", FilterCategory.Vibe, "Punch", ColorGrade(
            contrast = 0.28f, saturation = 1.28f, vibrance = 0.22f, blacks = -0.10f,
            whites = 0.08f, skinProtect = 0.78f,
        )),
        def("vibe.teal_orange", FilterCategory.Vibe, "Teal Orange", ColorGrade(
            temperature = 0.16f, balanceShadowB = 0.28f, balanceHighR = 0.24f,
            contrast = 0.16f, saturation = 1.16f, selectiveSat = 0.82f, skinProtect = 0.90f,
        )),
        def("vibe.neon", FilterCategory.Vibe, "Neon", ColorGrade(
            saturation = 1.36f, vibrance = 0.28f, contrast = 0.12f, temperature = -0.10f,
            highlights = 0.10f, bloom = 0.10f, skinProtect = 0.80f,
        )),
        def("vibe.urban", FilterCategory.Vibe, "Urban", ColorGrade(
            contrast = 0.20f, saturation = 1.10f, temperature = -0.08f, blacks = -0.12f,
            grain = 0.12f, vignette = 0.16f, skinProtect = 0.82f,
        )),

        // MOOD
        def("mood.noir", FilterCategory.Mood, "Noir", ColorGrade(
            saturation = 0.18f, contrast = 0.32f, blacks = -0.16f, vignette = 0.36f,
            grain = 0.14f, gamma = 1.08f, skinProtect = 0.70f,
        )),
        def("mood.dusk", FilterCategory.Mood, "Dusk", ColorGrade(
            temperature = 0.18f, exposure = -0.12f, shadows = 0.08f, saturation = 0.92f,
            balanceShadowB = 0.16f, vignette = 0.24f, skinProtect = 0.86f,
        )),
        def("mood.fog", FilterCategory.Mood, "Fog", ColorGrade(
            lift = 0.10f, contrast = -0.16f, saturation = 0.72f, bloom = 0.14f,
            temperature = -0.08f, skinProtect = 0.84f,
        )),
        def("mood.ember", FilterCategory.Mood, "Ember", ColorGrade(
            temperature = 0.34f, shadows = -0.10f, contrast = 0.14f, saturation = 1.12f,
            balanceHighR = 0.16f, vignette = 0.20f, grain = 0.08f, skinProtect = 0.88f,
        )),

        // CLASSIC
        def("classic.print", FilterCategory.Classic, "Print", ColorGrade(
            contrast = 0.14f, saturation = 1.08f, temperature = 0.10f, gamma = 0.96f,
            shadows = 0.04f, skinProtect = 0.84f,
        )),
        def("classic.chrome", FilterCategory.Classic, "Chrome", ColorGrade(
            contrast = 0.26f, saturation = 1.22f, highlights = 0.10f,
            whites = 0.08f, blacks = -0.08f, temperature = -0.04f, skinProtect = 0.80f,
        )),
        def("classic.fade", FilterCategory.Classic, "Fade", ColorGrade(
            contrast = -0.10f, lift = 0.07f, saturation = 0.82f, highlights = -0.08f,
            temperature = 0.08f, grain = 0.10f, skinProtect = 0.86f,
        )),
        def("classic.silver", FilterCategory.Classic, "Silver", ColorGrade(
            saturation = 0.06f, contrast = 0.18f, highlights = 0.08f, blacks = -0.08f,
            grain = 0.10f, vignette = 0.14f, skinProtect = 0.74f,
        )),

        // LUT — GPU samples a baked 3D LUT (same grade lattice, real transform)
        lut("lut.film_warm", "Film Warm", ColorGrade(
            temperature = 0.24f, contrast = 0.12f, saturation = 1.10f, gamma = 0.94f,
            shadows = 0.06f, grain = 0.08f, skinProtect = 0.86f,
        )),
        lut("lut.film_cool", "Film Cool", ColorGrade(
            temperature = -0.22f, contrast = 0.14f, saturation = 0.96f, gamma = 0.95f,
            highlights = 0.06f, skinProtect = 0.86f,
        )),
        lut("lut.cross_process", "Cross Process", ColorGrade(
            contrast = 0.22f, saturation = 1.24f, balanceHighR = -0.12f, balanceHighB = 0.18f,
            balanceShadowR = 0.10f, temperature = -0.08f, skinProtect = 0.76f,
        )),
        lut("lut.contrast_s", "Contrast S", ColorGrade(
            contrast = 0.34f, blacks = -0.12f, whites = 0.10f, gamma = 0.92f,
            saturation = 1.06f, skinProtect = 0.80f,
        )),
        lut("lut.split_tone", "Split Tone", ColorGrade(
            balanceHighR = 0.22f, balanceShadowB = 0.22f, contrast = 0.12f,
            saturation = 1.08f, vignette = 0.12f, skinProtect = 0.88f,
        )),

        scene("orbit.wrap", FilterCategory.Orbit360, "Orbit Wrap", ColorGrade(
            contrast = 0.16f, saturation = 1.08f, vignette = 0.42f, bloom = 0.10f,
            temperature = 0.08f, skinProtect = 0.82f,
        ), sceneMode = 6),

        scene("live.day_light", FilterCategory.Live, "Day Light", ColorGrade(
            exposure = 0.14f, contrast = 0.08f, saturation = 1.06f, temperature = 0.06f,
            shadows = 0.08f, highlights = -0.04f, skinProtect = 0.86f,
        ), sceneMode = 0),
        scene("live.neon_clouds", FilterCategory.Live, "Neon Clouds", ColorGrade(
            saturation = 1.22f, bloom = 0.36f, temperature = -0.12f, contrast = 0.08f,
            highlights = 0.10f, skinProtect = 0.80f,
        ), sceneMode = 2),

        scene("patterns.neon_pattern", FilterCategory.Patterns, "Neon Pattern", ColorGrade(
            saturation = 1.18f, contrast = 0.12f, temperature = -0.08f, bloom = 0.12f,
            skinProtect = 0.78f,
        ), sceneMode = 3),

        scene("blur.dream", FilterCategory.Blur, "Dream Blur", ColorGrade(
            contrast = -0.14f, bloom = 0.28f, lift = 0.06f, saturation = 0.94f,
            highlights = -0.04f, skinProtect = 0.84f,
        ), sceneMode = 0),

        scene("signature.prime", FilterCategory.Signature, "Prime", ColorGrade(
            contrast = 0.18f, saturation = 1.12f, temperature = 0.14f, vignette = 0.16f,
            shadows = 0.06f, grain = 0.06f, skinProtect = 0.88f,
        ), sceneMode = 0),

        scene("anime.cel", FilterCategory.Anime, "Cel Shade", ColorGrade(
            contrast = 0.28f, saturation = 1.20f, highlights = 0.10f, shadows = -0.08f,
            skinProtect = 0.74f,
        ), sceneMode = 7),

        scene("animal.leopard", FilterCategory.AnimalPrint, "Leopard", ColorGrade(
            contrast = 0.10f, saturation = 1.08f, temperature = 0.16f, skinProtect = 0.92f,
        ), sceneMode = 8),

        scene("nature.desert", FilterCategory.Nature, "Desert", ColorGrade(
            temperature = 0.38f, saturation = 1.10f, highlights = 0.10f, shadows = -0.06f,
            vignette = 0.14f, skinProtect = 0.90f,
        ), sceneMode = 4),

        scene("scenery.rainy_street", FilterCategory.Scenery, "Rainy Street", ColorGrade(
            temperature = -0.22f, contrast = 0.12f, saturation = 0.88f, lift = 0.04f,
            vignette = 0.18f, grain = 0.08f, skinProtect = 0.84f,
        ), sceneMode = 1),
        scene("scenery.city_sunset", FilterCategory.Scenery, "City Sunset", ColorGrade(
            temperature = 0.40f, tint = 0.08f, contrast = 0.14f, saturation = 1.16f,
            highlights = 0.10f, vignette = 0.20f, skinProtect = 0.90f,
        ), sceneMode = 0),

        scene("rooms.canopy_bed", FilterCategory.Rooms, "Canopy Bed Interior", ColorGrade(
            temperature = 0.22f, exposure = -0.06f, shadows = 0.12f, vignette = 0.32f,
            saturation = 0.96f, bloom = 0.08f, skinProtect = 0.86f,
        ), sceneMode = 5),
    )

    val byId: Map<String, FilterDefinition> = filters.associateBy { it.id }

    fun categories(): List<FilterCategory> = FilterCategory.entries.toList()

    fun inCategory(category: FilterCategory): List<FilterDefinition> =
        filters.filter { it.category == category }

    fun require(id: String): FilterDefinition? = byId[id]

    fun validate(): List<String> {
        val errors = ArrayList<String>()
        val ids = HashSet<String>()
        for (f in filters) {
            if (!ids.add(f.id)) errors += "duplicate id ${f.id}"
            if (f.id.isBlank()) errors += "blank id"
            if (f.displayName.isBlank()) errors += "blank name ${f.id}"
            if (f.intensityMin != 0f) errors += "${f.id} intensityMin must be 0"
            if (f.intensityMax != 1f) errors += "${f.id} intensityMax must be 1"
            if (f.defaultIntensity !in 0f..1f) errors += "${f.id} defaultIntensity"
            if (!f.grade.allFinite()) errors += "${f.id} non-finite grade"
            if (f.grade.isIdentity()) errors += "${f.id} identity grade (placeholder)"
            val lut = f.bakedLut().validate()
            if (lut is LutValidation.Invalid) errors += "${f.id} LUT ${lut.reason}"
            if (lut is LutValidation.Ok && lut.identityDelta < 0.01f) {
                errors += "${f.id} LUT is effectively identity"
            }
        }
        for (cat in FilterCategory.entries) {
            if (inCategory(cat).isEmpty()) errors += "empty category $cat"
        }
        return errors
    }

    private fun def(
        id: String,
        category: FilterCategory,
        name: String,
        grade: ColorGrade,
    ) = FilterDefinition(id, category, name, grade.clamp(), usesLut = false, sceneMode = 0)

    private fun scene(
        id: String,
        category: FilterCategory,
        name: String,
        grade: ColorGrade,
        sceneMode: Int,
    ) = FilterDefinition(id, category, name, grade.clamp(), usesLut = false, sceneMode = sceneMode)

    private fun lut(id: String, name: String, grade: ColorGrade) =
        FilterDefinition(id, FilterCategory.Lut, name, grade.clamp(), usesLut = true, sceneMode = 0)
}
