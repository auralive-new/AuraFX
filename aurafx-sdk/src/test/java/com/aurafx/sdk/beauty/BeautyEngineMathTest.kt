package com.aurafx.sdk.beauty

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BeautyRigTest {
    @Test
    fun intensityZeroIsIdentitySnapshot() {
        val rig = BeautyRig()
        assertThat(rig.snapshot().isIdentity()).isTrue()
        assertThat(rig.snapshot().skinIdentity()).isTrue()
        assertThat(rig.snapshot().shapeIdentity()).isTrue()
    }

    @Test
    fun slidersChangeSnapshotNotJustCopies() {
        val rig = BeautyRig()
        rig.applyBeauty {
            fineSmooth = 0.4f
            toothWhiten = 0.3f
            eyeEnlarge = 0.5f
        }
        val snap = rig.snapshot()
        assertThat(snap.fineSmooth).isWithin(1e-4f).of(0.4f)
        assertThat(snap.toothWhiten).isWithin(1e-4f).of(0.3f)
        assertThat(snap.eyeEnlarge).isWithin(1e-4f).of(0.5f)
        assertThat(snap.isIdentity()).isFalse()
    }

    @Test
    fun resetRestoresDefaults() {
        val rig = BeautyRig()
        rig.applySkin { smoothness = 1f; whiten = 1f }
        rig.applyFaceShape { vFace = 1f; eyeDistance = 0.8f }
        rig.reset()
        val snap = rig.snapshot()
        assertThat(snap.isIdentity()).isTrue()
        assertThat(snap.texturePreserve).isWithin(1e-4f).of(0.7f)
        assertThat(snap.naturalSkin).isWithin(1e-4f).of(0.4f)
        assertThat(snap.tone).isWithin(1e-4f).of(0.5f)
    }

    @Test
    fun invalidParametersAreClamped() {
        val rig = BeautyRig()
        rig.applyBeauty {
            fineSmooth = 4f
            eyeDistance = -3f
            toothWhiten = -1f
        }
        val b = rig.copyBeauty()
        assertThat(b.fineSmooth).isEqualTo(1f)
        assertThat(b.eyeDistance).isEqualTo(-1f)
        assertThat(b.toothWhiten).isEqualTo(0f)
    }

    @Test
    fun skinAndBeautyShareWhiten() {
        val rig = BeautyRig()
        rig.applySkin { whiten = 0.6f }
        assertThat(rig.copyBeauty().whiten).isWithin(1e-4f).of(0.6f)
    }
}

class WarpFieldTest {
    private fun syntheticFace(): FaceLandmarks {
        val xy = FloatArray(FaceTopology.LANDMARK_COUNT * 2)
        for (i in 0 until FaceTopology.LANDMARK_COUNT) {
            xy[i * 2] = 0.3f + (i % 20) * 0.02f
            xy[i * 2 + 1] = 0.25f + (i / 20) * 0.02f
        }
        xy[1 * 2] = 0.5f
        xy[1 * 2 + 1] = 0.45f
        xy[FaceTopology.CHIN * 2] = 0.5f
        xy[FaceTopology.CHIN * 2 + 1] = 0.82f
        xy[FaceTopology.LEFT_EYE_OUTER * 2] = 0.38f
        xy[FaceTopology.LEFT_EYE_OUTER * 2 + 1] = 0.4f
        xy[FaceTopology.RIGHT_EYE_OUTER * 2] = 0.62f
        xy[FaceTopology.RIGHT_EYE_OUTER * 2 + 1] = 0.4f
        return FaceLandmarks(1, xy, FaceTopology.LANDMARK_COUNT, 0L, true)
    }

    @Test
    fun zeroShapeProducesIdentityMesh() {
        val mesh = WarpField.buildMesh(syntheticFace(), BeautyRig().snapshot(), FloatArray(0))
        assertThat(WarpField.isIdentityMesh(mesh)).isTrue()
        for (v in mesh) {
            assertThat(v.isFinite()).isTrue()
        }
    }

    @Test
    fun eyeEnlargeChangesInteriorNotWholeFrameScale() {
        val rig = BeautyRig()
        rig.applyFaceShape { eyeEnlarge = 0.8f }
        val mesh = WarpField.buildMesh(syntheticFace(), rig.snapshot(), FloatArray(0))
        assertThat(WarpField.isIdentityMesh(mesh)).isFalse()
        val stride = 6
        val corner = 0
        assertThat(kotlin.math.abs(mesh[corner + 4])).isLessThan(0.01f)
        assertThat(kotlin.math.abs(mesh[corner + 5])).isLessThan(0.01f)
        var max = 0f
        var i = 4
        while (i < mesh.size) {
            max = maxOf(max, kotlin.math.abs(mesh[i]), kotlin.math.abs(mesh[i + 1]))
            assertThat(mesh[i].isFinite()).isTrue()
            i += stride
        }
        assertThat(max).isGreaterThan(0f)
        assertThat(max).isAtMost(0.12f)
    }

    @Test
    fun indicesStayInVertexRange() {
        val idx = WarpField.buildIndices()
        val verts = WarpField.vertexCount()
        for (i in idx) {
            assertThat(i).isAtLeast(0)
            assertThat(i).isLessThan(verts)
        }
        assertThat(idx.size % 3).isEqualTo(0)
    }
}

class RegionMaskBuilderTest {
    @Test
    fun emptyLandmarksStayZero() {
        val px = RegionMaskBuilder.build(null)
        assertThat(RegionMaskBuilder.channelSum(px, 0)).isEqualTo(0)
        assertThat(px.size).isEqualTo(RegionMaskBuilder.SIZE * RegionMaskBuilder.SIZE * 4)
    }

    @Test
    fun ovalProducesSkinInsideBounds() {
        val xy = FloatArray(FaceTopology.LANDMARK_COUNT * 2) { 0.5f }
        for ((i, idx) in FaceTopology.FACE_OVAL.withIndex()) {
            val a = i / FaceTopology.FACE_OVAL.size.toFloat() * (Math.PI * 2).toFloat()
            xy[idx * 2] = 0.5f + 0.2f * kotlin.math.cos(a)
            xy[idx * 2 + 1] = 0.5f + 0.25f * kotlin.math.sin(a)
        }
        val lm = FaceLandmarks(1, xy, FaceTopology.LANDMARK_COUNT, 1L, true)
        val px = RegionMaskBuilder.build(lm)
        assertThat(RegionMaskBuilder.channelSum(px, 0)).isGreaterThan(0)
    }

    @Test
    fun pointInPolygonMatchesSquare() {
        val xs = floatArrayOf(0f, 1f, 1f, 0f)
        val ys = floatArrayOf(0f, 0f, 1f, 1f)
        assertThat(RegionMaskBuilder.pointInPolygon(0.5f, 0.5f, xs, ys)).isTrue()
        assertThat(RegionMaskBuilder.pointInPolygon(1.5f, 0.5f, xs, ys)).isFalse()
    }
}

class LandmarkSmootherTest {
    @Test
    fun smootherHasNoNaNAndDampsJitter() {
        val a = FaceLandmarks(1, floatArrayOf(0.1f, 0.1f, 0.2f, 0.2f), 2, 0L, false)
        val b = FaceLandmarks(1, floatArrayOf(0.12f, 0.11f, 0.21f, 0.19f), 2, 1L, false)
        val s = LandmarkSmoother.smooth(a, b, alpha = 0.4f)
        assertThat(s.allFinite()).isTrue()
        assertThat(s.x(0)).isGreaterThan(0.1f)
        assertThat(s.x(0)).isLessThan(0.12f)
    }
}

class BeautyShaderSourceTest {
    @Test
    fun skinShaderDeclaresEveryControlUniform() {
        for (u in BeautyShaders.requiredSkinUniforms()) {
            assertThat(BeautyShaders.FRAG_SKIN).contains(u)
        }
        assertThat(BeautyShaders.FRAG_SKIN.count { it == '{' })
            .isEqualTo(BeautyShaders.FRAG_SKIN.count { it == '}' })
        assertThat(BeautyShaders.VERT_WARP.count { it == '{' })
            .isEqualTo(BeautyShaders.VERT_WARP.count { it == '}' })
    }
}
