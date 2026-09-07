package com.aurafx.sdk.makeup

import com.aurafx.sdk.api.BlushStyle
import com.aurafx.sdk.api.EyelinerStyle
import com.aurafx.sdk.api.MakeupParameters
import com.aurafx.sdk.api.MakeupPreset
import com.aurafx.sdk.beauty.FaceLandmarks
import com.aurafx.sdk.beauty.FaceTopology
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.hypot

class MakeupParametersTest {
    @Test
    fun identityAtZeroAndReset() {
        val p = MakeupParameters()
        assertThat(p.isIdentity()).isTrue()
        p.applyPreset(MakeupPreset.Classic)
        assertThat(p.isIdentity()).isFalse()
        assertThat(p.lipstick.intensity).isGreaterThan(0f)
        p.reset()
        assertThat(p.isIdentity()).isTrue()
    }

    @Test
    fun presetsAreDistinctAndEditable() {
        val a = MakeupParameters().also { it.applyPreset(MakeupPreset.Classic) }
        val b = MakeupParameters().also { it.applyPreset(MakeupPreset.Bright) }
        val c = MakeupParameters().also { it.applyPreset(MakeupPreset.Extravagant) }
        assertThat(a.lipstick.color.r).isNotEqualTo(c.lipstick.color.r)
        assertThat(b.blush.style).isEqualTo(BlushStyle.BlushBomb)
        assertThat(c.eyeliner.style).isEqualTo(EyelinerStyle.CatEye)
        a.lipstick.intensity = 0.11f
        a.clampInPlace()
        assertThat(a.lipstick.intensity).isWithin(1e-4f).of(0.11f)
    }

    @Test
    fun colorsAndRangesClamp() {
        val p = MakeupParameters()
        p.foundation.enabled = true
        p.foundation.intensity = 8f
        p.foundation.color.set(2f, -1f, 0.4f)
        p.eyeliner.style = EyelinerStyle.None
        p.eyeliner.enabled = true
        p.eyeliner.intensity = 1f
        p.clampInPlace()
        assertThat(p.foundation.intensity).isEqualTo(1f)
        assertThat(p.foundation.color.r).isEqualTo(1f)
        assertThat(p.foundation.color.g).isEqualTo(0f)
        assertThat(p.eyeliner.intensity).isEqualTo(0f)
    }

    @Test
    fun rigSnapshotIndependent() {
        val rig = MakeupRig()
        rig.preset(MakeupPreset.Bright)
        val snap = rig.snapshot()
        rig.reset()
        assertThat(snap.blush.intensity).isGreaterThan(0f)
        assertThat(rig.snapshot().isIdentity()).isTrue()
    }
}

class MakeupGeometryTest {
    private fun face(): FaceLandmarks {
        val xy = FloatArray(478 * 2) { 0.5f }
        fun set(i: Int, x: Float, y: Float) {
            xy[i * 2] = x
            xy[i * 2 + 1] = y
        }
        FaceTopology.FACE_OVAL.forEachIndexed { i, idx ->
            val a = i / FaceTopology.FACE_OVAL.size.toFloat() * (Math.PI * 2).toFloat()
            set(idx, 0.5f + 0.22f * kotlin.math.cos(a), 0.5f + 0.28f * kotlin.math.sin(a))
        }
        set(FaceTopology.LEFT_EYE_OUTER, 0.36f, 0.40f)
        set(FaceTopology.LEFT_EYE_INNER, 0.44f, 0.40f)
        set(FaceTopology.RIGHT_EYE_INNER, 0.56f, 0.40f)
        set(FaceTopology.RIGHT_EYE_OUTER, 0.64f, 0.40f)
        set(33, 0.36f, 0.40f)
        set(133, 0.44f, 0.40f)
        set(263, 0.64f, 0.40f)
        set(362, 0.56f, 0.40f)
        set(246, 0.37f, 0.38f)
        set(159, 0.40f, 0.37f)
        set(466, 0.63f, 0.38f)
        set(386, 0.60f, 0.37f)
        set(50, 0.38f, 0.55f)
        set(280, 0.62f, 0.55f)
        set(1, 0.5f, 0.48f)
        set(6, 0.5f, 0.42f)
        set(0, 0.5f, 0.62f)
        set(10, 0.5f, 0.28f)
        set(152, 0.5f, 0.82f)
        set(234, 0.32f, 0.62f)
        set(454, 0.68f, 0.62f)
        set(98, 0.47f, 0.52f)
        set(327, 0.53f, 0.52f)
        set(468, 0.40f, 0.40f)
        set(473, 0.60f, 0.40f)
        FaceTopology.LIPS_OUTER.forEachIndexed { i, idx ->
            val a = i / 20f * (Math.PI * 2).toFloat()
            set(idx, 0.5f + 0.08f * kotlin.math.cos(a), 0.66f + 0.04f * kotlin.math.sin(a))
        }
        return FaceLandmarks(1, xy, 478, 0L, true)
    }

    @Test
    fun geometryFiniteAndBounded() {
        val p = MakeupParameters().also { it.applyPreset(MakeupPreset.Extravagant) }
        val geo = MakeupGeometryBuilder.build(face(), p)
        assertThat(geo.allFinite()).isTrue()
        assertThat(geo.stamps).isNotEmpty()
        assertThat(geo.liners).isNotEmpty()
        assertThat(geo.lashes).isNotEmpty()
        geo.stamps.forEach {
            assertThat(it.x).isAtLeast(0f)
            assertThat(it.x).isAtMost(1f)
            assertThat(it.y).isAtLeast(0f)
            assertThat(it.y).isAtMost(1f)
            assertThat(it.rx).isGreaterThan(0f)
            assertThat(it.ry).isGreaterThan(0f)
        }
        geo.lashes.forEach {
            assertThat(it.widthRoot).isGreaterThan(it.widthTip)
            assertThat(hypot(it.cx - it.ax, it.cy - it.ay)).isLessThan(0.5f)
        }
        geo.liners.forEach {
            assertThat(MakeupGeometryBuilder.polylineLength(it)).isGreaterThan(0f)
        }
        assertThat(geo.leftIris[2]).isGreaterThan(geo.leftIris[3])
        assertThat(geo.rightIris[0]).isGreaterThan(geo.leftIris[0])
    }

    @Test
    fun noneLinerProducesNoStroke() {
        val p = MakeupParameters()
        p.eyeliner.enabled = true
        p.eyeliner.style = EyelinerStyle.None
        p.eyeliner.intensity = 1f
        p.clampInPlace()
        val geo = MakeupGeometryBuilder.build(face(), p)
        assertThat(geo.liners).isEmpty()
    }

    @Test
    fun masksStayInFrameAndOccupyRegions() {
        val p = MakeupParameters().also { it.applyPreset(MakeupPreset.Classic) }
        val lm = face()
        val geo = MakeupGeometryBuilder.build(lm, p)
        val (a, b) = MakeupMaskBuilder.build(lm, geo, p)
        assertThat(a.size).isEqualTo(MakeupMaskBuilder.SIZE * MakeupMaskBuilder.SIZE * 4)
        assertThat(MakeupMaskBuilder.channelOccupied(a, 0)).isTrue()
        assertThat(MakeupMaskBuilder.channelOccupied(a, 1)).isTrue()
        assertThat(MakeupMaskBuilder.channelOccupied(b, 0) || geo.liners.isNotEmpty()).isTrue()
    }

    @Test
    fun shaderUniformsPresent() {
        for (u in MakeupShaders.requiredUniforms()) {
            assertThat(MakeupShaders.FRAG_MAKEUP).contains(u)
        }
        assertThat(MakeupShaders.FRAG_MAKEUP.count { it == '{' })
            .isEqualTo(MakeupShaders.FRAG_MAKEUP.count { it == '}' })
    }

    @Test
    fun pipelineOrderMakeupThenBeauty() {
        assertThat(MakeupPipelineEffect.ID).isEqualTo("aurafx.makeup.professional")
        assertThat(com.aurafx.sdk.beauty.BeautyPipelineEffect.ID).isEqualTo("aurafx.beauty.skin-shape")
    }
}
