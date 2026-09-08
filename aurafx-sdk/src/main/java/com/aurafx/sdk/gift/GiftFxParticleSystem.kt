package com.aurafx.sdk.gift

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Fixed-capacity particle buffer. No per-frame allocations.
 * Layout stride 8: x, y, vx, vy, life, maxLife, size, kind
 */
class GiftFxParticleSystem(
    capacity: Int = MAX,
) {
    companion object {
        const val MAX = 192
        const val STRIDE = 8
    }

    private val cap = capacity.coerceIn(8, MAX)
    val data = FloatArray(cap * STRIDE)
    private var spawnCarry = 0f

    fun reset() {
        data.fill(0f)
        spawnCarry = 0f
    }

    fun step(
        dt: Float,
        originX: Float,
        originY: Float,
        gravityY: Float,
        emitPerSec: Float,
        kind: Int,
        trailBoost: Float,
        aliveScale: Float,
    ) {
        val dtc = dt.coerceIn(0.001f, 0.05f)
        for (i in 0 until cap) {
            val o = i * STRIDE
            var life = data[o + 4]
            if (life <= 0f) continue
            life -= dtc
            data[o] += data[o + 2] * dtc
            data[o + 1] += data[o + 3] * dtc
            data[o + 3] += gravityY * dtc
            data[o + 4] = life
            if (!data[o].isFinite()) data[o] = originX
            if (!data[o + 1].isFinite()) data[o + 1] = originY
        }
        if (aliveScale <= 0.01f) return
        spawnCarry += emitPerSec * dtc * aliveScale
        var toSpawn = spawnCarry.toInt()
        spawnCarry -= toSpawn
        var slot = 0
        while (toSpawn > 0 && slot < cap) {
            val o = slot * STRIDE
            if (data[o + 4] <= 0f) {
                val a = hash(slot + originX * 17f) * 6.283185f
                val sp = 0.05f + 0.18f * hash(slot * 3f + originY)
                data[o] = originX + (hash(slot + 2f) - 0.5f) * 0.04f
                data[o + 1] = originY + (hash(slot + 5f) - 0.5f) * 0.04f
                data[o + 2] = cos(a) * sp * (0.4f + trailBoost)
                data[o + 3] = sin(a) * sp - gravityY * 0.12f
                val life = 0.45f + 0.85f * hash(slot + 9f)
                data[o + 4] = life
                data[o + 5] = life
                data[o + 6] = 0.01f + 0.03f * hash(slot + 4f)
                data[o + 7] = kind.toFloat()
                toSpawn--
            }
            slot++
        }
    }

    fun capacity(): Int = cap

    fun forEachAlive(visitor: (x: Float, y: Float, life01: Float, size: Float, kind: Int) -> Unit) {
        for (i in 0 until cap) {
            val o = i * STRIDE
            val life = data[o + 4]
            if (life <= 0f) continue
            val max = data[o + 5].coerceAtLeast(1e-4f)
            visitor(data[o], data[o + 1], (life / max).coerceIn(0f, 1f), data[o + 6], data[o + 7].toInt())
        }
    }

    private fun hash(x: Float): Float {
        val s = sin(x * 12.9898f) * 43758.5453f
        return s - floor(s.toDouble()).toFloat()
    }
}
