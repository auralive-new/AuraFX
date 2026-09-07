package com.aurafx.sdk.capture

/**
 * A/V timestamps are derived from observed clocks, never invented FPS.
 * Video PTS uses SurfaceTexture timestamp deltas. Audio PTS uses elapsed
 * nanoseconds from the same recording start mark.
 */
object AvTimestamps {
    fun toUs(ns: Long): Long = ns / 1_000L

    fun relativeUs(timestampNs: Long, originNs: Long): Long {
        val d = timestampNs - originNs
        return if (d < 0L) 0L else toUs(d)
    }

    fun monotonic(previousUs: Long, nextUs: Long): Boolean = nextUs >= previousUs

    fun bumpIfNeeded(previousUs: Long, candidateUs: Long): Long {
        return if (candidateUs > previousUs) candidateUs else previousUs + 1L
    }
}
