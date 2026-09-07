package com.aurafx.sdk.ar

import com.aurafx.sdk.api.ARParameters

class ARRig {
    private val lock = Any()
    private val params = ARParameters()

    fun apply(block: ARParameters.() -> Unit) = synchronized(lock) {
        block(params)
        params.clampInPlace()
        if (params.effectId != null && AREffectCatalog.require(params.effectId!!) == null) {
            params.effectId = null
        }
    }

    fun set(id: String, intensity: Float): Boolean {
        if (AREffectCatalog.require(id) == null) return false
        synchronized(lock) {
            params.effectId = id
            params.intensity = intensity
            params.clampInPlace()
        }
        return true
    }

    fun clear() = synchronized(lock) { params.reset(); params.intensity = 0f; params.effectId = null }

    fun snapshot(): ARParameters = synchronized(lock) { params.copy().also { it.clampInPlace() } }
}

class AREngine(private val rig: ARRig) {
    fun apply(block: ARParameters.() -> Unit) = rig.apply(block)
    fun setAREffect(id: String, intensity: Float = 0.85f): Boolean = rig.set(id, intensity)
    fun setAREffectIntensity(intensity: Float) = rig.apply { this.intensity = intensity }
    fun clear() = rig.clear()
    fun reset() = rig.clear()
    fun snapshot() = rig.snapshot()
    fun catalog() = AREffectCatalog.effects
}

class AREffectManager(private val engine: AREngine) {
    fun active(): AREffectDefinition? = engine.snapshot().effectId?.let { AREffectCatalog.require(it) }
    fun switchTo(id: String, intensity: Float): Boolean = engine.setAREffect(id, intensity)
}
