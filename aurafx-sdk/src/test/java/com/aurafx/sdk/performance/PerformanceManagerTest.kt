package com.aurafx.sdk.performance

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PerformanceManagerTest {
    @Test
    fun snapshotStartsAtZeroWithoutInventingGpuOrStartup() {
        val manager = PerformanceManager(
            memoryProbe = { 1234L },
            nativeHeapProbe = { null },
        )
        val snap = manager.snapshot()
        assertThat(snap.fps).isEqualTo(0f)
        assertThat(snap.presentedFrames).isEqualTo(0)
        assertThat(snap.droppedFrames).isEqualTo(0)
        assertThat(snap.gpuTimeMs).isNull()
        assertThat(snap.cameraStartupMs).isNull()
        assertThat(snap.lastEffectLoadMs).isNull()
        assertThat(snap.javaHeapUsedBytes).isEqualTo(1234L)
        assertThat(snap.nativeHeapAllocatedBytes).isNull()
    }

    @Test
    fun recordsPresentedDroppedAndStartupFromRealMarks() {
        val manager = PerformanceManager(memoryProbe = { 1L }, nativeHeapProbe = { 2L })
        manager.markCameraStart()
        Thread.sleep(5)
        manager.markCameraReady()
        manager.onDropped()
        manager.onFramePresented(timestampNs = 100L, processNs = 2_000_000L, gpuNs = 1_000_000L)
        manager.markEffectLoadNs(4_000_000L)
        val snap = manager.snapshot()
        assertThat(snap.presentedFrames).isEqualTo(1)
        assertThat(snap.droppedFrames).isEqualTo(1)
        assertThat(snap.cameraStartupMs).isGreaterThan(0f)
        assertThat(snap.gpuTimeMs).isEqualTo(1f)
        assertThat(snap.lastEffectLoadMs).isEqualTo(4f)
        assertThat(snap.lastCameraFrameTimestampNs).isEqualTo(100L)
        assertThat(snap.nativeHeapAllocatedBytes).isEqualTo(2L)
        assertThat(snap.frameProcessTimeMs).isGreaterThan(0f)
    }
}
