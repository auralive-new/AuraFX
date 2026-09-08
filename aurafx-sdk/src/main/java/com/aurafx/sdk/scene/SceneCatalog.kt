package com.aurafx.sdk.scene

import com.aurafx.sdk.api.BackgroundTray
import com.aurafx.sdk.api.BackgroundType
import com.aurafx.sdk.api.HairColorId
import com.aurafx.sdk.api.HairStyleCapability
import com.aurafx.sdk.api.LightingMode

data class BackgroundDefinition(
    val id: String,
    val displayName: String,
    val type: BackgroundType,
    val shaderMode: Int,
    val colorA: FloatArray,
    val colorB: FloatArray,
    val tray: BackgroundTray = BackgroundTray.Studio,
    val compatibleWithFilters: Boolean = true,
    val imageAsset: String? = null,
)

data class HairStyleDefinition(
    val id: String,
    val displayName: String,
    val capability: HairStyleCapability,
    val productionRealistic: Boolean,
    val renderingStrategy: String,
    val anchors: List<String>,
    val requiredAsset: String?,
)

data class HairColorDefinition(
    val id: HairColorId,
    val displayName: String,
    val rgb: FloatArray,
)

data class BodyTransformDefinition(
    val id: String,
    val displayName: String,
    val region: String,
    val parameter: String,
    val maxAmount: Float,
)

object BackgroundCatalog {
    val items: List<BackgroundDefinition> = listOf(
        bg("bg.blur.soft", "Soft Blur", BackgroundType.Blur, 0, c(0.2f), c(0.1f)),
        bg("bg.blur.strong", "Strong Blur", BackgroundType.Blur, 1, c(0.2f), c(0.1f)),
        bg("bg.solid.black", "Black", BackgroundType.Solid, 2, c(0.02f), c(0.02f)),
        bg("bg.solid.white", "White", BackgroundType.Solid, 2, c(0.94f), c(0.94f)),
        bg("bg.gradient.studio_gray", "Studio Gray", BackgroundType.Gradient, 3, floatArrayOf(0.55f, 0.56f, 0.58f), floatArrayOf(0.22f, 0.22f, 0.24f)),
        bg("bg.gradient.studio_warm", "Studio Warm", BackgroundType.Gradient, 3, floatArrayOf(0.72f, 0.58f, 0.42f), floatArrayOf(0.28f, 0.16f, 0.10f)),
        bg("bg.gradient.studio_cool", "Studio Cool", BackgroundType.Gradient, 3, floatArrayOf(0.42f, 0.52f, 0.68f), floatArrayOf(0.10f, 0.14f, 0.22f)),
        bg("bg.gradient.sunset", "Sunset", BackgroundType.Gradient, 3, floatArrayOf(0.95f, 0.45f, 0.22f), floatArrayOf(0.25f, 0.08f, 0.28f)),
        bg("bg.env.cyc_white", "Cyclorama", BackgroundType.ProceduralEnvironment, 4, c(0.9f), floatArrayOf(0.7f, 0.72f, 0.74f)),
        bg("bg.env.bokeh_warm", "Warm Bokeh", BackgroundType.ProceduralEnvironment, 5, floatArrayOf(0.9f, 0.55f, 0.25f), floatArrayOf(0.2f, 0.08f, 0.04f)),
        bg("bg.env.bokeh_cool", "Cool Bokeh", BackgroundType.ProceduralEnvironment, 5, floatArrayOf(0.35f, 0.55f, 0.9f), floatArrayOf(0.04f, 0.06f, 0.12f)),
        bg("bg.env.sky_dusk", "Dusk Sky", BackgroundType.ProceduralEnvironment, 6, floatArrayOf(0.95f, 0.55f, 0.35f), floatArrayOf(0.12f, 0.14f, 0.35f)),
        bg("bg.env.sky_noon", "Noon Sky", BackgroundType.ProceduralEnvironment, 6, floatArrayOf(0.55f, 0.72f, 0.95f), floatArrayOf(0.75f, 0.85f, 0.98f)),
        bg("bg.env.office_window", "Office Window", BackgroundType.ProceduralEnvironment, 7, floatArrayOf(0.85f, 0.88f, 0.92f), floatArrayOf(0.35f, 0.38f, 0.42f)),
        bg("bg.env.night_city", "Night City", BackgroundType.ProceduralEnvironment, 8, floatArrayOf(0.95f, 0.85f, 0.45f), floatArrayOf(0.04f, 0.05f, 0.08f)),
        bg("bg.env.forest_bokeh", "Forest Bokeh", BackgroundType.ProceduralEnvironment, 5, floatArrayOf(0.35f, 0.55f, 0.22f), floatArrayOf(0.06f, 0.10f, 0.04f)),
        bg("bg.env.beach_haze", "Beach Haze", BackgroundType.ProceduralEnvironment, 6, floatArrayOf(0.85f, 0.78f, 0.55f), floatArrayOf(0.45f, 0.62f, 0.78f)),
        bg("bg.env.studio_blue", "Studio Blue", BackgroundType.Gradient, 3, floatArrayOf(0.15f, 0.28f, 0.55f), floatArrayOf(0.04f, 0.08f, 0.16f)),
        bg("bg.env.spotlight", "Spotlight Falloff", BackgroundType.ProceduralEnvironment, 9, c(0.9f), c(0.05f)),
        bg("bg.env.paper_warm", "Warm Paper", BackgroundType.ProceduralEnvironment, 4, floatArrayOf(0.86f, 0.78f, 0.62f), floatArrayOf(0.62f, 0.52f, 0.38f)),

        bg("bg.orbit.aurora_view", "Aurora View", BackgroundType.ProceduralEnvironment, 10, floatArrayOf(0.25f, 0.95f, 0.55f), floatArrayOf(0.08f, 0.12f, 0.35f), BackgroundTray.Orbit360),
        bg("bg.orbit.sun_drift", "Sun Drift", BackgroundType.ProceduralEnvironment, 11, floatArrayOf(1.0f, 0.72f, 0.28f), floatArrayOf(0.35f, 0.22f, 0.45f), BackgroundTray.Orbit360),
        bg("bg.orbit.rainy_street", "Rainy Street", BackgroundType.ProceduralEnvironment, 12, floatArrayOf(0.45f, 0.52f, 0.62f), floatArrayOf(0.08f, 0.09f, 0.12f), BackgroundTray.Orbit360),

        bg("bg.glow.cosmic_light", "Cosmic Light", BackgroundType.ProceduralEnvironment, 13, floatArrayOf(0.65f, 0.35f, 1.0f), floatArrayOf(0.05f, 0.02f, 0.12f), BackgroundTray.Glow),

        bg("bg.pat.cherry_pop", "Cherry Pop", BackgroundType.ProceduralEnvironment, 14, floatArrayOf(0.95f, 0.22f, 0.42f), floatArrayOf(0.35f, 0.05f, 0.12f), BackgroundTray.Patterns),
        bg("bg.pat.love_clouds", "Love Clouds", BackgroundType.ProceduralEnvironment, 14, floatArrayOf(1.0f, 0.55f, 0.72f), floatArrayOf(0.55f, 0.35f, 0.55f), BackgroundTray.Patterns),
        bg("bg.pat.neon_clouds", "Neon Clouds", BackgroundType.ProceduralEnvironment, 14, floatArrayOf(0.35f, 0.85f, 1.0f), floatArrayOf(0.55f, 0.15f, 0.75f), BackgroundTray.Patterns),
        bg("bg.pat.neon_sweet", "Neon Sweet", BackgroundType.ProceduralEnvironment, 14, floatArrayOf(1.0f, 0.35f, 0.75f), floatArrayOf(0.25f, 0.08f, 0.28f), BackgroundTray.Patterns),

        bg("bg.blur.named", "Blur", BackgroundType.Blur, 0, c(0.22f), c(0.1f), BackgroundTray.Blur),
        bg("bg.blur.rainy", "Rainy Blur", BackgroundType.ProceduralEnvironment, 15, floatArrayOf(0.42f, 0.48f, 0.55f), floatArrayOf(0.1f, 0.12f, 0.16f), BackgroundTray.Blur),

        bg("bg.sig.neon_room", "Neon Room", BackgroundType.ProceduralEnvironment, 16, floatArrayOf(0.15f, 0.95f, 0.85f), floatArrayOf(0.08f, 0.05f, 0.18f), BackgroundTray.Signature),
        bg("bg.sig.neon_pattern", "Neon Pattern", BackgroundType.ProceduralEnvironment, 16, floatArrayOf(0.95f, 0.2f, 0.85f), floatArrayOf(0.05f, 0.08f, 0.22f), BackgroundTray.Signature),
        bg("bg.sig.rhythm", "Rhythm", BackgroundType.ProceduralEnvironment, 16, floatArrayOf(0.95f, 0.55f, 0.12f), floatArrayOf(0.12f, 0.05f, 0.22f), BackgroundTray.Signature),
        bg("bg.sig.golden_pattern", "Golden Pattern", BackgroundType.ProceduralEnvironment, 16, floatArrayOf(0.95f, 0.78f, 0.22f), floatArrayOf(0.28f, 0.16f, 0.05f), BackgroundTray.Signature),
        bg("bg.sig.comics_kiss", "Comics Kiss", BackgroundType.ProceduralEnvironment, 16, floatArrayOf(0.95f, 0.15f, 0.35f), floatArrayOf(0.15f, 0.05f, 0.22f), BackgroundTray.Signature),
        bg("bg.sig.stage_lights", "Stage Lights", BackgroundType.ProceduralEnvironment, 13, floatArrayOf(1.0f, 0.85f, 0.35f), floatArrayOf(0.05f, 0.04f, 0.08f), BackgroundTray.Signature),
        bg("bg.sig.pink_swirls", "Pink Swirls", BackgroundType.ProceduralEnvironment, 16, floatArrayOf(1.0f, 0.45f, 0.72f), floatArrayOf(0.35f, 0.08f, 0.28f), BackgroundTray.Signature),

        bg("bg.anime.city_sunset", "City Sunset", BackgroundType.ProceduralEnvironment, 6, floatArrayOf(0.98f, 0.42f, 0.22f), floatArrayOf(0.18f, 0.08f, 0.32f), BackgroundTray.Anime),
        bg("bg.anime.ocean_sunset", "Ocean Sunset", BackgroundType.ProceduralEnvironment, 6, floatArrayOf(0.98f, 0.55f, 0.28f), floatArrayOf(0.08f, 0.18f, 0.38f), BackgroundTray.Anime),

        bg("bg.animal.cartoonish_leopard", "Cartoonish Leopard", BackgroundType.ProceduralEnvironment, 17, floatArrayOf(0.85f, 0.62f, 0.28f), floatArrayOf(0.22f, 0.12f, 0.06f), BackgroundTray.AnimalPrint),
        bg("bg.animal.animal_love", "Animal Love", BackgroundType.ProceduralEnvironment, 17, floatArrayOf(0.92f, 0.45f, 0.35f), floatArrayOf(0.28f, 0.12f, 0.1f), BackgroundTray.AnimalPrint),
        bg("bg.animal.snake_skin", "Snake Skin", BackgroundType.ProceduralEnvironment, 17, floatArrayOf(0.35f, 0.55f, 0.28f), floatArrayOf(0.08f, 0.12f, 0.08f), BackgroundTray.AnimalPrint),

        bg("bg.nature.forest", "Forest", BackgroundType.ProceduralEnvironment, 18, floatArrayOf(0.28f, 0.55f, 0.18f), floatArrayOf(0.05f, 0.12f, 0.06f), BackgroundTray.Nature),
        bg("bg.nature.jungle", "Jungle", BackgroundType.ProceduralEnvironment, 18, floatArrayOf(0.18f, 0.48f, 0.16f), floatArrayOf(0.04f, 0.1f, 0.05f), BackgroundTray.Nature),
        bg("bg.nature.desert", "Desert", BackgroundType.ProceduralEnvironment, 18, floatArrayOf(0.92f, 0.68f, 0.32f), floatArrayOf(0.45f, 0.28f, 0.12f), BackgroundTray.Nature),
        bg("bg.nature.sea", "Sea", BackgroundType.ProceduralEnvironment, 18, floatArrayOf(0.22f, 0.62f, 0.78f), floatArrayOf(0.04f, 0.12f, 0.28f), BackgroundTray.Nature),

        bg("bg.scenery.sunset_beach", "Sunset Beach", BackgroundType.ProceduralEnvironment, 6, floatArrayOf(0.98f, 0.52f, 0.28f), floatArrayOf(0.22f, 0.28f, 0.55f), BackgroundTray.Scenery),

        bg("bg.rooms.white_canopy", "White Canopy", BackgroundType.ProceduralEnvironment, 19, c(0.92f), floatArrayOf(0.72f, 0.74f, 0.78f), BackgroundTray.Rooms),
        bg("bg.rooms.wardrobe", "Wardrobe", BackgroundType.ProceduralEnvironment, 19, floatArrayOf(0.42f, 0.28f, 0.18f), floatArrayOf(0.12f, 0.08f, 0.06f), BackgroundTray.Rooms),
        bg("bg.rooms.canopy_bed_interior", "Canopy Bed Interior", BackgroundType.ProceduralEnvironment, 19, floatArrayOf(0.85f, 0.55f, 0.72f), floatArrayOf(0.18f, 0.1f, 0.28f), BackgroundTray.Rooms),
        bg("bg.rooms.gothic_interior", "Gothic Interior", BackgroundType.ProceduralEnvironment, 19, floatArrayOf(0.35f, 0.12f, 0.18f), floatArrayOf(0.05f, 0.03f, 0.06f), BackgroundTray.Rooms),
        bg("bg.rooms.luxurious_bathroom", "Luxurious Bathroom", BackgroundType.ProceduralEnvironment, 19, floatArrayOf(0.78f, 0.85f, 0.88f), floatArrayOf(0.35f, 0.42f, 0.48f), BackgroundTray.Rooms),
        bg("bg.rooms.neon_lounge", "Neon Lounge", BackgroundType.ProceduralEnvironment, 16, floatArrayOf(0.35f, 0.95f, 0.85f), floatArrayOf(0.08f, 0.04f, 0.16f), BackgroundTray.Rooms),
        bg("bg.rooms.romantic_velvet_corner", "Romantic Velvet Corner", BackgroundType.ProceduralEnvironment, 19, floatArrayOf(0.55f, 0.08f, 0.18f), floatArrayOf(0.12f, 0.03f, 0.06f), BackgroundTray.Rooms),
        bg("bg.rooms.satin_bed_rose_petals", "Satin Bed with Rose Petals", BackgroundType.ProceduralEnvironment, 19, floatArrayOf(0.92f, 0.45f, 0.55f), floatArrayOf(0.35f, 0.12f, 0.18f), BackgroundTray.Rooms),
        bg("bg.rooms.blue_armchair", "Blue Armchair", BackgroundType.ProceduralEnvironment, 19, floatArrayOf(0.22f, 0.38f, 0.72f), floatArrayOf(0.08f, 0.1f, 0.22f), BackgroundTray.Rooms),
    )
    val byId = items.associateBy { it.id }
    fun require(id: String) = byId[id]
    fun validate(): List<String> {
        val e = ArrayList<String>()
        if (items.size < 20) e += "need 20 backgrounds"
        val ids = HashSet<String>()
        for (it in items) {
            if (!ids.add(it.id)) e += "dup ${it.id}"
            if (it.type == BackgroundType.Image && it.imageAsset.isNullOrBlank()) {
                e += "${it.id} image without asset"
            }
            if (it.colorA.size != 3 || it.colorB.size != 3) e += "${it.id} colors"
        }
        return e
    }
    fun inTray(tray: BackgroundTray) = items.filter { it.tray == tray }

    private fun bg(
        id: String,
        name: String,
        type: BackgroundType,
        mode: Int,
        a: FloatArray,
        b: FloatArray,
        tray: BackgroundTray = BackgroundTray.Studio,
    ) = BackgroundDefinition(id, name, type, mode, a, b, tray)
    private fun c(v: Float) = floatArrayOf(v, v, v)
}

object HairCatalog {
    val colors: List<HairColorDefinition> = listOf(
        HairColorDefinition(HairColorId.Black, "Black", floatArrayOf(0.06f, 0.05f, 0.05f)),
        HairColorDefinition(HairColorId.Brown, "Brown", floatArrayOf(0.28f, 0.16f, 0.08f)),
        HairColorDefinition(HairColorId.DarkBrown, "Dark Brown", floatArrayOf(0.16f, 0.09f, 0.05f)),
        HairColorDefinition(HairColorId.LightBrown, "Light Brown", floatArrayOf(0.45f, 0.30f, 0.16f)),
        HairColorDefinition(HairColorId.Blonde, "Blonde", floatArrayOf(0.78f, 0.66f, 0.38f)),
        HairColorDefinition(HairColorId.Platinum, "Platinum", floatArrayOf(0.86f, 0.84f, 0.80f)),
        HairColorDefinition(HairColorId.Red, "Red", floatArrayOf(0.62f, 0.14f, 0.10f)),
        HairColorDefinition(HairColorId.Auburn, "Auburn", floatArrayOf(0.52f, 0.22f, 0.12f)),
        HairColorDefinition(HairColorId.Pink, "Pink", floatArrayOf(0.85f, 0.35f, 0.55f)),
        HairColorDefinition(HairColorId.Purple, "Purple", floatArrayOf(0.42f, 0.18f, 0.62f)),
        HairColorDefinition(HairColorId.Blue, "Blue", floatArrayOf(0.18f, 0.32f, 0.72f)),
        HairColorDefinition(HairColorId.Custom, "Custom", floatArrayOf(0.35f, 0.18f, 0.08f)),
    )

    val styles: List<HairStyleDefinition> = listOf(
        HairStyleDefinition(
            "hair.style.natural", "Natural", HairStyleCapability.ProductionReady, true,
            "identity-own-hair",
            listOf("crown", "hairline", "temples"),
            requiredAsset = null,
        ),
        style("hair.style.bob", "Bob"),
        style("hair.style.pixie", "Pixie"),
        style("hair.style.long_layers", "Long Layers"),
        style("hair.style.bangs", "Bangs"),
        style("hair.style.ponytail", "Ponytail"),
        style("hair.style.bun", "Bun"),
        style("hair.style.braid", "Braid"),
        style("hair.style.curtain", "Curtain"),
        style("hair.style.wolf", "Wolf Cut"),
        style("hair.style.shag", "Shag"),
        style("hair.style.volume", "Crown Volume"),
        style("hair.style.asymmetric", "Asymmetric"),
    )

    fun colorRgb(id: HairColorId, custom: FloatArray? = null): FloatArray {
        if (id == HairColorId.Custom && custom != null && custom.size >= 3) return custom
        return colors.first { it.id == id }.rgb
    }

    fun style(id: String) = styles.firstOrNull { it.id == id }

    fun validate(): List<String> {
        val e = ArrayList<String>()
        if (styles.size < 13) e += "need 13 styles"
        if (colors.size < 12) e += "need 12 colors"
        if (styles.count { it.productionRealistic } != 13) e += "all 13 styles must be production-rendered"
        val ids = HashSet<String>()
        for (s in styles) {
            if (!ids.add(s.id)) e += "dup ${s.id}"
            if (s.id != "hair.style.natural") {
                if (s.requiredAsset.isNullOrBlank()) e += "${s.id} missing groom asset"
                if (s.capability != HairStyleCapability.ProductionReady) e += "${s.id} not production-ready"
            }
        }
        return e
    }

    private fun style(id: String, name: String) = HairStyleDefinition(
        id, name, HairStyleCapability.ProductionReady, true,
        "tracked-strand-mesh",
        listOf("crown", "hairline", "leftTemple", "rightTemple", "headPose", "ears"),
        requiredAsset = "hair/groom/$id.json",
    )
}

object BodyCatalog {
    val items: List<BodyTransformDefinition> = listOf(
        BodyTransformDefinition("body.slim", "Slim", "torso-hips", "slim", 0.35f),
        BodyTransformDefinition("body.waist", "Waist", "waist", "waist", 0.32f),
        BodyTransformDefinition("body.hips", "Hips", "hips", "hips", 0.34f),
        BodyTransformDefinition("body.shoulders", "Shoulders", "shoulders", "shoulders", 0.28f),
        BodyTransformDefinition("body.legs", "Legs", "thighs-calves", "legs", 0.28f),
        BodyTransformDefinition("body.arms", "Arms", "upper-arm", "arms", 0.18f),
        BodyTransformDefinition("body.torso", "Torso proportion", "torso-length", "torso", 0.22f),
    )
}

object LightingCatalog {
    val modes: List<LightingMode> = LightingMode.entries.toList()
}

object AuraFxEffectOrder {
    const val SEGMENTATION = "aurafx.scene.segmentation-upload"
    const val BACKGROUND = "aurafx.background"
    const val MAKEUP = "aurafx.makeup.professional"
    const val BEAUTY = "aurafx.beauty.skin-shape"
    const val HAIR = "aurafx.hair"
    const val BODY = "aurafx.body"
    const val LIGHTING = "aurafx.lighting"
    const val FILTER = "aurafx.filter.engine"
    const val AR = "aurafx.ar.effects"
    const val GIFT = "aurafx.gift.engine"

    val gpuIds: List<String> = listOf(
        SEGMENTATION, BACKGROUND, MAKEUP, BEAUTY, HAIR, BODY, LIGHTING, FILTER, AR, GIFT,
    )
}
