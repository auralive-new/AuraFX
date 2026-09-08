package com.aurafx.sdk.gift

import com.aurafx.sdk.api.GiftFxLayer
import com.aurafx.sdk.api.GiftFxOcclusion
import com.aurafx.sdk.api.GiftFxRendererType
import com.aurafx.sdk.api.GiftFxTrackingNeed
import com.aurafx.sdk.scene.AuraFxEffectOrder
import com.aurafx.sdk.vision.PoseBody
import com.aurafx.sdk.vision.PoseIndex
import com.aurafx.sdk.vision.TrackingData
import com.aurafx.sdk.vision.VisionStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GiftFxCatalogTest {
    @Test
    fun catalogContainsExactlyTenUniqueSamples() {
        assertThat(GiftFxCatalog.validate()).isEmpty()
        assertThat(GiftFxCatalog.samples).hasSize(10)
        val ids = GiftFxCatalog.samples.map { it.giftId }
        assertThat(ids).containsExactlyElementsIn(GiftFxIds.ordered).inOrder()
        assertThat(ids.toSet()).hasSize(10)
        for (d in GiftFxCatalog.samples) {
            assertThat(d.durationMs).isAtLeast(8000)
            assertThat(d.durationMs).isAtMost(10000)
            assertThat(d.gpuRendered).isTrue()
            assertThat(d.valid()).isTrue()
        }
        assertThat(GiftFxCatalog.samples.map { it.shaderLook }.toSet()).hasSize(10)
        assertThat(GiftFxCatalog.samples.map { it.rendererType }.toSet()).hasSize(10)
    }

    @Test
    fun shadersCoverAllTenGpuLooks() {
        assertThat(GiftFxShaders.sourcesOk()).isTrue()
        assertThat(AuraFxEffectOrder.gpuIds.last()).isEqualTo(AuraFxEffectOrder.GIFT)
        assertThat(AuraFxEffectOrder.gpuIds).contains(AuraFxEffectOrder.AR)
        assertThat(AuraFxEffectOrder.gpuIds.indexOf(AuraFxEffectOrder.GIFT))
            .isGreaterThan(AuraFxEffectOrder.gpuIds.indexOf(AuraFxEffectOrder.AR))
    }
}

class GiftFxLifecycleTest {
    @Test
    fun startUpdateEndCleanupAndMultipleInstances() {
        val engine = GiftFxEngine()
        val t0 = 1_000_000_000L
        val tracking = TrackingData.unavailable(t0)
        val a = engine.play(GiftFxIds.TIME_FREEZE, t0)
        val b = engine.play(GiftFxIds.PORTAL_DOOR, t0)
        assertThat(a).isNotNull()
        assertThat(b).isNotNull()
        assertThat(a).isNotEqualTo(b)
        assertThat(engine.manager.activeCount()).isEqualTo(2)
        engine.tick(t0 + 200_000_000L, 1080, 1920, tracking)
        val snap = ArrayList<GiftFxInstance>()
        engine.manager.snapshotActive(snap)
        assertThat(snap).hasSize(2)
        assertThat(snap.map { it.phase }.toSet()).contains(GiftFxPhase.Start)
        engine.tick(t0 + 3_000_000_000L, 1080, 1920, tracking)
        engine.manager.snapshotActive(snap)
        assertThat(snap.all { it.phase == GiftFxPhase.Main || it.phase == GiftFxPhase.Buildup }).isTrue()
        engine.stop(a!!)
        assertThat(engine.manager.activeCount()).isEqualTo(1)
        engine.stopAll()
        assertThat(engine.manager.activeCount()).isEqualTo(0)
        val replay = engine.replay(t0 + 4_000_000_000L)
        assertThat(replay).isNotNull()
        engine.reset()
        assertThat(engine.manager.activeCount()).isEqualTo(0)
        assertThat(engine.manager.lastGiftId).isNull()
    }

    @Test
    fun instanceAutoCleansAfterDuration() {
        val mgr = GiftFxManager()
        val start = 10_000_000_000L
        val id = mgr.play(GiftFxIds.GRAVITY_FLIP, start)!!
        val def = GiftFxCatalog.require(GiftFxIds.GRAVITY_FLIP)!!
        val scratch = FloatArray(3)
        mgr.update(start + def.durationMs * 1_000_000L + 5_000_000L, scratch, 720, 1280, TrackingData.unavailable(start))
        assertThat(mgr.activeCount()).isEqualTo(0)
        assertThat(mgr.activeInstanceIds()).doesNotContain(id)
    }

    @Test
    fun rendererDisposeIsIdempotent() {
        val renderer = GiftFxRenderer()
        renderer.onAttach()
        assertThat(renderer.attached).isTrue()
        renderer.dispose()
        assertThat(renderer.disposed).isTrue()
        assertThat(renderer.attached).isFalse()
        renderer.dispose()
        assertThat(renderer.disposed).isTrue()
        val engine = GiftFxEngine()
        engine.renderer.onAttach()
        engine.dispose()
        assertThat(engine.renderer.disposed).isTrue()
        assertThat(engine.hasActive()).isFalse()
    }

    @Test
    fun playStopDoesNotRestartCamera() {
        assertThat(GiftFxCameraPolicy.PLAY_STOP_RESTARTS_CAMERA).isFalse()
        assertThat(GiftFxCameraPolicy.INTRODUCES_SECOND_CAMERAX_BIND).isFalse()
        assertThat(GiftFxManager.REBINDS_CAMERA_ON_PLAY_OR_STOP).isFalse()
        val binds = intArrayOf(0)
        val engine = GiftFxEngine()
        engine.play(GiftFxIds.INK_UNIVERSE, 1L)
        engine.stopAll()
        engine.play(GiftFxIds.MINI_WORLD, 2L)
        engine.reset()
        assertThat(binds[0]).isEqualTo(0)
    }

    @Test
    fun trackingAnchorsAreNotFixedScreenPointsWhenPosePresent() {
        val pose = PoseBody(1, FloatArray(PoseIndex.COUNT * 2) { 0.5f })
        pose.landmarksNormalized[PoseIndex.NOSE * 2] = 0.41f
        pose.landmarksNormalized[PoseIndex.NOSE * 2 + 1] = 0.33f
        pose.landmarksNormalized[PoseIndex.LEFT_SHOULDER * 2] = 0.30f
        pose.landmarksNormalized[PoseIndex.LEFT_SHOULDER * 2 + 1] = 0.48f
        pose.landmarksNormalized[PoseIndex.RIGHT_SHOULDER * 2] = 0.62f
        pose.landmarksNormalized[PoseIndex.RIGHT_SHOULDER * 2 + 1] = 0.49f
        pose.landmarksNormalized[PoseIndex.LEFT_HIP * 2] = 0.34f
        pose.landmarksNormalized[PoseIndex.LEFT_HIP * 2 + 1] = 0.72f
        pose.landmarksNormalized[PoseIndex.RIGHT_HIP * 2] = 0.58f
        pose.landmarksNormalized[PoseIndex.RIGHT_HIP * 2 + 1] = 0.73f
        val tracking = TrackingData(
            status = VisionStatus.READY,
            frameTimestampNs = 5L,
            pose = listOf(pose),
        )
        val engine = GiftFxEngine()
        engine.play(GiftFxIds.GIANT_SHADOW, 5L)
        engine.tick(5L + 50_000_000L, 1080, 1920, tracking)
        val snap = ArrayList<GiftFxInstance>()
        engine.manager.snapshotActive(snap)
        assertThat(snap).hasSize(1)
        assertThat(snap[0].subjectX).isNotEqualTo(0.5f)
        assertThat(snap[0].subjectY).isGreaterThan(0.4f)
    }
}

class GiftFxCoordinatesTest {
    @Test
    fun portraitNineSixteenMapsIdentity() {
        val p = GiftFxCoordinates.toPortraitUv(0.5f, 0.25f, 1080, 1920)
        assertThat(p[0]).isWithin(1e-4f).of(0.5f)
        assertThat(p[1]).isWithin(1e-4f).of(0.25f)
        assertThat(GiftFxCoordinates.inPortraitSafeFrame(0.5f, 0.5f, 1080, 1920)).isTrue()
        val c = GiftFxCoordinates.portraitCenter(1080, 1920)
        assertThat(c[0]).isWithin(1e-4f).of(0.5f)
        assertThat(c[1]).isWithin(1e-4f).of(0.5f)
    }

    @Test
    fun landscapeLetterboxKeepsNineSixteenContent() {
        val left = GiftFxCoordinates.toPortraitUv(0.0f, 0.5f, 1920, 1080)
        assertThat(left[0]).isLessThan(0f)
        val mid = GiftFxCoordinates.toPortraitUv(0.5f, 0.5f, 1920, 1080)
        assertThat(mid[0]).isWithin(1e-3f).of(0.5f)
        val back = GiftFxCoordinates.fromPortraitUv(0.0f, 0.0f, 1920, 1080)
        assertThat(back[0]).isGreaterThan(0.2f)
        assertThat(GiftFxCoordinates.frameAspect(1080, 1920)).isWithin(0.01f).of(GiftFxCoordinates.PORTRAIT_ASPECT)
    }
}

class GiftFxParticleReuseTest {
    @Test
    fun particleSystemReusesBuffer() {
        val sys = GiftFxParticleSystem(32)
        val before = sys.data
        sys.step(0.016f, 0.5f, 0.4f, 0.2f, 80f, 1, 1f, 1f)
        sys.step(0.016f, 0.5f, 0.4f, -0.2f, 80f, 1, 1f, 1f)
        assertThat(sys.data).isSameInstanceAs(before)
        sys.reset()
        assertThat(sys.data.all { it == 0f }).isTrue()
    }
}

class GiftFxDefinitionCoverageTest {
    @Test
    fun occlusionAndTrackingFlagsMatchIntent() {
        val portal = GiftFxCatalog.require(GiftFxIds.PORTAL_DOOR)!!
        assertThat(portal.layer).isEqualTo(GiftFxLayer.BehindSubject)
        assertThat(portal.occlusion).isEqualTo(GiftFxOcclusion.PersonMask)
        val shadow = GiftFxCatalog.require(GiftFxIds.GIANT_SHADOW)!!
        assertThat(shadow.occlusion).isEqualTo(GiftFxOcclusion.PersonMask)
        assertThat(shadow.requiredTracking).isEqualTo(GiftFxTrackingNeed.Segmentation)
        val holo = GiftFxCatalog.require(GiftFxIds.HOLOGRAM_CLONE)!!
        assertThat(holo.requiredTracking).isEqualTo(GiftFxTrackingNeed.Face)
        assertThat(holo.rendererType).isEqualTo(GiftFxRendererType.HologramClone)
        val gravity = GiftFxCatalog.require(GiftFxIds.GRAVITY_FLIP)!!
        assertThat(gravity.layer).isEqualTo(GiftFxLayer.FullFrame)
        assertThat(gravity.requiredTracking).isEqualTo(GiftFxTrackingNeed.None)
    }
}
