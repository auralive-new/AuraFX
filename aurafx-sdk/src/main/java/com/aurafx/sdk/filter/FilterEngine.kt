package com.aurafx.sdk.filter

import com.aurafx.sdk.api.FilterFinish
import com.aurafx.sdk.api.FilterParameters

class FilterRig {
    private val lock = Any()
    private val params = FilterParameters()

    fun apply(block: FilterParameters.() -> Unit) {
        synchronized(lock) {
            block(params)
            params.clampInPlace()
            resolveKnownId()
        }
    }

    fun setFilter(id: String, intensity: Float): Boolean {
        if (FilterCatalog.require(id) == null) return false
        synchronized(lock) {
            params.id = id
            params.intensity = intensity
            params.clampInPlace()
        }
        return true
    }

    fun clear() {
        synchronized(lock) { params.reset(); params.intensity = 0f; params.id = null }
    }

    fun reset() = clear()

    fun snapshot(): FilterSnapshot = synchronized(lock) {
        params.clampInPlace()
        val def = params.id?.let { FilterCatalog.require(it) }
        FilterSnapshot(
            definition = def,
            intensity = if (def == null) 0f else params.intensity,
            finish = params.finish,
            finishIntensity = params.finishIntensity,
        )
    }

    fun copy(): FilterParameters = synchronized(lock) {
        params.copy()
    }

    private fun resolveKnownId() {
        val id = params.id ?: return
        if (FilterCatalog.require(id) == null) {
            params.id = null
        }
    }
}

data class FilterSnapshot(
    val definition: FilterDefinition?,
    val intensity: Float,
    val finish: FilterFinish,
    val finishIntensity: Float,
) {
    fun isIdentity(): Boolean = definition == null || intensity <= 0f

    fun grain(): Float {
        val base = definition?.grade?.grain ?: 0f
        val extra = if (finish == FilterFinish.Grain || finish == FilterFinish.GrainVignette) {
            0.22f * finishIntensity
        } else 0f
        return (base + extra).coerceIn(0f, 1f)
    }

    fun vignette(): Float {
        val base = definition?.grade?.vignette ?: 0f
        val extra = if (finish == FilterFinish.Vignette || finish == FilterFinish.GrainVignette) {
            0.28f * finishIntensity
        } else 0f
        return (base + extra).coerceIn(0f, 1f)
    }
}

class FilterEngine(private val rig: FilterRig) {
    fun apply(block: FilterParameters.() -> Unit) = rig.apply(block)

    fun setFilter(id: String, intensity: Float): Boolean = rig.setFilter(id, intensity)

    fun clear() = rig.clear()

    fun reset() = rig.reset()

    fun snapshot() = rig.snapshot()

    fun parameters() = rig.copy()

    fun catalog(): List<FilterDefinition> = FilterCatalog.filters
}

class FilterRenderer(private val rig: FilterRig) {
    fun snapshot() = rig.snapshot()
}
