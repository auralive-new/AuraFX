package com.aurafx.sdk.scene

import com.aurafx.sdk.api.BackgroundParameters
import com.aurafx.sdk.api.BodyParameters
import com.aurafx.sdk.api.HairParameters
import com.aurafx.sdk.api.LightingParameters
import com.aurafx.sdk.vision.PoseBody
import com.aurafx.sdk.vision.SegmentationMask
import com.aurafx.sdk.vision.TrackingData
import com.aurafx.sdk.vision.VisionFrame

class BackgroundRig {
    private val lock = Any()
    private val params = BackgroundParameters()
    fun apply(block: BackgroundParameters.() -> Unit) = synchronized(lock) {
        block(params); params.clampInPlace()
    }
    fun reset() = synchronized(lock) { params.reset() }
    fun snapshot(): BackgroundParameters = synchronized(lock) { params.copy().also { it.clampInPlace() } }
}

class HairRig {
    private val lock = Any()
    private val params = HairParameters()
    fun apply(block: HairParameters.() -> Unit) = synchronized(lock) {
        block(params); params.clampInPlace()
    }
    fun reset() = synchronized(lock) { params.reset() }
    fun snapshot(): HairParameters = synchronized(lock) { params.copy().also { it.clampInPlace() } }
}

class BodyRig {
    private val lock = Any()
    private val params = BodyParameters()
    fun apply(block: BodyParameters.() -> Unit) = synchronized(lock) {
        block(params); params.clampInPlace()
    }
    fun reset() = synchronized(lock) { params.reset() }
    fun snapshot(): BodyParameters = synchronized(lock) { params.copy().also { it.clampInPlace() } }
}

class LightingRig {
    private val lock = Any()
    private val params = LightingParameters()
    fun apply(block: LightingParameters.() -> Unit) = synchronized(lock) {
        block(params); params.clampInPlace()
    }
    fun reset() = synchronized(lock) { params.reset() }
    fun snapshot(): LightingParameters = synchronized(lock) { params.copy().also { it.clampInPlace() } }
}

class BackgroundEngine(private val rig: BackgroundRig) {
    fun apply(block: BackgroundParameters.() -> Unit) = rig.apply(block)
    fun reset() = rig.reset()
    fun snapshot() = rig.snapshot()
    fun set(id: String, intensity: Float): Boolean {
        if (BackgroundCatalog.require(id) == null) return false
        rig.apply { enabled = true; this.id = id; this.intensity = intensity }
        return true
    }
}

class HairEngine(private val rig: HairRig) {
    fun apply(block: HairParameters.() -> Unit) = rig.apply(block)
    fun reset() = rig.reset()
    fun snapshot() = rig.snapshot()
}

class BodyEngine(private val rig: BodyRig) {
    fun apply(block: BodyParameters.() -> Unit) = rig.apply(block)
    fun reset() = rig.reset()
    fun snapshot() = rig.snapshot()
}

class LightingEngine(private val rig: LightingRig) {
    fun apply(block: LightingParameters.() -> Unit) = rig.apply(block)
    fun reset() = rig.reset()
    fun snapshot() = rig.snapshot()
}

class BackgroundMaskProvider {
    fun from(tracking: TrackingData): SegmentationMask? = tracking.segmentation?.takeIf { it.inBounds() }
}

class HairMaskProvider {
    fun from(tracking: TrackingData): SegmentationMask? =
        tracking.segmentation?.takeIf { it.inBounds() && it.hairCoverage > 0.001f }
}

class BodySegmentationProvider {
    fun from(tracking: TrackingData): SegmentationMask? =
        tracking.segmentation?.takeIf { it.inBounds() && it.bodyCoverage > 0.001f }

    fun segment(frame: VisionFrame): SegmentationMask? = null
}

class PoseProvider {
    fun from(tracking: TrackingData): PoseBody? = tracking.pose.firstOrNull()?.takeIf { it.isValid() }
    fun detect(frame: VisionFrame): List<PoseBody> = emptyList()
}
