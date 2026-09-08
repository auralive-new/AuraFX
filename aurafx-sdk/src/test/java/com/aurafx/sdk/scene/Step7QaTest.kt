package com.aurafx.sdk.scene

import com.aurafx.sdk.api.CapturePolicy
import com.aurafx.sdk.api.HairParameters
import com.aurafx.sdk.beauty.FaceLandmarks
import com.aurafx.sdk.beauty.FaceTopology
import com.aurafx.sdk.capture.AvTimestamps
import com.aurafx.sdk.editor.EditorCapabilities
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HairGroomMeshTest {
    @Test
    fun everyCatalogStyleBuildsFiniteTrackedMesh() {
        val lm = syntheticFace()
        for (style in HairCatalog.styles) {
            val mesh = HairGroomMeshBuilder.build(style.id, lm)
            if (style.id == "hair.style.natural") {
                assertThat(mesh.indices).isEmpty()
            } else {
                assertThat(mesh.indices.size).isGreaterThan(0)
                assertThat(mesh.allFinite()).isTrue()
                assertThat(mesh.vertices.any { !it.isFinite() }).isFalse()
                assertThat(HairGroomRecipes.forStyle(style.id).assetPath()).isEqualTo(style.requiredAsset)
            }
            assertThat(style.productionRealistic).isTrue()
        }
        assertThat(HairCatalog.validate()).isEmpty()
        assertThat(SceneShaders.groomOk()).isTrue()
    }

    @Test
    fun nanLandmarksDoNotEmitNaNVertices() {
        val xy = FloatArray(FaceTopology.LANDMARK_COUNT * 2) { Float.NaN }
        val lm = FaceLandmarks(1, xy, FaceTopology.LANDMARK_COUNT, 0L, false)
        val mesh = HairGroomMeshBuilder.build("hair.style.bob", lm)
        assertThat(mesh.vertices).isEmpty()
        assertThat(mesh.allFinite()).isTrue()
    }

    @Test
    fun headPoseRespondsToYaw() {
        val left = syntheticFace()
        val right = syntheticFace()
        right.xy[FaceTopology.NOSE_TIP * 2] = 0.72f
        val yawL = HairGroomMeshBuilder.yaw(left)
        val yawR = HairGroomMeshBuilder.yaw(right)
        assertThat(yawR).isGreaterThan(yawL)
    }

    @Test
    fun styleWithoutColorStillActivatesPipeline() {
        val p = HairParameters(enabled = true, intensity = 0f, styleId = "hair.style.bob")
        assertThat(p.wantsStyle()).isTrue()
        assertThat(p.isIdentity()).isFalse()
        p.reset()
        assertThat(p.isIdentity()).isTrue()
    }

    private fun syntheticFace(): FaceLandmarks {
        val n = FaceTopology.LANDMARK_COUNT
        val xy = FloatArray(n * 2)
        for (i in 0 until n) {
            xy[i * 2] = 0.35f + 0.3f * (i % 20) / 20f
            xy[i * 2 + 1] = 0.28f + 0.4f * (i % 17) / 17f
        }
        xy[FaceTopology.FOREHEAD * 2] = 0.50f
        xy[FaceTopology.FOREHEAD * 2 + 1] = 0.22f
        xy[FaceTopology.CHIN * 2] = 0.50f
        xy[FaceTopology.CHIN * 2 + 1] = 0.82f
        xy[FaceTopology.LEFT_JAW * 2] = 0.28f
        xy[FaceTopology.LEFT_JAW * 2 + 1] = 0.52f
        xy[FaceTopology.RIGHT_JAW * 2] = 0.72f
        xy[FaceTopology.RIGHT_JAW * 2 + 1] = 0.52f
        xy[FaceTopology.NOSE_TIP * 2] = 0.50f
        xy[FaceTopology.NOSE_TIP * 2 + 1] = 0.48f
        xy[FaceTopology.LEFT_EYE_OUTER * 2] = 0.36f
        xy[FaceTopology.LEFT_EYE_OUTER * 2 + 1] = 0.40f
        xy[FaceTopology.RIGHT_EYE_OUTER * 2] = 0.64f
        xy[FaceTopology.RIGHT_EYE_OUTER * 2 + 1] = 0.40f
        for (idx in FaceTopology.FACE_OVAL) {
            val t = FaceTopology.FACE_OVAL.indexOf(idx) / FaceTopology.FACE_OVAL.size.toFloat()
            xy[idx * 2] = 0.50f + 0.22f * kotlin.math.cos(t * 6.28f)
            xy[idx * 2 + 1] = 0.50f + 0.28f * kotlin.math.sin(t * 6.28f)
        }
        return FaceLandmarks(1, xy, n, 1L, true)
    }
}

class CaptureAndEditorLogicTest {
    @Test
    fun cameraSwitchRefusedWhileRecording() {
        assertThat(CapturePolicy.denySwitchWhileRecording(false)).isNull()
        assertThat(CapturePolicy.denySwitchWhileRecording(true)?.message).contains("refused")
    }

    @Test
    fun timestampsAreMonotonicAfterBump() {
        assertThat(AvTimestamps.relativeUs(2_000_000L, 1_000_000L)).isEqualTo(1000L)
        assertThat(AvTimestamps.monotonic(10, 11)).isTrue()
        assertThat(AvTimestamps.bumpIfNeeded(50, 40)).isEqualTo(51)
        assertThat(AvTimestamps.bumpIfNeeded(50, 80)).isEqualTo(80)
    }

    @Test
    fun editorMarksUnavailableFeaturesInsteadOfFakingThem() {
        assertThat(EditorCapabilities.available("trim")).isTrue()
        assertThat(EditorCapabilities.available("export")).isTrue()
        assertThat(EditorCapabilities.available("video.crop")).isFalse()
        assertThat(EditorCapabilities.available("video.effectReprocess")).isFalse()
        assertThat(com.aurafx.sdk.internal.ResourceLifecycle.owners.size).isAtLeast(8)
    }
}

class PipelineOrderTest {
    @Test
    fun gpuOrderMatchesStep7Contract() {
        assertThat(AuraFxEffectOrder.gpuIds).containsExactly(
            AuraFxEffectOrder.SEGMENTATION,
            AuraFxEffectOrder.BACKGROUND,
            AuraFxEffectOrder.MAKEUP,
            AuraFxEffectOrder.BEAUTY,
            AuraFxEffectOrder.HAIR,
            AuraFxEffectOrder.BODY,
            AuraFxEffectOrder.LIGHTING,
            AuraFxEffectOrder.FILTER,
            AuraFxEffectOrder.AR,
            AuraFxEffectOrder.GIFT,
        ).inOrder()
    }
}
