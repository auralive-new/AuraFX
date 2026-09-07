package com.aurafx.sdk.ar

import com.aurafx.sdk.api.ARCategory
import com.aurafx.sdk.api.AROcclusionMode

data class ARAnchorDefinition(
    val id: String,
    val landmarkHint: String,
) {
    fun valid(): Boolean = id.isNotBlank() && landmarkHint.isNotBlank()
}

data class ARAssetDefinition(
    val id: String,
    val kind: String,
    val procedural: Boolean,
) {
    fun valid(): Boolean = id.isNotBlank() && kind.isNotBlank() && procedural
}

data class AREffectDefinition(
    val id: String,
    val displayName: String,
    val categories: Set<ARCategory>,
    val look: Int,
    val assets: List<ARAssetDefinition>,
    val anchors: List<ARAnchorDefinition>,
    val animation: ARAnimationDefinition,
    val occlusion: AROcclusionMode,
    val intensityMin: Float = 0f,
    val intensityMax: Float = 1f,
    val defaultIntensity: Float = 0.85f,
    val productionRendered: Boolean,
    val hairOcclude: Float,
    val needsFace: Boolean = true,
)

object AREffectCatalog {
    val effects: List<AREffectDefinition> = listOf(
        def(
            "ar.desert_sun", "Desert Sun", 0,
            setOf(ARCategory.FaceAccessory, ARCategory.Particle, ARCategory.Environment, ARCategory.Animated),
            assets("halo", "dust"),
            anchors("forehead", "cheeks"),
            ARAnimationDefinition(true, 28f, 8, "none", false),
            AROcclusionMode.HairOccludes, 0.72f,
        ),
        def(
            "ar.purrfect_match", "Purrfect Match", 1,
            setOf(ARCategory.FaceMask, ARCategory.FaceAccessory, ARCategory.EyeEffect, ARCategory.Animated),
            assets("ears", "whiskers", "eye-glow"),
            anchors("forehead", "cheeks", "iris"),
            ARAnimationDefinition(true, 6f, 8, "browRaise", true),
            AROcclusionMode.HairOccludes, 0.85f,
        ),
        def(
            "ar.black_cat", "Black Cat", 2,
            setOf(ARCategory.FaceMask, ARCategory.EyeEffect, ARCategory.Particle, ARCategory.Animated),
            assets("ears", "whiskers", "eye-glow"),
            anchors("forehead", "iris", "cheeks"),
            ARAnimationDefinition(true, 10f, 8, "smile", true),
            AROcclusionMode.HairOccludes, 0.85f,
        ),
        def(
            "ar.party_hop", "Party Hop", 3,
            setOf(ARCategory.FaceAccessory, ARCategory.Particle, ARCategory.Animated, ARCategory.Environment),
            assets("glasses", "confetti"),
            anchors("eyes", "forehead"),
            ARAnimationDefinition(true, 42f, 7, "smile", true),
            AROcclusionMode.HairOccludes, 0.4f,
        ),
        def(
            "ar.pride_paint", "Pride Paint", 4,
            setOf(ARCategory.FacePaint, ARCategory.Particle, ARCategory.Animated),
            assets("face-paint", "sparkle"),
            anchors("cheeks", "forehead", "faceOval"),
            ARAnimationDefinition(true, 18f, 6, "smile", false),
            AROcclusionMode.PersonOnly, 0.15f,
        ),
        def(
            "ar.summer_vibes", "Summer Vibes", 5,
            setOf(ARCategory.FaceAccessory, ARCategory.Environment, ARCategory.Particle),
            assets("glasses", "dust"),
            anchors("eyes", "forehead"),
            ARAnimationDefinition(true, 16f, 9, "none", false),
            AROcclusionMode.HairOccludes, 0.55f,
        ),
        def(
            "ar.red_hero", "Red Hero", 6,
            setOf(ARCategory.FacePaint, ARCategory.EyeEffect, ARCategory.Particle, ARCategory.Animated),
            assets("face-paint", "eye-glow", "energy"),
            anchors("forehead", "iris", "faceOval"),
            ARAnimationDefinition(true, 22f, 8, "mouthOpen", false),
            AROcclusionMode.HairOccludes, 0.5f,
        ),
        def(
            "ar.blush_pop", "Blush Pop", 7,
            setOf(ARCategory.FacePaint, ARCategory.Particle, ARCategory.Animated),
            assets("face-paint", "hearts"),
            anchors("cheeks", "mouth"),
            ARAnimationDefinition(true, 24f, 5, "smile", false),
            AROcclusionMode.PersonOnly, 0.2f,
        ),
        def(
            "ar.cupid", "Cupid", 8,
            setOf(ARCategory.FaceAccessory, ARCategory.Particle, ARCategory.Animated, ARCategory.EyeEffect),
            assets("halo", "hearts"),
            anchors("forehead", "mouth", "iris"),
            ARAnimationDefinition(true, 36f, 5, "smile", true),
            AROcclusionMode.HairOccludes, 0.65f,
        ),
        def(
            "ar.moonlit_glow", "Moonlit Glow", 9,
            setOf(ARCategory.FaceAccessory, ARCategory.Environment, ARCategory.Particle, ARCategory.EyeEffect),
            assets("moon", "sparkle"),
            anchors("forehead", "iris"),
            ARAnimationDefinition(true, 20f, 6, "none", false),
            AROcclusionMode.HairOccludes, 0.7f,
        ),
    )

    val byId = effects.associateBy { it.id }

    fun require(id: String) = byId[id]

    fun validate(): List<String> {
        val e = ArrayList<String>()
        val ids = HashSet<String>()
        for (fx in effects) {
            if (!ids.add(fx.id)) e += "dup ${fx.id}"
            if (!fx.productionRendered) e += "${fx.id} not rendered"
            if (fx.assets.isEmpty() || fx.assets.any { !it.valid() }) e += "${fx.id} assets"
            if (fx.anchors.isEmpty() || fx.anchors.any { !it.valid() }) e += "${fx.id} anchors"
            if (!fx.animation.valid()) e += "${fx.id} anim"
            if (fx.intensityMin != 0f || fx.intensityMax != 1f) e += "${fx.id} intensity range"
            if (fx.look !in 0..9) e += "${fx.id} look"
            if (fx.categories.isEmpty()) e += "${fx.id} cats"
        }
        return e
    }

    private fun def(
        id: String,
        name: String,
        look: Int,
        cats: Set<ARCategory>,
        assets: List<ARAssetDefinition>,
        anchors: List<ARAnchorDefinition>,
        anim: ARAnimationDefinition,
        occ: AROcclusionMode,
        hair: Float,
    ) = AREffectDefinition(
        id, name, cats, look, assets, anchors, anim, occ,
        productionRendered = true, hairOcclude = hair,
    )

    private fun assets(vararg kinds: String) =
        kinds.map { ARAssetDefinition("asset.$it", it, procedural = true) }

    private fun anchors(vararg ids: String) =
        ids.map { ARAnchorDefinition(it, it) }
}
