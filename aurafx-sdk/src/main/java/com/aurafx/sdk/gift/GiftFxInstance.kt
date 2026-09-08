package com.aurafx.sdk.gift

class GiftFxInstance {
    var instanceId: Long = 0L
    var definition: GiftFxDefinition? = null
    var startNs: Long = 0L
    var lastNs: Long = 0L
    var elapsedMs: Float = 0f
    var normalized: Float = 0f
    var phase: GiftFxPhase = GiftFxPhase.Ended
    var envelope: Float = 0f
    var cleanupDone: Boolean = false
    var freezeCaptured: Boolean = false
    var subjectX: Float = 0.5f
    var subjectY: Float = 0.38f
    var subjectIod: Float = 0.16f
    var cloneX: Float = 0.62f
    var cloneY: Float = 0.38f
    var gravitySign: Float = 1f
    val particles = GiftFxParticleSystem()
    val timeline = GiftFxTimeline()
    private val histX = FloatArray(HIST)
    private val histY = FloatArray(HIST)
    private var histWrite = 0
    private var histCount = 0

    fun recycle() {
        instanceId = 0L
        definition = null
        startNs = 0L
        lastNs = 0L
        elapsedMs = 0f
        normalized = 0f
        phase = GiftFxPhase.Ended
        envelope = 0f
        cleanupDone = false
        freezeCaptured = false
        subjectX = 0.5f
        subjectY = 0.38f
        subjectIod = 0.16f
        cloneX = 0.62f
        cloneY = 0.38f
        gravitySign = 1f
        particles.reset()
        histX.fill(0.5f)
        histY.fill(0.38f)
        histWrite = 0
        histCount = 0
    }

    fun start(id: Long, def: GiftFxDefinition, nowNs: Long) {
        recycle()
        instanceId = id
        definition = def
        startNs = nowNs
        lastNs = nowNs
        phase = GiftFxPhase.Start
        particles.reset()
    }

    fun update(nowNs: Long, sx: Float, sy: Float, iod: Float) {
        val def = definition ?: return
        if (startNs == 0L) startNs = nowNs
        val dt = if (lastNs == 0L) 0.016f else ((nowNs - lastNs).coerceIn(1_000_000L, 80_000_000L) / 1e9f)
        lastNs = nowNs
        elapsedMs = ((nowNs - startNs) / 1_000_000f).coerceAtLeast(0f)
        normalized = (elapsedMs / def.durationMs.toFloat()).coerceIn(0f, 1f)
        phase = timeline.phase(normalized)
        envelope = timeline.envelope(normalized)
        subjectX = sx
        subjectY = sy
        subjectIod = iod
        pushHistory(sx, sy)
        val delayed = delayedSubject()
        cloneX = delayed[0] + def.subjectCloneOffsetX()
        cloneY = delayed[1]
        gravitySign = gravityFor(def, normalized)
        val cfg = def.particles
        val emitScale = when (phase) {
            GiftFxPhase.Start -> 0.35f
            GiftFxPhase.Buildup -> 0.75f
            GiftFxPhase.Main -> 1f
            GiftFxPhase.Finale -> 0.55f
            GiftFxPhase.Cleanup -> 0.15f
            GiftFxPhase.Ended -> 0f
        }
        val originX = if (cfg.trail) meteorOriginX(def, normalized, sx) else sx
        val originY = if (cfg.trail) meteorOriginY(def, normalized, sy) else sy
        particles.step(
            dt = dt,
            originX = originX,
            originY = originY,
            gravityY = cfg.gravity * gravitySign,
            emitPerSec = cfg.emitPerSec,
            kind = cfg.kind,
            trailBoost = if (cfg.trail) 1f else 0.2f,
            aliveScale = emitScale * envelope,
        )
        if (phase == GiftFxPhase.Ended || normalized >= 1f) {
            end()
        }
    }

    fun end() {
        if (phase != GiftFxPhase.Ended) {
            phase = GiftFxPhase.Ended
            normalized = 1f
            envelope = 0f
        }
        cleanup()
    }

    fun cleanup() {
        if (cleanupDone) return
        particles.reset()
        freezeCaptured = false
        cleanupDone = true
        phase = GiftFxPhase.Ended
        envelope = 0f
    }

    fun isActive(): Boolean = definition != null && !cleanupDone && phase != GiftFxPhase.Ended

    fun phaseIndex(): Int = timeline.phaseIndex(normalized)

    private fun pushHistory(x: Float, y: Float) {
        histX[histWrite] = x
        histY[histWrite] = y
        histWrite = (histWrite + 1) % HIST
        if (histCount < HIST) histCount++
    }

    private fun delayedSubject(): FloatArray {
        if (histCount < 8) return floatArrayOf(subjectX, subjectY)
        val idx = (histWrite - 8 + HIST) % HIST
        return floatArrayOf(histX[idx], histY[idx])
    }

    private fun gravityFor(def: GiftFxDefinition, t: Float): Float {
        if (def.rendererType != com.aurafx.sdk.api.GiftFxRendererType.GravityFlip) return 1f
        return when {
            t < 0.28f -> 1f
            t < 0.38f -> {
                val u = (t - 0.28f) / 0.10f
                val e = u * u * (3f - 2f * u)
                1f - 2f * e
            }
            t < 0.72f -> -1f
            t < 0.84f -> {
                val u = (t - 0.72f) / 0.12f
                val e = u * u * (3f - 2f * u)
                -1f + 2f * e
            }
            else -> 1f
        }
    }

    private fun meteorOriginX(def: GiftFxDefinition, t: Float, sx: Float): Float {
        if (def.rendererType != com.aurafx.sdk.api.GiftFxRendererType.MeteorCreature) return sx
        return 0.18f + 0.64f * t + 0.08f * kotlin.math.sin(t * 9f)
    }

    private fun meteorOriginY(def: GiftFxDefinition, t: Float, sy: Float): Float {
        if (def.rendererType != com.aurafx.sdk.api.GiftFxRendererType.MeteorCreature) {
            return if (def.rendererType == com.aurafx.sdk.api.GiftFxRendererType.GravityFlip) {
                if (gravitySign >= 0f) 0.08f else 0.92f
            } else sy
        }
        return -0.05f + 1.15f * t
    }

    companion object {
        private const val HIST = 24
    }
}

private fun GiftFxDefinition.subjectCloneOffsetX(): Float =
    if (rendererType == com.aurafx.sdk.api.GiftFxRendererType.HologramClone) 0.18f else 0f
