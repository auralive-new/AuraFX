package com.aurafx.sdk.gift

import com.aurafx.sdk.api.GiftFxAnchor
import com.aurafx.sdk.api.GiftFxCategory
import com.aurafx.sdk.api.GiftFxCleanupBehavior
import com.aurafx.sdk.api.GiftFxLayer
import com.aurafx.sdk.api.GiftFxOcclusion
import com.aurafx.sdk.api.GiftFxRendererType
import com.aurafx.sdk.api.GiftFxTrackingNeed

object GiftFxIds {
    const val TIME_FREEZE = "gift.time_freeze"
    const val PORTAL_DOOR = "gift.portal_door"
    const val METEOR_CREATURE = "gift.meteor_creature"
    const val HOLOGRAM_CLONE = "gift.hologram_clone"
    const val MAGIC_PAINT = "gift.magic_paint"
    const val GIANT_SHADOW = "gift.giant_shadow"
    const val MINI_WORLD = "gift.mini_world"
    const val GRAVITY_FLIP = "gift.gravity_flip"
    const val MIRROR_DIMENSION = "gift.mirror_dimension"
    const val INK_UNIVERSE = "gift.ink_universe"

    val ordered: List<String> = listOf(
        TIME_FREEZE,
        PORTAL_DOOR,
        METEOR_CREATURE,
        HOLOGRAM_CLONE,
        MAGIC_PAINT,
        GIANT_SHADOW,
        MINI_WORLD,
        GRAVITY_FLIP,
        MIRROR_DIMENSION,
        INK_UNIVERSE,
    )
}

object GiftFxCatalog {
    private val timeline = GiftFxTimelineSpec()

    val samples: List<GiftFxDefinition> = listOf(
        def(
            GiftFxIds.TIME_FREEZE, "Time Freeze", 8500,
            GiftFxCategory.Temporal, GiftFxRendererType.TimeFreeze, 0,
            GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.FullFrame, GiftFxAnchor.Face,
            GiftFxOcclusion.None, GiftFxCleanupBehavior.SnapRelease,
            GiftFxParticleConfig(48, 18f, 0f, true, 0),
        ),
        def(
            GiftFxIds.PORTAL_DOOR, "Portal Door", 9200,
            GiftFxCategory.Portal, GiftFxRendererType.PortalDoor, 1,
            GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Body,
            GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade,
            GiftFxParticleConfig(96, 42f, -0.04f, true, 1),
        ),
        def(
            GiftFxIds.METEOR_CREATURE, "Meteor Creature", 8800,
            GiftFxCategory.Creature, GiftFxRendererType.MeteorCreature, 2,
            GiftFxTrackingNeed.None, GiftFxLayer.Foreground, GiftFxAnchor.ScreenCenter,
            GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse,
            GiftFxParticleConfig(128, 64f, 0.35f, true, 2),
        ),
        def(
            GiftFxIds.HOLOGRAM_CLONE, "Hologram Clone", 9000,
            GiftFxCategory.Hologram, GiftFxRendererType.HologramClone, 3,
            GiftFxTrackingNeed.Face, GiftFxLayer.SubjectAttached, GiftFxAnchor.Face,
            GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade,
            GiftFxParticleConfig(32, 12f, 0f, false, 3),
        ),
        def(
            GiftFxIds.MAGIC_PAINT, "Magic Paint", 9500,
            GiftFxCategory.Paint, GiftFxRendererType.MagicPaint, 4,
            GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Head,
            GiftFxOcclusion.HairMask, GiftFxCleanupBehavior.ParticleCollapse,
            GiftFxParticleConfig(160, 55f, 0.02f, true, 4),
        ),
        def(
            GiftFxIds.GIANT_SHADOW, "Giant Shadow", 8700,
            GiftFxCategory.Shadow, GiftFxRendererType.GiantShadow, 5,
            GiftFxTrackingNeed.Segmentation, GiftFxLayer.BehindSubject, GiftFxAnchor.Body,
            GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade,
            GiftFxParticleConfig(24, 8f, 0f, false, 5),
        ),
        def(
            GiftFxIds.MINI_WORLD, "Mini World", 10000,
            GiftFxCategory.World, GiftFxRendererType.MiniWorld, 6,
            GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.SubjectAttached, GiftFxAnchor.Head,
            GiftFxOcclusion.HairMask, GiftFxCleanupBehavior.AutoFade,
            GiftFxParticleConfig(80, 28f, -0.01f, true, 6),
        ),
        def(
            GiftFxIds.GRAVITY_FLIP, "Gravity Flip", 8200,
            GiftFxCategory.Gravity, GiftFxRendererType.GravityFlip, 7,
            GiftFxTrackingNeed.None, GiftFxLayer.FullFrame, GiftFxAnchor.ScreenCenter,
            GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse,
            GiftFxParticleConfig(192, 70f, 0.55f, true, 7),
        ),
        def(
            GiftFxIds.MIRROR_DIMENSION, "Mirror Dimension", 9600,
            GiftFxCategory.Dimension, GiftFxRendererType.MirrorDimension, 8,
            GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Face,
            GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade,
            GiftFxParticleConfig(40, 16f, 0f, false, 8),
        ),
        def(
            GiftFxIds.INK_UNIVERSE, "Ink Universe", 9800,
            GiftFxCategory.Ink, GiftFxRendererType.InkUniverse, 9,
            GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Body,
            GiftFxOcclusion.PersonAndHair, GiftFxCleanupBehavior.ParticleCollapse,
            GiftFxParticleConfig(144, 48f, 0.06f, true, 9),
        ),
    )

    fun require(id: String): GiftFxDefinition? = samples.firstOrNull { it.giftId == id }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (samples.size != 10) errors += "catalog size ${samples.size} != 10"
        val ids = samples.map { it.giftId }
        if (ids.toSet().size != ids.size) errors += "duplicate gift ids"
        if (ids != GiftFxIds.ordered) errors += "id order mismatch"
        for (d in samples) {
            if (!d.valid()) errors += "invalid ${d.giftId}"
            if (!d.gpuRendered) errors += "${d.giftId} not gpu"
        }
        return errors
    }

    private fun def(
        id: String,
        name: String,
        durationMs: Int,
        category: GiftFxCategory,
        renderer: GiftFxRendererType,
        look: Int,
        tracking: GiftFxTrackingNeed,
        layer: GiftFxLayer,
        anchor: GiftFxAnchor,
        occlusion: GiftFxOcclusion,
        cleanup: GiftFxCleanupBehavior,
        particles: GiftFxParticleConfig,
    ) = GiftFxDefinition(
        giftId = id,
        displayName = name,
        durationMs = durationMs,
        category = category,
        rendererType = renderer,
        requiredTracking = tracking,
        layer = layer,
        anchor = anchor,
        occlusion = occlusion,
        timeline = timeline,
        particles = particles,
        cleanup = cleanup,
        shaderLook = look,
        gpuRendered = true,
    )
}
