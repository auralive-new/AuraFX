package com.aurafx.sdk.ar

import kotlin.math.cos
import kotlin.math.sin

/**
 * GPU-friendly particle + timeline state. Fixed pools, no per-frame object alloc.
 */
class ARAnimationEngine {
    companion object {
        const val MAX_PARTICLES = 96
        const val STRIDE = 8
    }

    val particles = FloatArray(MAX_PARTICLES * STRIDE)
    var timeSec: Float = 0f
        private set
    private var lastNs: Long = 0L
    private var spawnCarry = 0f

    fun reset() {
        particles.fill(0f)
        timeSec = 0f
        lastNs = 0L
        spawnCarry = 0f
    }

    fun step(timestampNs: Long, emitPerSec: Float, originX: Float, originY: Float, yaw: Float, smile: Float, kind: Int) {
        val dt = if (lastNs == 0L) 0.016f else ((timestampNs - lastNs).coerceIn(1_000_000L, 80_000_000L) / 1e9f)
        lastNs = timestampNs
        timeSec += dt
        for (i in 0 until MAX_PARTICLES) {
            val o = i * STRIDE
            var life = particles[o + 4]
            if (life <= 0f) continue
            life -= dt
            particles[o] += particles[o + 2] * dt
            particles[o + 1] += particles[o + 3] * dt
            particles[o + 3] += 0.12f * dt
            particles[o + 4] = life
            if (!particles[o].isFinite()) particles[o] = originX
            if (!particles[o + 1].isFinite()) particles[o + 1] = originY
        }
        spawnCarry += emitPerSec * dt * (0.35f + smile)
        var toSpawn = spawnCarry.toInt()
        spawnCarry -= toSpawn
        var slot = 0
        while (toSpawn > 0 && slot < MAX_PARTICLES) {
            val o = slot * STRIDE
            if (particles[o + 4] <= 0f) {
                val a = hash(slot + timeSec) * 6.283f
                val sp = 0.08f + 0.12f * hash(slot * 3f + 1f)
                particles[o] = originX
                particles[o + 1] = originY
                particles[o + 2] = cos(a) * sp + yaw * 0.05f
                particles[o + 3] = sin(a) * sp - 0.12f
                particles[o + 4] = 0.6f + 0.7f * hash(slot + 9f)
                particles[o + 5] = particles[o + 4]
                particles[o + 6] = 0.012f + 0.02f * hash(slot + 4f)
                particles[o + 7] = kind.toFloat()
                toSpawn--
            }
            slot++
        }
    }

    fun easeLoop(speed: Float): Float {
        val t = (timeSec * speed)
        val f = t - t.toInt()
        return f * f * (3f - 2f * f)
    }

    fun bounce(smile: Float): Float = 1f + 0.07f * sin(timeSec * 9f) * smile

    private fun hash(x: Float): Float {
        val s = sin(x * 12.9898f) * 43758.5453f
        return s - kotlin.math.floor(s.toDouble()).toFloat()
    }
}

data class ARAnimationDefinition(
    val looping: Boolean,
    val emitPerSec: Float,
    val particleKind: Int,
    val expressionBoost: String,
    val scalePulse: Boolean,
) {
    fun valid(): Boolean = emitPerSec.isFinite() && emitPerSec >= 0f && particleKind >= 0
}
