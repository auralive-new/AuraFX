package com.aurafx.sdk.scene

/**
 * Step 5 replacement hairstyles are catalogued with tracked anchors but are not
 * production-rendered without groom assets. Do not mark them complete.
 * Close in Step 7 QA with real assets.
 */
object Step5Outstanding {
    const val REPLACEMENT_HAIRSTYLES_REQUIRING_GROOM = 12
    val ids: List<String> = listOf(
        "hair.style.bob", "hair.style.pixie", "hair.style.long_layers", "hair.style.bangs",
        "hair.style.ponytail", "hair.style.bun", "hair.style.braid", "hair.style.curtain",
        "hair.style.wolf", "hair.style.shag", "hair.style.volume", "hair.style.asymmetric",
    )
}
