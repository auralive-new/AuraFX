package com.aurafx.sdk.gift

import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong

/**
 * Frame-safe multi-gift lifecycle. Play/stop only mutates gift instances.
 * Does not bind CameraX or restart the frame pipeline.
 */
class GiftFxManager {
    companion object {
        const val MAX_ACTIVE = 6
        const val REBINDS_CAMERA_ON_PLAY_OR_STOP = false
    }

    private val lock = Any()
    private val active = ArrayList<GiftFxInstance>(MAX_ACTIVE)
    private val pool = ArrayDeque<GiftFxInstance>()
    private val ids = AtomicLong(1L)
    @Volatile var lastGiftId: String? = null
        private set

    fun catalog(): List<GiftFxDefinition> = GiftFxCatalog.samples

    fun play(giftId: String, nowNs: Long): Long? {
        val def = GiftFxCatalog.require(giftId) ?: return null
        synchronized(lock) {
            if (active.size >= MAX_ACTIVE) {
                val oldest = active.removeAt(0)
                oldest.end()
                pool.addLast(oldest)
            }
            val inst = pool.pollFirst() ?: GiftFxInstance()
            val id = ids.getAndIncrement()
            inst.start(id, def, nowNs)
            active.add(inst)
            lastGiftId = giftId
            return id
        }
    }

    fun replay(nowNs: Long): Long? {
        val id = lastGiftId ?: return null
        return play(id, nowNs)
    }

    fun stop(instanceId: Long) {
        synchronized(lock) {
            val it = active.iterator()
            while (it.hasNext()) {
                val inst = it.next()
                if (inst.instanceId == instanceId) {
                    inst.end()
                    it.remove()
                    pool.addLast(inst)
                    break
                }
            }
        }
    }

    fun stopAll() {
        synchronized(lock) {
            for (inst in active) {
                inst.end()
                pool.addLast(inst)
            }
            active.clear()
        }
    }

    fun reset() {
        synchronized(lock) {
            stopAllLocked()
            lastGiftId = null
        }
    }

    private fun stopAllLocked() {
        for (inst in active) {
            inst.end()
            pool.addLast(inst)
        }
        active.clear()
    }

    fun update(nowNs: Long, subjectScratch: FloatArray, width: Int, height: Int, tracking: com.aurafx.sdk.vision.TrackingData) {
        synchronized(lock) {
            var i = 0
            while (i < active.size) {
                val inst = active[i]
                val def = inst.definition
                if (def == null) {
                    active.removeAt(i)
                    continue
                }
                GiftFxCoordinates.subjectUv(tracking, def.anchor, width, height, subjectScratch)
                inst.update(nowNs, subjectScratch[0], subjectScratch[1], subjectScratch[2])
                if (!inst.isActive()) {
                    active.removeAt(i)
                    pool.addLast(inst)
                } else {
                    i++
                }
            }
        }
    }

    fun snapshotActive(into: ArrayList<GiftFxInstance>) {
        synchronized(lock) {
            into.clear()
            into.addAll(active)
        }
    }

    fun activeCount(): Int = synchronized(lock) { active.size }

    fun activeGiftIds(): List<String> = synchronized(lock) {
        active.mapNotNull { it.definition?.giftId }
    }

    fun activeInstanceIds(): List<Long> = synchronized(lock) {
        active.map { it.instanceId }
    }
}

/**
 * Play/stop/reset never request a CameraX rebind. Effect switching stays on the
 * existing one-camera GPU graph.
 */
object GiftFxCameraPolicy {
    const val PLAY_STOP_RESTARTS_CAMERA = false
    const val INTRODUCES_SECOND_CAMERAX_BIND = false
}
