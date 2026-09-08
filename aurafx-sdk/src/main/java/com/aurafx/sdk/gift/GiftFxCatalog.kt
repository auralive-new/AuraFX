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
    const val NEON_TRAIN = "gift.neon_train"
    const val FLOATING_CASTLE = "gift.floating_castle"
    const val DRAGON_FLIGHT = "gift.dragon_flight"
    const val OCEAN_WAVE = "gift.ocean_wave"
    const val VOLCANO = "gift.volcano"
    const val SAKURA_WORLD = "gift.sakura_world"
    const val SAMURAI_PORTAL = "gift.samurai_portal"
    const val ROBOT_ARRIVAL = "gift.robot_arrival"
    const val SPACE_STATION = "gift.space_station"
    const val BLACK_HOLE = "gift.black_hole"
    const val CRYSTAL_FOREST = "gift.crystal_forest"
    const val MOON_ECLIPSE = "gift.moon_eclipse"
    const val CLOUD_CITY = "gift.cloud_city"
    const val THUNDER_GOD = "gift.thunder_god"
    const val AURORA_DANCE = "gift.aurora_dance"
    const val LAVA_RIFT = "gift.lava_rift"
    const val FAIRY_GARDEN = "gift.fairy_garden"
    const val STEAM_MACHINE = "gift.steam_machine"
    const val PAPER_UNIVERSE = "gift.paper_universe"
    const val MAGNETIC_FIELD = "gift.magnetic_field"
    const val LASER_SHOW = "gift.laser_show"
    const val DIGITAL_RAIN = "gift.digital_rain"
    const val PIXEL_WORLD = "gift.pixel_world"
    const val UNDERWATER_REALM = "gift.underwater_realm"
    const val DESERT_STORM = "gift.desert_storm"
    const val ICE_PALACE = "gift.ice_palace"
    const val FLOWER_BLOOM = "gift.flower_bloom"
    const val JUNGLE_ESCAPE = "gift.jungle_escape"
    const val PHANTOM_TRAIN = "gift.phantom_train"
    const val CLOCKWORK_DIMENSION = "gift.clockwork_dimension"
    const val ROCKET_LAUNCH = "gift.rocket_launch"
    const val METEOR_SHOWER = "gift.meteor_shower"
    const val LEVITATING_TEMPLE = "gift.levitating_temple"
    const val STORM_EYE = "gift.storm_eye"
    const val FLOATING_ISLANDS = "gift.floating_islands"
    const val ALIEN_CONTACT = "gift.alien_contact"
    const val GOLDEN_DIMENSION = "gift.golden_dimension"
    const val MUSIC_DIMENSION = "gift.music_dimension"
    const val COSMIC_BUTTERFLY = "gift.cosmic_butterfly"
    const val FINAL_DIMENSION = "gift.final_dimension"

    val originalTen: List<String> = listOf(
        TIME_FREEZE, PORTAL_DOOR, METEOR_CREATURE, HOLOGRAM_CLONE, MAGIC_PAINT,
        GIANT_SHADOW, MINI_WORLD, GRAVITY_FLIP, MIRROR_DIMENSION, INK_UNIVERSE,
    )

    val ordered: List<String> = originalTen + listOf(
        NEON_TRAIN, FLOATING_CASTLE, DRAGON_FLIGHT, OCEAN_WAVE, VOLCANO,
        SAKURA_WORLD, SAMURAI_PORTAL, ROBOT_ARRIVAL, SPACE_STATION, BLACK_HOLE,
        CRYSTAL_FOREST, MOON_ECLIPSE, CLOUD_CITY, THUNDER_GOD, AURORA_DANCE,
        LAVA_RIFT, FAIRY_GARDEN, STEAM_MACHINE, PAPER_UNIVERSE, MAGNETIC_FIELD,
        LASER_SHOW, DIGITAL_RAIN, PIXEL_WORLD, UNDERWATER_REALM, DESERT_STORM,
        ICE_PALACE, FLOWER_BLOOM, JUNGLE_ESCAPE, PHANTOM_TRAIN, CLOCKWORK_DIMENSION,
        ROCKET_LAUNCH, METEOR_SHOWER, LEVITATING_TEMPLE, STORM_EYE, FLOATING_ISLANDS,
        ALIEN_CONTACT, GOLDEN_DIMENSION, MUSIC_DIMENSION, COSMIC_BUTTERFLY, FINAL_DIMENSION,
    )
}

object GiftFxCatalog {
    const val SIZE = 50
    private val timeline = GiftFxTimelineSpec()

    val samples: List<GiftFxDefinition> = listOf(
        def(GiftFxIds.TIME_FREEZE, "Time Freeze", 8500, GiftFxCategory.Temporal, GiftFxRendererType.TimeFreeze, 0, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.FullFrame, GiftFxAnchor.Face, GiftFxOcclusion.None, GiftFxCleanupBehavior.SnapRelease, parts(48, 18f, 0f, true, 0)),
        def(GiftFxIds.PORTAL_DOOR, "Portal Door", 9200, GiftFxCategory.Portal, GiftFxRendererType.PortalDoor, 1, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Body, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(96, 42f, -0.04f, true, 1)),
        def(GiftFxIds.METEOR_CREATURE, "Meteor Creature", 8800, GiftFxCategory.Creature, GiftFxRendererType.MeteorCreature, 2, GiftFxTrackingNeed.None, GiftFxLayer.Foreground, GiftFxAnchor.ScreenCenter, GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse, parts(128, 64f, 0.35f, true, 2)),
        def(GiftFxIds.HOLOGRAM_CLONE, "Hologram Clone", 9000, GiftFxCategory.Hologram, GiftFxRendererType.HologramClone, 3, GiftFxTrackingNeed.Face, GiftFxLayer.SubjectAttached, GiftFxAnchor.Face, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(32, 12f, 0f, false, 3)),
        def(GiftFxIds.MAGIC_PAINT, "Magic Paint", 9500, GiftFxCategory.Paint, GiftFxRendererType.MagicPaint, 4, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Head, GiftFxOcclusion.HairMask, GiftFxCleanupBehavior.ParticleCollapse, parts(160, 55f, 0.02f, true, 4)),
        def(GiftFxIds.GIANT_SHADOW, "Giant Shadow", 8700, GiftFxCategory.Shadow, GiftFxRendererType.GiantShadow, 5, GiftFxTrackingNeed.Segmentation, GiftFxLayer.BehindSubject, GiftFxAnchor.Body, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(24, 8f, 0f, false, 5)),
        def(GiftFxIds.MINI_WORLD, "Mini World", 10000, GiftFxCategory.World, GiftFxRendererType.MiniWorld, 6, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.SubjectAttached, GiftFxAnchor.Head, GiftFxOcclusion.HairMask, GiftFxCleanupBehavior.AutoFade, parts(80, 28f, -0.01f, true, 6)),
        def(GiftFxIds.GRAVITY_FLIP, "Gravity Flip", 8200, GiftFxCategory.Gravity, GiftFxRendererType.GravityFlip, 7, GiftFxTrackingNeed.None, GiftFxLayer.FullFrame, GiftFxAnchor.ScreenCenter, GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse, parts(192, 70f, 0.55f, true, 7)),
        def(GiftFxIds.MIRROR_DIMENSION, "Mirror Dimension", 9600, GiftFxCategory.Dimension, GiftFxRendererType.MirrorDimension, 8, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Face, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(40, 16f, 0f, false, 8)),
        def(GiftFxIds.INK_UNIVERSE, "Ink Universe", 9800, GiftFxCategory.Ink, GiftFxRendererType.InkUniverse, 9, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Body, GiftFxOcclusion.PersonAndHair, GiftFxCleanupBehavior.ParticleCollapse, parts(144, 48f, 0.06f, true, 9)),
        def(GiftFxIds.NEON_TRAIN, "Neon Train", 8400, GiftFxCategory.SciFi, GiftFxRendererType.NeonTrain, 10, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Torso, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(96, 40f, 0.02f, true, 10)),
        def(GiftFxIds.FLOATING_CASTLE, "Floating Castle", 9300, GiftFxCategory.Fantasy, GiftFxRendererType.FloatingCastle, 11, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Head, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(72, 22f, -0.02f, true, 11)),
        def(GiftFxIds.DRAGON_FLIGHT, "Dragon Flight", 9100, GiftFxCategory.Fantasy, GiftFxRendererType.DragonFlight, 12, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Head, GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse, parts(128, 50f, 0.04f, true, 12)),
        def(GiftFxIds.OCEAN_WAVE, "Ocean Wave", 8600, GiftFxCategory.Nature, GiftFxRendererType.OceanWave, 13, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Body, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(110, 36f, 0.18f, true, 13)),
        def(GiftFxIds.VOLCANO, "Volcano", 8900, GiftFxCategory.Nature, GiftFxRendererType.Volcano, 14, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Torso, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.ParticleCollapse, parts(160, 58f, -0.28f, true, 14)),
        def(GiftFxIds.SAKURA_WORLD, "Sakura World", 9400, GiftFxCategory.Nature, GiftFxRendererType.SakuraWorld, 15, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Head, GiftFxOcclusion.HairMask, GiftFxCleanupBehavior.ParticleCollapse, parts(150, 48f, 0.12f, true, 15)),
        def(GiftFxIds.SAMURAI_PORTAL, "Samurai Portal", 9700, GiftFxCategory.Fantasy, GiftFxRendererType.SamuraiPortal, 16, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Body, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(88, 30f, 0f, true, 16)),
        def(GiftFxIds.ROBOT_ARRIVAL, "Robot Arrival", 8300, GiftFxCategory.Tech, GiftFxRendererType.RobotArrival, 17, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Shoulder, GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse, parts(100, 34f, 0.06f, true, 17)),
        def(GiftFxIds.SPACE_STATION, "Space Station", 9900, GiftFxCategory.SciFi, GiftFxRendererType.SpaceStation, 18, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.SubjectAttached, GiftFxAnchor.Head, GiftFxOcclusion.HairMask, GiftFxCleanupBehavior.AutoFade, parts(80, 26f, 0f, true, 18)),
        def(GiftFxIds.BLACK_HOLE, "Black Hole", 9050, GiftFxCategory.Cosmic, GiftFxRendererType.BlackHole, 19, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Body, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.SnapRelease, parts(140, 44f, 0f, true, 19)),
        def(GiftFxIds.CRYSTAL_FOREST, "Crystal Forest", 8750, GiftFxCategory.Magic, GiftFxRendererType.CrystalForest, 20, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Torso, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.ParticleCollapse, parts(96, 28f, 0.02f, true, 20)),
        def(GiftFxIds.MOON_ECLIPSE, "Moon Eclipse", 9150, GiftFxCategory.Cosmic, GiftFxRendererType.MoonEclipse, 21, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Head, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(48, 14f, 0f, false, 21)),
        def(GiftFxIds.CLOUD_CITY, "Cloud City", 9450, GiftFxCategory.Adventure, GiftFxRendererType.CloudCity, 22, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Head, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(90, 24f, -0.03f, true, 22)),
        def(GiftFxIds.THUNDER_GOD, "Thunder God", 8550, GiftFxCategory.Fantasy, GiftFxRendererType.ThunderGod, 23, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Body, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.ParticleCollapse, parts(120, 52f, 0f, true, 23)),
        def(GiftFxIds.AURORA_DANCE, "Aurora Dance", 9650, GiftFxCategory.Cinematic, GiftFxRendererType.AuroraDance, 24, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.FullFrame, GiftFxAnchor.Face, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(100, 32f, 0f, true, 24)),
        def(GiftFxIds.LAVA_RIFT, "Lava Rift", 8350, GiftFxCategory.Adventure, GiftFxRendererType.LavaRift, 25, GiftFxTrackingNeed.None, GiftFxLayer.FullFrame, GiftFxAnchor.ScreenCenter, GiftFxOcclusion.None, GiftFxCleanupBehavior.SnapRelease, parts(150, 46f, -0.2f, true, 25)),
        def(GiftFxIds.FAIRY_GARDEN, "Fairy Garden", 9250, GiftFxCategory.Magic, GiftFxRendererType.FairyGarden, 26, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Head, GiftFxOcclusion.HairMask, GiftFxCleanupBehavior.ParticleCollapse, parts(130, 40f, -0.05f, true, 26)),
        def(GiftFxIds.STEAM_MACHINE, "Steam Machine", 8850, GiftFxCategory.Tech, GiftFxRendererType.SteamMachine, 27, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.SubjectAttached, GiftFxAnchor.Torso, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(70, 20f, 0.01f, true, 27)),
        def(GiftFxIds.PAPER_UNIVERSE, "Paper Universe", 9550, GiftFxCategory.Magic, GiftFxRendererType.PaperUniverse, 28, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Face, GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse, parts(110, 34f, -0.08f, true, 28)),
        def(GiftFxIds.MAGNETIC_FIELD, "Magnetic Field", 8150, GiftFxCategory.SciFi, GiftFxRendererType.MagneticField, 29, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.SubjectAttached, GiftFxAnchor.Body, GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse, parts(160, 60f, 0f, true, 29)),
        def(GiftFxIds.LASER_SHOW, "Laser Show", 8050, GiftFxCategory.Tech, GiftFxRendererType.LaserShow, 30, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Eyes, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(64, 18f, 0f, false, 30)),
        def(GiftFxIds.DIGITAL_RAIN, "Digital Rain", 9750, GiftFxCategory.Tech, GiftFxRendererType.DigitalRain, 31, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.FullFrame, GiftFxAnchor.Face, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(120, 38f, 0.4f, true, 31)),
        def(GiftFxIds.PIXEL_WORLD, "Pixel World", 8450, GiftFxCategory.Tech, GiftFxRendererType.PixelWorld, 32, GiftFxTrackingNeed.None, GiftFxLayer.FullFrame, GiftFxAnchor.ScreenCenter, GiftFxOcclusion.None, GiftFxCleanupBehavior.SnapRelease, parts(80, 22f, 0f, false, 32)),
        def(GiftFxIds.UNDERWATER_REALM, "Underwater Realm", 9350, GiftFxCategory.Nature, GiftFxRendererType.UnderwaterRealm, 33, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Body, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(140, 42f, -0.15f, true, 33)),
        def(GiftFxIds.DESERT_STORM, "Desert Storm", 8650, GiftFxCategory.Adventure, GiftFxRendererType.DesertStorm, 34, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.FullFrame, GiftFxAnchor.Torso, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(150, 50f, 0.08f, true, 34)),
        def(GiftFxIds.ICE_PALACE, "Ice Palace", 9950, GiftFxCategory.Fantasy, GiftFxRendererType.IcePalace, 35, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Body, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.ParticleCollapse, parts(100, 30f, 0.05f, true, 35)),
        def(GiftFxIds.FLOWER_BLOOM, "Flower Bloom", 8250, GiftFxCategory.Nature, GiftFxRendererType.FlowerBloom, 36, GiftFxTrackingNeed.None, GiftFxLayer.Foreground, GiftFxAnchor.ScreenCenter, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.ParticleCollapse, parts(130, 36f, -0.12f, true, 36)),
        def(GiftFxIds.JUNGLE_ESCAPE, "Jungle Escape", 9850, GiftFxCategory.Adventure, GiftFxRendererType.JungleEscape, 37, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Shoulder, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(120, 33f, 0.03f, true, 37)),
        def(GiftFxIds.PHANTOM_TRAIN, "Phantom Train", 8950, GiftFxCategory.Cinematic, GiftFxRendererType.PhantomTrain, 38, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Torso, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(90, 28f, 0.02f, true, 38)),
        def(GiftFxIds.CLOCKWORK_DIMENSION, "Clockwork Dimension", 9120, GiftFxCategory.Tech, GiftFxRendererType.ClockworkDimension, 39, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.SubjectAttached, GiftFxAnchor.Body, GiftFxOcclusion.HairMask, GiftFxCleanupBehavior.AutoFade, parts(70, 18f, 0f, false, 39)),
        def(GiftFxIds.ROCKET_LAUNCH, "Rocket Launch", 8680, GiftFxCategory.SciFi, GiftFxRendererType.RocketLaunch, 40, GiftFxTrackingNeed.None, GiftFxLayer.Foreground, GiftFxAnchor.ScreenCenter, GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse, parts(170, 62f, -0.45f, true, 40)),
        def(GiftFxIds.METEOR_SHOWER, "Meteor Shower", 9220, GiftFxCategory.Cosmic, GiftFxRendererType.MeteorShower, 41, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Head, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.ParticleCollapse, parts(160, 55f, 0.4f, true, 41)),
        def(GiftFxIds.LEVITATING_TEMPLE, "Levitating Temple", 9480, GiftFxCategory.Fantasy, GiftFxRendererType.LevitatingTemple, 42, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Body, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(80, 20f, -0.04f, true, 42)),
        def(GiftFxIds.STORM_EYE, "Storm Eye", 8780, GiftFxCategory.Cinematic, GiftFxRendererType.StormEye, 43, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.FullFrame, GiftFxAnchor.Face, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(140, 44f, 0f, true, 43)),
        def(GiftFxIds.FLOATING_ISLANDS, "Floating Islands", 9580, GiftFxCategory.Adventure, GiftFxRendererType.FloatingIslands, 44, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.BehindSubject, GiftFxAnchor.Head, GiftFxOcclusion.PersonMask, GiftFxCleanupBehavior.AutoFade, parts(90, 24f, 0.02f, true, 44)),
        def(GiftFxIds.ALIEN_CONTACT, "Alien Contact", 8380, GiftFxCategory.SciFi, GiftFxRendererType.AlienContact, 45, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Head, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(110, 36f, 0.05f, true, 45)),
        def(GiftFxIds.GOLDEN_DIMENSION, "Golden Dimension", 9680, GiftFxCategory.Premium, GiftFxRendererType.GoldenDimension, 46, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Face, GiftFxOcclusion.HairMask, GiftFxCleanupBehavior.ParticleCollapse, parts(120, 40f, 0f, true, 46)),
        def(GiftFxIds.MUSIC_DIMENSION, "Music Dimension", 8880, GiftFxCategory.Premium, GiftFxRendererType.MusicDimension, 47, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.SubjectAttached, GiftFxAnchor.Eyes, GiftFxOcclusion.None, GiftFxCleanupBehavior.AutoFade, parts(100, 30f, 0f, true, 47)),
        def(GiftFxIds.COSMIC_BUTTERFLY, "Cosmic Butterfly", 9180, GiftFxCategory.Cosmic, GiftFxRendererType.CosmicButterfly, 48, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.Foreground, GiftFxAnchor.Head, GiftFxOcclusion.None, GiftFxCleanupBehavior.ParticleCollapse, parts(150, 48f, -0.02f, true, 48)),
        def(GiftFxIds.FINAL_DIMENSION, "Final Dimension", 9990, GiftFxCategory.Premium, GiftFxRendererType.FinalDimension, 49, GiftFxTrackingNeed.FaceOrBody, GiftFxLayer.FullFrame, GiftFxAnchor.Face, GiftFxOcclusion.None, GiftFxCleanupBehavior.SnapRelease, parts(180, 58f, 0f, true, 49)),
    )

    fun require(id: String): GiftFxDefinition? = samples.firstOrNull { it.giftId == id }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (samples.size != SIZE) errors += "catalog size ${samples.size} != $SIZE"
        val ids = samples.map { it.giftId }
        if (ids.toSet().size != ids.size) errors += "duplicate gift ids"
        if (ids != GiftFxIds.ordered) errors += "id order mismatch"
        if (ids.take(10) != GiftFxIds.originalTen) errors += "original ten mutated"
        val looks = samples.map { it.shaderLook }
        if (looks.toSet().size != samples.size) errors += "duplicate shaderLook"
        val renderers = samples.map { it.rendererType }
        if (renderers.toSet().size != samples.size) errors += "duplicate rendererType"
        for (d in samples) {
            if (!d.valid()) errors += "invalid ${d.giftId}"
            if (!d.gpuRendered) errors += "${d.giftId} not gpu"
            if (d.timeline == GiftFxTimelineSpec() && !d.timeline.valid()) errors += "${d.giftId} bad timeline"
        }
        return errors
    }

    private fun parts(max: Int, emit: Float, g: Float, trail: Boolean, kind: Int) =
        GiftFxParticleConfig(max, emit, g, trail, kind)

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
