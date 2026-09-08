package com.aurafx.sdk.gift

import com.aurafx.sdk.vision.TrackingData

/**
 * CPU gift orchestrator. GPU draw is [GiftFxRenderer] on the GL thread.
 */
class GiftFxEngine {
    val manager = GiftFxManager()
    val renderer = GiftFxRenderer()
    private val subjectScratch = FloatArray(3)
    private val drawList = ArrayList<GiftFxInstance>(GiftFxManager.MAX_ACTIVE)

    fun play(giftId: String, nowNs: Long = System.nanoTime()): Long? = manager.play(giftId, nowNs)

    fun replay(nowNs: Long = System.nanoTime()): Long? = manager.replay(nowNs)

    fun stop(instanceId: Long) = manager.stop(instanceId)

    fun stopAll() = manager.stopAll()

    fun reset() {
        manager.reset()
        renderer.resetCpu()
    }

    fun hasActive(): Boolean = manager.activeCount() > 0

    fun tick(nowNs: Long, width: Int, height: Int, tracking: TrackingData): ArrayList<GiftFxInstance> {
        manager.update(nowNs, subjectScratch, width, height, tracking)
        manager.snapshotActive(drawList)
        return drawList
    }

    fun dispose() {
        manager.reset()
        renderer.dispose()
    }

    fun catalog() = GiftFxCatalog.samples
}
