package com.aurafx.sdk.effect

import com.aurafx.sdk.vision.TrackingData

/**
 * Modular GPU effect. Beauty / makeup / filters / AR must implement this in later steps.
 * Step 1 does not ship concrete cosmetic or AR effects.
 */
interface Effect {
    val id: String

    fun onAttach(context: EffectContext)

    fun process(frame: FrameContext, tracking: TrackingData)

    fun onDetach()
}
