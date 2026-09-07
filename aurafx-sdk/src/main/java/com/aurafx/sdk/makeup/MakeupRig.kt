package com.aurafx.sdk.makeup

import com.aurafx.sdk.api.MakeupParameters
import com.aurafx.sdk.api.MakeupPreset

class MakeupRig {
    private val lock = Any()
    private val params = MakeupParameters()

    fun apply(block: MakeupParameters.() -> Unit) {
        synchronized(lock) {
            block(params)
            params.clampInPlace()
        }
    }

    fun preset(preset: MakeupPreset) {
        synchronized(lock) { params.applyPreset(preset) }
    }

    fun reset() {
        synchronized(lock) { params.reset() }
    }

    fun copy(): MakeupParameters = synchronized(lock) {
        MakeupParameters(
            foundation = params.foundation.copy(color = params.foundation.color.copy()),
            concealer = params.concealer.copy(color = params.concealer.color.copy()),
            blush = params.blush.copy(color = params.blush.color.copy()),
            contour = params.contour.copy(color = params.contour.color.copy()),
            highlight = params.highlight.copy(color = params.highlight.color.copy()),
            eyebrow = params.eyebrow.copy(color = params.eyebrow.color.copy()),
            eyeshadow = params.eyeshadow.copy(
                style = params.eyeshadow.style,
                lidColor = params.eyeshadow.lidColor.copy(),
                creaseColor = params.eyeshadow.creaseColor.copy(),
            ),
            eyeliner = params.eyeliner.copy(color = params.eyeliner.color.copy()),
            eyelashes = params.eyelashes.copy(color = params.eyelashes.color.copy()),
            lipstick = params.lipstick.copy(color = params.lipstick.color.copy()),
            lipLiner = params.lipLiner.copy(color = params.lipLiner.color.copy()),
            lipGloss = params.lipGloss.copy(),
            lens = params.lens.copy(),
        )
    }

    fun snapshot(): MakeupParameters = copy().also { it.clampInPlace() }
}

class MakeupEngine(private val rig: MakeupRig) {
    fun apply(block: MakeupParameters.() -> Unit) = rig.apply(block)
    fun preset(preset: MakeupPreset) = rig.preset(preset)
    fun reset() = rig.reset()
    fun snapshot() = rig.snapshot()
}
