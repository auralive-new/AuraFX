package com.aurafx.sdk.api

enum class GiftFxCategory {
    Temporal,
    Portal,
    Creature,
    Hologram,
    Paint,
    Shadow,
    World,
    Gravity,
    Dimension,
    Ink,
}

enum class GiftFxRendererType {
    TimeFreeze,
    PortalDoor,
    MeteorCreature,
    HologramClone,
    MagicPaint,
    GiantShadow,
    MiniWorld,
    GravityFlip,
    MirrorDimension,
    InkUniverse,
}

enum class GiftFxLayer {
    Background,
    BehindSubject,
    SubjectAttached,
    Foreground,
    FullFrame,
}

enum class GiftFxAnchor {
    ScreenCenter,
    Face,
    Head,
    Body,
    Shoulder,
}

enum class GiftFxOcclusion {
    None,
    PersonMask,
    HairMask,
    PersonAndHair,
}

enum class GiftFxTrackingNeed {
    None,
    Face,
    Body,
    FaceOrBody,
    Segmentation,
}

enum class GiftFxCleanupBehavior {
    AutoFade,
    ParticleCollapse,
    SnapRelease,
}
