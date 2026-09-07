package com.aurafx.sdk.effect

import com.aurafx.sdk.performance.PerformanceManager
import com.aurafx.sdk.vision.TrackingData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EffectManagerTest {
    private class RecordingEffect(override val id: String) : Effect {
        var attached = false
        var processed = 0
        override fun onAttach(context: EffectContext) {
            attached = true
        }
        override fun process(frame: FrameContext, tracking: TrackingData) {
            processed += 1
        }
        override fun onDetach() {
            attached = false
        }
    }

    @Test
    fun emptyManagerIsIdentityGraph() {
        val manager = EffectManager(PerformanceManager(memoryProbe = { 0L }))
        assertThat(manager.registeredIds()).isEmpty()
        manager.process(
            FrameContext(
                timestampNs = 1L,
                width = 1,
                height = 1,
                inputTextureId = 0,
                inputIsOes = true,
                texMatrix = FloatArray(16),
                lensFacing = com.aurafx.sdk.api.LensFacing.FRONT,
            ),
            TrackingData.unavailable(1L),
        )
    }

    @Test
    fun registerAndUnregisterWithoutAttachDoesNotPretendToRunGpu() {
        val manager = EffectManager(PerformanceManager(memoryProbe = { 0L }))
        val effect = RecordingEffect("noop")
        manager.register(effect)
        assertThat(manager.registeredIds()).containsExactly("noop")
        assertThat(effect.attached).isFalse()
        manager.unregister("noop")
        assertThat(manager.registeredIds()).isEmpty()
        assertThat(effect.attached).isFalse()
    }
}
