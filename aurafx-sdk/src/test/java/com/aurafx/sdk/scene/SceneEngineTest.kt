package com.aurafx.sdk.scene

import com.aurafx.sdk.api.BodyParameters
import com.aurafx.sdk.api.HairColorId
import com.aurafx.sdk.api.HairStyleCapability
import com.aurafx.sdk.api.LightingMode
import com.aurafx.sdk.api.LightingParameters
import com.aurafx.sdk.vision.PoseBody
import com.aurafx.sdk.vision.PoseIndex
import com.aurafx.sdk.vision.SceneMaskPacker
import com.aurafx.sdk.vision.SelfieClass
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SceneCatalogTest {
    @Test
    fun catalogsAreValid() {
        assertThat(BackgroundCatalog.validate()).isEmpty()
        assertThat(HairCatalog.validate()).isEmpty()
        assertThat(BackgroundCatalog.items).hasSize(20)
        assertThat(HairCatalog.styles).hasSize(13)
        assertThat(HairCatalog.colors).hasSize(12)
        assertThat(BodyCatalog.items).hasSize(6)
        assertThat(LightingCatalog.modes).containsExactlyElementsIn(LightingMode.entries)
        val ids = BackgroundCatalog.items.map { it.id }
        assertThat(ids.toSet()).hasSize(20)
        assertThat(HairCatalog.style("hair.style.natural")!!.productionRealistic).isTrue()
        assertThat(HairCatalog.styles.count { it.capability == HairStyleCapability.RequiresGroomAsset }).isEqualTo(12)
        assertThat(AuraFxEffectOrder.gpuIds).containsExactly(
            AuraFxEffectOrder.SEGMENTATION,
            AuraFxEffectOrder.BACKGROUND,
            AuraFxEffectOrder.MAKEUP,
            AuraFxEffectOrder.BEAUTY,
            AuraFxEffectOrder.HAIR,
            AuraFxEffectOrder.BODY,
            AuraFxEffectOrder.LIGHTING,
            AuraFxEffectOrder.FILTER,
        ).inOrder()
    }
}

class SceneMaskTest {
    @Test
    fun packSeparatesHairFromBackgroundAndFace() {
        val w = 8
        val h = 8
        val cat = ByteArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                cat[y * w + x] = when {
                    x < 2 -> SelfieClass.BACKGROUND.toByte()
                    x < 4 -> SelfieClass.HAIR.toByte()
                    x < 6 -> SelfieClass.FACE_SKIN.toByte()
                    else -> SelfieClass.CLOTHES.toByte()
                }
            }
        }
        val mask = SceneMaskPacker.packCategory(w, h, cat, mirrorX = false)
        assertThat(mask.inBounds()).isTrue()
        assertThat(mask.personCoverage).isGreaterThan(0.5f)
        assertThat(mask.hairCoverage).isGreaterThan(0.1f)
        assertThat(mask.faceCoverage).isGreaterThan(0.1f)
        assertThat(mask.bodyCoverage).isGreaterThan(0.1f)
        val midHair = ((3 * w + 3) * 4)
        assertThat(mask.packedRgba[midHair + 1].toInt() and 0xff).isGreaterThan(200)
        val facePx = ((3 * w + 5) * 4)
        assertThat(mask.packedRgba[facePx + 1].toInt() and 0xff).isLessThan(80)
        val mirrored = SceneMaskPacker.packCategory(w, h, cat, mirrorX = true)
        assertThat(mirrored.packedRgba[0].toInt() and 0xff).isGreaterThan(200)
    }

    @Test
    fun resetAndUnknownBackground() {
        val bg = BackgroundEngine(BackgroundRig())
        assertThat(bg.set("missing", 1f)).isFalse()
        assertThat(bg.snapshot().isIdentity()).isTrue()
        assertThat(bg.set("bg.blur.soft", 0.4f)).isTrue()
        assertThat(bg.snapshot().enabled).isTrue()
        bg.reset()
        assertThat(bg.snapshot().isIdentity()).isTrue()
        val hair = HairEngine(HairRig())
        hair.apply {
            enabled = true
            intensity = 8f
            color = HairColorId.Pink
        }
        assertThat(hair.snapshot().intensity).isEqualTo(1f)
        hair.reset()
        assertThat(hair.snapshot().isIdentity()).isTrue()
    }
}

class BodyWarpTest {
    @Test
    fun warpIsBoundedAndFinite() {
        val pose = PoseBody(1, FloatArray(PoseIndex.COUNT * 2) { 0.5f })
        pose.landmarksNormalized[PoseIndex.LEFT_HIP * 2] = 0.38f
        pose.landmarksNormalized[PoseIndex.RIGHT_HIP * 2] = 0.62f
        pose.landmarksNormalized[PoseIndex.LEFT_SHOULDER * 2] = 0.32f
        pose.landmarksNormalized[PoseIndex.RIGHT_SHOULDER * 2] = 0.68f
        assertThat(pose.isValid()).isTrue()
        val snap = BodyParameters(enabled = true, slim = 0.8f, waist = 0.5f)
        snap.clampInPlace()
        val mesh = BodyWarpField.buildMesh(pose, snap, FloatArray(0))
        assertThat(mesh.all { it.isFinite() }).isTrue()
        assertThat(BodyWarpField.maxAbs(mesh)).isAtMost(0.12f)
        val identity = BodyWarpField.buildMesh(pose, BodyParameters(), FloatArray(0))
        assertThat(BodyWarpField.maxAbs(identity)).isEqualTo(0f)
        val nanPose = PoseBody(1, FloatArray(PoseIndex.COUNT * 2) { Float.NaN })
        assertThat(nanPose.isValid()).isFalse()
        val still = BodyWarpField.buildMesh(nanPose, snap, FloatArray(0))
        assertThat(still.all { it.isFinite() }).isTrue()
        val body = BodyEngine(BodyRig())
        body.apply { enabled = true; slim = 4f; arms = -2f }
        assertThat(body.snapshot().slim).isEqualTo(1f)
        assertThat(body.snapshot().arms).isEqualTo(0f)
        body.reset()
        assertThat(body.snapshot().isIdentity()).isTrue()
    }
}

class LightingEngineTest {
    @Test
    fun lightingClampsAndShadersExist() {
        val e = LightingEngine(LightingRig())
        e.apply {
            enabled = true
            intensity = 3f
            warmth = 9f
            mode = LightingMode.Warm
        }
        val s = e.snapshot()
        assertThat(s.intensity).isEqualTo(1f)
        assertThat(s.warmth).isEqualTo(1f)
        e.reset()
        assertThat(e.snapshot().isIdentity()).isTrue()
        assertThat(SceneShaders.lightingOk()).isTrue()
        assertThat(SceneShaders.backgroundOk()).isTrue()
        assertThat(SceneShaders.hairOk()).isTrue()
        LightingParameters().also {
            it.intensity = Float.NaN
            it.clampInPlace()
            assertThat(it.intensity.isFinite()).isTrue()
        }
    }
}

class SceneSwitchTest {
    @Test
    fun switchingFeaturesDoesNotRequireDuplicatePipeline() {
        val bg = BackgroundEngine(BackgroundRig())
        val hair = HairEngine(HairRig())
        bg.set("bg.env.cyc_white", 0.8f)
        hair.apply { enabled = true; color = HairColorId.Blue; intensity = 0.4f }
        bg.set("bg.gradient.sunset", 0.5f)
        assertThat(bg.snapshot().id).isEqualTo("bg.gradient.sunset")
        assertThat(hair.snapshot().color).isEqualTo(HairColorId.Blue)
        bg.reset()
        assertThat(hair.snapshot().enabled).isTrue()
    }
}
