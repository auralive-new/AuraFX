package com.aurafx.sdk.ar

import com.aurafx.sdk.api.ARCategory
import com.aurafx.sdk.api.AROcclusionMode
import com.aurafx.sdk.api.MaskTray

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
    val family: Int,
    val tray: MaskTray,
    val paint: FloatArray,
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
    val effects: List<AREffectDefinition> = buildList {
        var look = 0
        fun item(
            id: String,
            name: String,
            tray: MaskTray,
            family: Int,
            cats: Set<ARCategory>,
            kinds: Array<String>,
            anchors: Array<String>,
            expr: String,
            occ: AROcclusionMode,
            hair: Float,
            emit: Float,
            r: Float,
            g: Float,
            b: Float,
            pulse: Boolean = true,
            pKind: Int = 6,
        ) {
            add(
                AREffectDefinition(
                    id = id,
                    displayName = name,
                    categories = cats,
                    look = look++,
                    family = family,
                    tray = tray,
                    paint = floatArrayOf(r, g, b),
                    assets = kinds.map { ARAssetDefinition("asset.$it", it, true) },
                    anchors = anchors.map { ARAnchorDefinition(it, it) },
                    animation = ARAnimationDefinition(true, emit, pKind, expr, pulse),
                    occlusion = occ,
                    productionRendered = true,
                    hairOcclude = hair,
                ),
            )
        }

        val acc = setOf(ARCategory.FaceAccessory, ARCategory.Particle, ARCategory.Animated)
        val ears = setOf(ARCategory.FaceMask, ARCategory.FaceAccessory, ARCategory.EyeEffect, ARCategory.Animated)
        val paint = setOf(ARCategory.FacePaint, ARCategory.Particle, ARCategory.Animated)
        val env = setOf(ARCategory.FaceAccessory, ARCategory.Environment, ARCategory.Particle, ARCategory.Animated)
        val eye = setOf(ARCategory.EyeEffect, ARCategory.Particle, ARCategory.Animated)

        item("ar.desert_sun", "Desert Sun", MaskTray.New, 0, env, arrayOf("halo", "dust"), arrayOf("forehead", "cheeks"), "none", AROcclusionMode.HairOccludes, 0.72f, 28f, 1f, 0.62f, 0.18f)
        item("ar.purrfect_match", "Purrfect Match", MaskTray.Original, 1, ears, arrayOf("ears", "whiskers", "eye-glow"), arrayOf("forehead", "cheeks", "iris"), "browRaise", AROcclusionMode.HairOccludes, 0.85f, 6f, 0.82f, 0.55f, 0.32f)
        item("ar.black_cat", "Black Cat", MaskTray.Ears, 1, ears, arrayOf("ears", "whiskers", "eye-glow"), arrayOf("forehead", "iris", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.85f, 10f, 0.08f, 0.07f, 0.09f)
        item("ar.party_hop", "Party Hop", MaskTray.Ears, 2, acc + ARCategory.Environment, arrayOf("glasses", "confetti"), arrayOf("eyes", "forehead"), "smile", AROcclusionMode.HairOccludes, 0.4f, 42f, 0.75f, 0.25f, 0.85f, pKind = 7)
        item("ar.pride_paint", "Pride Paint", MaskTray.Trendy, 3, paint, arrayOf("face-paint", "sparkle"), arrayOf("cheeks", "forehead", "faceOval"), "smile", AROcclusionMode.PersonOnly, 0.15f, 18f, 0.9f, 0.2f, 0.7f)
        item("ar.summer_vibes", "Summer Vibes", MaskTray.Trendy, 2, env, arrayOf("glasses", "dust"), arrayOf("eyes", "forehead"), "none", AROcclusionMode.HairOccludes, 0.55f, 16f, 0.95f, 0.82f, 0.35f)
        item("ar.red_hero", "Red Hero", MaskTray.Original, 3, paint + ARCategory.EyeEffect, arrayOf("face-paint", "eye-glow", "energy"), arrayOf("forehead", "iris", "faceOval"), "mouthOpen", AROcclusionMode.HairOccludes, 0.5f, 22f, 0.75f, 0.12f, 0.12f)
        item("ar.blush_pop", "Blush Pop", MaskTray.Trendy, 3, paint, arrayOf("face-paint", "hearts"), arrayOf("cheeks", "mouth"), "smile", AROcclusionMode.PersonOnly, 0.2f, 24f, 1f, 0.45f, 0.55f, pKind = 5)
        item("ar.cupid", "Cupid", MaskTray.Original, 0, acc + ARCategory.EyeEffect, arrayOf("halo", "hearts"), arrayOf("forehead", "mouth", "iris"), "smile", AROcclusionMode.HairOccludes, 0.65f, 36f, 1f, 0.45f, 0.55f, pKind = 5)
        item("ar.moonlit_glow", "Moonlit Glow", MaskTray.Trendy, 4, env + ARCategory.EyeEffect, arrayOf("moon", "sparkle"), arrayOf("forehead", "iris"), "none", AROcclusionMode.HairOccludes, 0.7f, 20f, 0.45f, 0.55f, 0.9f)

        item("ar.solar_flare", "Solar Flare", MaskTray.New, 0, env, arrayOf("halo", "sparkle"), arrayOf("forehead", "iris"), "none", AROcclusionMode.HairOccludes, 0.6f, 32f, 1f, 0.45f, 0.08f)
        item("ar.burning_man", "Burning Man", MaskTray.New, 0, env + ARCategory.FacePaint, arrayOf("halo", "dust"), arrayOf("forehead", "faceOval"), "mouthOpen", AROcclusionMode.HairOccludes, 0.55f, 26f, 0.95f, 0.28f, 0.05f)

        item("ar.aqua_grace", "Aqua Grace", MaskTray.Earrings, 5, acc, arrayOf("sparkle"), arrayOf("cheeks", "chin"), "smile", AROcclusionMode.HairOccludes, 0.35f, 12f, 0.25f, 0.72f, 0.85f)

        item("ar.golden_link", "Golden Link", MaskTray.Ears, 1, ears, arrayOf("ears", "whiskers"), arrayOf("forehead", "cheeks"), "browRaise", AROcclusionMode.HairOccludes, 0.8f, 8f, 0.92f, 0.72f, 0.22f)
        item("ar.sketchy_beat", "Sketchy Beat", MaskTray.Ears, 1, ears, arrayOf("ears", "whiskers"), arrayOf("forehead", "iris"), "smile", AROcclusionMode.HairOccludes, 0.75f, 14f, 0.12f, 0.12f, 0.14f)
        item("ar.catwoman", "Catwoman", MaskTray.Ears, 1, ears, arrayOf("ears", "whiskers", "eye-glow"), arrayOf("forehead", "iris", "cheeks"), "browRaise", AROcclusionMode.HairOccludes, 0.88f, 10f, 0.05f, 0.05f, 0.07f)
        item("ar.spring_cat", "Spring Cat", MaskTray.Ears, 1, ears, arrayOf("ears", "whiskers"), arrayOf("forehead", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.82f, 12f, 0.95f, 0.55f, 0.72f)
        item("ar.snow_kitty", "Snow Kitty", MaskTray.Ears, 1, ears, arrayOf("ears", "whiskers"), arrayOf("forehead", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.8f, 8f, 0.92f, 0.94f, 0.98f)
        item("ar.meow_mode", "Meow Mode", MaskTray.Ears, 1, ears, arrayOf("ears", "whiskers", "eye-glow"), arrayOf("forehead", "iris"), "mouthOpen", AROcclusionMode.HairOccludes, 0.84f, 16f, 0.98f, 0.62f, 0.2f)
        item("ar.easter_hop", "Easter Hop", MaskTray.Ears, 1, ears, arrayOf("ears", "confetti"), arrayOf("forehead", "eyes"), "smile", AROcclusionMode.HairOccludes, 0.7f, 20f, 0.85f, 0.55f, 0.95f, pKind = 7)
        item("ar.fluffy_hop", "Fluffy Hop", MaskTray.Ears, 1, ears, arrayOf("ears"), arrayOf("forehead"), "smile", AROcclusionMode.HairOccludes, 0.78f, 10f, 0.98f, 0.88f, 0.78f)
        item("ar.spring_hop", "Spring Hop", MaskTray.Ears, 1, ears, arrayOf("ears", "sparkle"), arrayOf("forehead", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.76f, 14f, 0.55f, 0.9f, 0.4f)
        item("ar.foxy_mode", "Foxy Mode", MaskTray.Ears, 1, ears, arrayOf("ears", "whiskers"), arrayOf("forehead", "cheeks"), "browRaise", AROcclusionMode.HairOccludes, 0.83f, 11f, 0.78f, 0.38f, 0.12f)
        item("ar.groovy_cat", "Groovy Cat", MaskTray.Ears, 1, ears, arrayOf("ears", "eye-glow"), arrayOf("forehead", "iris"), "smile", AROcclusionMode.HairOccludes, 0.8f, 18f, 0.55f, 0.2f, 0.85f)
        item("ar.party_bear", "Party Bear", MaskTray.Ears, 1, ears, arrayOf("ears", "confetti"), arrayOf("forehead", "eyes"), "smile", AROcclusionMode.HairOccludes, 0.72f, 22f, 0.45f, 0.28f, 0.12f, pKind = 7)

        item("ar.india_glow", "India Glow", MaskTray.Trendy, 3, paint, arrayOf("face-paint", "sparkle"), arrayOf("cheeks", "forehead"), "smile", AROcclusionMode.PersonOnly, 0.2f, 16f, 1f, 0.55f, 0.12f)
        item("ar.ultra_fan", "Ultra Fan", MaskTray.Trendy, 2, acc, arrayOf("glasses", "confetti"), arrayOf("eyes", "forehead"), "smile", AROcclusionMode.HairOccludes, 0.4f, 28f, 0.15f, 0.45f, 0.95f, pKind = 7)
        item("ar.goal_rush", "Goal Rush", MaskTray.Trendy, 3, paint, arrayOf("face-paint"), arrayOf("cheeks", "faceOval"), "mouthOpen", AROcclusionMode.PersonOnly, 0.18f, 20f, 0.15f, 0.7f, 0.25f)
        item("ar.bad_santa", "Bad Santa", MaskTray.Trendy, 8, acc, arrayOf("halo", "sparkle"), arrayOf("forehead"), "smile", AROcclusionMode.HairOccludes, 0.7f, 14f, 0.75f, 0.12f, 0.1f)
        item("ar.england", "England", MaskTray.Trendy, 3, paint, arrayOf("face-paint"), arrayOf("cheeks", "forehead"), "smile", AROcclusionMode.PersonOnly, 0.15f, 12f, 0.85f, 0.12f, 0.18f)
        item("ar.france", "France", MaskTray.Trendy, 3, paint, arrayOf("face-paint"), arrayOf("cheeks", "forehead"), "smile", AROcclusionMode.PersonOnly, 0.15f, 12f, 0.2f, 0.28f, 0.75f)
        item("ar.patriot_pop", "Patriot Pop", MaskTray.Trendy, 3, paint, arrayOf("face-paint", "sparkle"), arrayOf("cheeks", "forehead"), "smile", AROcclusionMode.PersonOnly, 0.16f, 18f, 0.15f, 0.22f, 0.72f)
        item("ar.freedom_fun", "Freedom Fun", MaskTray.Trendy, 3, paint, arrayOf("face-paint", "confetti"), arrayOf("cheeks", "faceOval"), "smile", AROcclusionMode.PersonOnly, 0.14f, 24f, 0.9f, 0.15f, 0.18f, pKind = 7)
        item("ar.mega_mason", "Mega Mason", MaskTray.Trendy, 2, acc, arrayOf("glasses"), arrayOf("eyes"), "none", AROcclusionMode.HairOccludes, 0.45f, 8f, 0.35f, 0.22f, 0.12f)
        item("ar.shell_belle", "Shell Belle", MaskTray.Trendy, 7, acc, arrayOf("halo", "sparkle"), arrayOf("forehead", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.55f, 14f, 0.95f, 0.72f, 0.78f)
        item("ar.haji_glow", "Haji Glow", MaskTray.Trendy, 0, env, arrayOf("halo", "sparkle"), arrayOf("forehead"), "none", AROcclusionMode.HairOccludes, 0.6f, 16f, 0.55f, 0.85f, 0.45f)
        item("ar.psg_fever", "PSG Fever", MaskTray.Trendy, 3, paint, arrayOf("face-paint"), arrayOf("cheeks", "forehead"), "smile", AROcclusionMode.PersonOnly, 0.15f, 16f, 0.12f, 0.18f, 0.55f)
        item("ar.arsenal_vibe", "Arsenal Vibe", MaskTray.Trendy, 3, paint, arrayOf("face-paint"), arrayOf("cheeks", "forehead"), "smile", AROcclusionMode.PersonOnly, 0.15f, 16f, 0.75f, 0.08f, 0.12f)
        item("ar.starman", "Starman", MaskTray.Trendy, 4, env + ARCategory.EyeEffect, arrayOf("halo", "sparkle"), arrayOf("forehead", "iris"), "none", AROcclusionMode.HairOccludes, 0.65f, 22f, 0.95f, 0.9f, 0.45f, pKind = 6)
        item("ar.rock_king", "Rock King", MaskTray.Trendy, 6, acc, arrayOf("ears", "sparkle"), arrayOf("forehead"), "mouthOpen", AROcclusionMode.HairOccludes, 0.7f, 18f, 0.12f, 0.08f, 0.1f)
        item("ar.disco_diva", "Disco Diva", MaskTray.Trendy, 5, acc + ARCategory.EyeEffect, arrayOf("sparkle", "glasses"), arrayOf("eyes", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.4f, 30f, 0.95f, 0.2f, 0.75f)
        item("ar.fire_princess", "Fire Princess", MaskTray.Trendy, 0, env, arrayOf("halo", "dust"), arrayOf("forehead", "iris"), "smile", AROcclusionMode.HairOccludes, 0.62f, 24f, 1f, 0.32f, 0.08f)
        item("ar.island_cheers", "Island Cheers", MaskTray.Trendy, 7, acc, arrayOf("halo", "sparkle"), arrayOf("forehead", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.5f, 14f, 0.35f, 0.78f, 0.55f)
        item("ar.farm_crush", "Farm Crush", MaskTray.Trendy, 7, acc, arrayOf("halo"), arrayOf("forehead"), "smile", AROcclusionMode.HairOccludes, 0.55f, 10f, 0.72f, 0.55f, 0.22f)
        item("ar.spring_aura", "Spring Aura", MaskTray.Trendy, 7, env, arrayOf("sparkle"), arrayOf("forehead", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.45f, 18f, 0.65f, 0.92f, 0.4f)
        item("ar.floral_fantasy", "Floral Fantasy", MaskTray.Trendy, 7, acc, arrayOf("halo", "sparkle"), arrayOf("forehead", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.5f, 16f, 0.95f, 0.45f, 0.7f)
        item("ar.rave", "Rave", MaskTray.Trendy, 2, acc, arrayOf("glasses", "confetti"), arrayOf("eyes"), "mouthOpen", AROcclusionMode.HairOccludes, 0.35f, 40f, 0.2f, 0.95f, 0.85f, pKind = 7)
        item("ar.hot_devil", "Hot Devil", MaskTray.Trendy, 6, acc, arrayOf("ears", "sparkle"), arrayOf("forehead", "iris"), "smile", AROcclusionMode.HairOccludes, 0.75f, 20f, 0.85f, 0.08f, 0.05f)
        item("ar.pumpkin_doll", "Pumpkin Doll", MaskTray.Trendy, 3, paint, arrayOf("face-paint"), arrayOf("cheeks", "forehead"), "smile", AROcclusionMode.PersonOnly, 0.2f, 12f, 0.95f, 0.45f, 0.12f)
        item("ar.deadly_bloom", "Deadly Bloom", MaskTray.Trendy, 7, paint, arrayOf("face-paint", "sparkle"), arrayOf("cheeks", "mouth"), "smile", AROcclusionMode.PersonOnly, 0.22f, 14f, 0.45f, 0.08f, 0.18f)
        item("ar.sweet_feels", "Sweet Feels", MaskTray.Trendy, 3, paint, arrayOf("face-paint", "hearts"), arrayOf("cheeks", "mouth"), "smile", AROcclusionMode.PersonOnly, 0.18f, 20f, 1f, 0.62f, 0.72f, pKind = 5)
        item("ar.t_rex", "T-Rex", MaskTray.Trendy, 6, ears, arrayOf("ears"), arrayOf("forehead"), "mouthOpen", AROcclusionMode.HairOccludes, 0.8f, 8f, 0.22f, 0.55f, 0.18f)
        item("ar.love_lens", "Love Lens", MaskTray.Trendy, 9, eye, arrayOf("eye-glow", "hearts"), arrayOf("iris", "mouth"), "smile", AROcclusionMode.HairOccludes, 0.25f, 16f, 1f, 0.35f, 0.45f, pKind = 5)
        item("ar.retro", "Retro", MaskTray.Trendy, 2, acc, arrayOf("glasses"), arrayOf("eyes"), "none", AROcclusionMode.HairOccludes, 0.42f, 8f, 0.35f, 0.22f, 0.08f)

        item("ar.brainy", "Brainy", MaskTray.Funny, 8, acc, arrayOf("halo"), arrayOf("forehead"), "browRaise", AROcclusionMode.HairOccludes, 0.78f, 6f, 0.95f, 0.55f, 0.62f)
        item("ar.showstopper", "Showstopper", MaskTray.Funny, 2, acc, arrayOf("glasses", "sparkle"), arrayOf("eyes", "forehead"), "smile", AROcclusionMode.HairOccludes, 0.4f, 26f, 0.95f, 0.85f, 0.2f)
        item("ar.lucky_charm", "Lucky Charm", MaskTray.Funny, 7, acc, arrayOf("sparkle"), arrayOf("forehead", "cheeks"), "smile", AROcclusionMode.HairOccludes, 0.45f, 18f, 0.25f, 0.78f, 0.28f)
        item("ar.get_lucky", "Get Lucky", MaskTray.Funny, 7, paint, arrayOf("face-paint", "sparkle"), arrayOf("cheeks"), "smile", AROcclusionMode.PersonOnly, 0.2f, 16f, 0.95f, 0.78f, 0.15f)
        item("ar.emerald_veil", "Emerald Veil", MaskTray.Funny, 3, paint, arrayOf("face-paint"), arrayOf("cheeks", "forehead"), "smile", AROcclusionMode.PersonOnly, 0.22f, 10f, 0.12f, 0.62f, 0.32f)
        item("ar.daisy_daze", "Daisy Daze", MaskTray.Funny, 7, acc, arrayOf("halo", "sparkle"), arrayOf("forehead"), "smile", AROcclusionMode.HairOccludes, 0.5f, 12f, 1f, 0.92f, 0.35f)

        item("ar.desert_lace", "Desert Lace", MaskTray.Fantasy, 3, paint, arrayOf("face-paint", "sparkle"), arrayOf("cheeks", "forehead"), "smile", AROcclusionMode.PersonOnly, 0.2f, 12f, 0.82f, 0.55f, 0.32f)
        item("ar.winter_bloom", "Winter Bloom", MaskTray.Fantasy, 7, env, arrayOf("sparkle", "halo"), arrayOf("forehead", "iris"), "none", AROcclusionMode.HairOccludes, 0.55f, 18f, 0.72f, 0.85f, 1f)
        item("ar.masked_meow", "Masked Meow", MaskTray.Fantasy, 1, ears, arrayOf("ears", "whiskers", "eye-glow"), arrayOf("forehead", "iris", "cheeks"), "browRaise", AROcclusionMode.HairOccludes, 0.86f, 10f, 0.55f, 0.22f, 0.62f)
        item("ar.hippie", "Hippie", MaskTray.Fantasy, 7, acc, arrayOf("glasses", "sparkle"), arrayOf("eyes", "forehead"), "smile", AROcclusionMode.HairOccludes, 0.4f, 14f, 0.55f, 0.85f, 0.35f)
    }

    val byId = effects.associateBy { it.id }

    fun require(id: String) = byId[id]

    fun inTray(tray: MaskTray) = effects.filter { it.tray == tray }

    fun validate(): List<String> {
        val e = ArrayList<String>()
        val ids = HashSet<String>()
        val looks = HashSet<Int>()
        for (fx in effects) {
            if (!ids.add(fx.id)) e += "dup ${fx.id}"
            if (!looks.add(fx.look)) e += "dup look ${fx.look}"
            if (!fx.productionRendered) e += "${fx.id} not rendered"
            if (fx.assets.isEmpty() || fx.assets.any { !it.valid() }) e += "${fx.id} assets"
            if (fx.anchors.isEmpty() || fx.anchors.any { !it.valid() }) e += "${fx.id} anchors"
            if (!fx.animation.valid()) e += "${fx.id} anim"
            if (fx.intensityMin != 0f || fx.intensityMax != 1f) e += "${fx.id} intensity range"
            if (fx.categories.isEmpty()) e += "${fx.id} cats"
            if (fx.paint.size != 3) e += "${fx.id} paint"
            if (fx.family !in 0..9) e += "${fx.id} family"
        }
        return e
    }
}
