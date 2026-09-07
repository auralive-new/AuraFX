package com.aurafx.sdk.reference

import java.io.File

/**
 * Video-file audit. Counts are established only from files that actually exist
 * under [referenceRoot]. Missing files do not produce invented totals.
 */
object ReferenceVideoAudit {
    const val STATUS_SOURCE_MISSING = "SOURCE_MISSING"
    const val STATUS_UNREADABLE = "UNREADABLE — FRAME REVIEW REQUIRED"
    const val PARITY_INCOMPLETE = "INCOMPLETE"

    val videoExtensions = setOf("mp4", "mov", "webm", "mkv", "m4v")

    fun searchRoots(): List<File> {
        val cwd = File(".").canonicalFile
        val parent = cwd.parentFile
        return listOfNotNull(
            File(cwd, "reference"),
            parent?.let { File(it, "reference") },
        ).distinctBy { it.canonicalPath }
    }

    fun videoFiles(): List<File> =
        searchRoots()
            .filter { it.exists() && it.isDirectory }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension.lowercase() in videoExtensions }
                    .toList()
            }
            .distinctBy { it.canonicalPath }
            .sortedBy { it.path }

    fun videoCount(): Int = videoFiles().size

    fun sourceStatus(): String =
        if (videoCount() == 0) STATUS_SOURCE_MISSING else "VIDEOS_PRESENT"

    /**
     * Totals from this workspace's video files. Null means not established.
     */
    fun referenceFilterTotal(): Int? = if (videoCount() == 0) null else null

    fun referenceArTotal(): Int? = if (videoCount() == 0) null else null

    fun referenceMaskTotal(): Int? = if (videoCount() == 0) null else null

    fun referenceBackgroundTotal(): Int? = if (videoCount() == 0) null else null

    fun parityStatus(): String = PARITY_INCOMPLETE

    /**
     * Still-frame evidence that is not a video file. Names here are only those
     * actually readable on the supplied still. Host chrome is recorded as
     * out-of-scope for the SDK (Step 8).
     */
    data class StillEvidence(
        val source: String,
        val readableLabels: List<String>,
        val carouselItemsVisible: Int,
        val notes: String,
    )

    fun attachedStillEvidence(): StillEvidence = StillEvidence(
        source = "user-attached still (not a video file)",
        readableLabels = listOf(
            "Go Live",
            "Live Description",
            "Live singing",
            "Hot",
            "Premium",
            "Party",
            "Audio",
            "3,000/min",
        ),
        carouselItemsVisible = 0,
        notes = "Portrait host preview with an interior canopy/city-night look. " +
            "No filter/AR/mask/background carousel tiles are visible on this still. " +
            "Go Live chrome is AuraLive host UI and is out of scope until Step 8.",
    )
}
