package com.aurafx.sdk.pipeline

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class FramePipelineTest {
    @Test
    fun admitsSingleFrameAndRejectsWhileBusy() {
        val pipeline = FramePipeline()
        assertThat(pipeline.tryBegin()).isTrue()
        assertThat(pipeline.tryBegin()).isFalse()
        assertThat(pipeline.droppedCount()).isEqualTo(1)
        pipeline.end()
        assertThat(pipeline.tryBegin()).isTrue()
        pipeline.end()
        assertThat(pipeline.admittedCount()).isEqualTo(2)
    }

    @Test
    fun concurrentBeginOnlyAllowsOne() {
        val pipeline = FramePipeline()
        val started = AtomicInteger()
        val gate = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(8)
        repeat(32) {
            pool.execute {
                gate.await()
                if (pipeline.tryBegin()) started.incrementAndGet()
            }
        }
        gate.countDown()
        pool.shutdown()
        pool.awaitTermination(2, TimeUnit.SECONDS)
        assertThat(started.get()).isEqualTo(1)
        assertThat(pipeline.droppedCount()).isEqualTo(31)
        pipeline.end()
    }
}
