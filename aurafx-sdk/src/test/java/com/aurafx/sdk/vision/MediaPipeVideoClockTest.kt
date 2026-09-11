package com.aurafx.sdk.vision

import com.aurafx.sdk.vision.mediapipe.MediaPipeVideoClock
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaPipeVideoClockTest {
    @Test
    fun advancesWhenCameraRepeatsOrRewindsTimestamps() {
        val clock = MediaPipeVideoClock()
        val a = clock.nextMs(5_000_000L)
        val b = clock.nextMs(5_000_000L)
        val c = clock.nextMs(1_000_000L)
        val d = clock.nextMs(12_000_000L)
        assertThat(a).isEqualTo(5L)
        assertThat(b).isEqualTo(6L)
        assertThat(c).isEqualTo(7L)
        assertThat(d).isEqualTo(12L)
    }
}
