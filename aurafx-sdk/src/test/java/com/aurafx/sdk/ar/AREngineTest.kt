package com.aurafx.sdk.ar

import com.aurafx.sdk.api.ARCategory
import com.aurafx.sdk.api.AROcclusionMode
import com.aurafx.sdk.beauty.FaceLandmarks
import com.aurafx.sdk.beauty.FaceTopology
import com.aurafx.sdk.scene.AuraFxEffectOrder
import com.aurafx.sdk.scene.Step5Outstanding
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class ARCatalogTest {
    @Test
    fun catalogIntegrityAndRenderedSet() {
        assertThat(AREffectCatalog.validate()).isEmpty()
        assertThat(AREffectCatalog.effects.size).isAtLeast(61)
        assertThat(AREffectCatalog.effects.map { it.id }.toSet()).hasSize(AREffectCatalog.effects.size)
        assertThat(AREffectCatalog.effects.all { it.productionRendered }).isTrue()
        val assets = ARAssetManager()
        for (fx in AREffectCatalog.effects) {
            assertThat(assets.requiredAssetsPresent(fx)).isTrue()
            assertThat(fx.anchors.all { it.valid() }).isTrue()
            assertThat(fx.animation.valid()).isTrue()
            assertThat(fx.occlusion).isNotEqualTo(null)
            val thumb = ARThumbnail.argb(fx, 8, 8)
            assertThat(thumb.size).isEqualTo(64)
        }
        val a = ARThumbnail.argb(AREffectCatalog.require("ar.desert_sun")!!, 8, 8)
        val b = ARThumbnail.argb(AREffectCatalog.require("ar.moonlit_glow")!!, 8, 8)
        assertThat(a.contentEquals(b)).isFalse()
        assertThat(ARShaders.sourcesOk()).isTrue()
        assertThat(AuraFxEffectOrder.gpuIds.last()).isEqualTo(AuraFxEffectOrder.AR)
        assertThat(Step5Outstanding.REPLACEMENT_HAIRSTYLES_REQUIRING_GROOM).isEqualTo(0)
    }

    @Test
    fun categoriesCoverRequiredTypes() {
        val cats = AREffectCatalog.effects.flatMap { it.categories }.toSet()
        assertThat(cats).containsAtLeast(
            ARCategory.FaceAccessory,
            ARCategory.FaceMask,
            ARCategory.EyeEffect,
            ARCategory.Particle,
            ARCategory.FacePaint,
            ARCategory.Environment,
            ARCategory.Animated,
        )
        assertThat(AREffectCatalog.effects.count { ARCategory.FaceMask in it.categories }).isAtLeast(2)
        assertThat(AREffectCatalog.effects.count { ARCategory.EyeEffect in it.categories }).isAtLeast(3)
        assertThat(AREffectCatalog.effects.count { ARCategory.Particle in it.categories }).isAtLeast(8)
        assertThat(AREffectCatalog.effects.count { ARCategory.Animated in it.categories }).isAtLeast(8)
        assertThat(AREffectCatalog.effects.count { ARCategory.Environment in it.categories }).isAtLeast(3)
        assertThat(AREffectCatalog.effects.count { ARCategory.FaceAccessory in it.categories }).isAtLeast(4)
    }
}

class ARTrackingAndAnimTest {
    private fun face(openEyes: Boolean = true, smile: Boolean = false, mouth: Boolean = false, brow: Boolean = false): FaceLandmarks {
        val xy = FloatArray(478 * 2) { 0.5f }
        fun set(i: Int, x: Float, y: Float) {
            xy[i * 2] = x
            xy[i * 2 + 1] = y
        }
        set(FaceTopology.LEFT_EYE_OUTER, 0.36f, 0.40f)
        set(FaceTopology.LEFT_EYE_INNER, 0.44f, 0.40f)
        set(FaceTopology.RIGHT_EYE_INNER, 0.56f, 0.40f)
        set(FaceTopology.RIGHT_EYE_OUTER, 0.64f, 0.40f)
        val eyeH = if (openEyes) 0.06f else 0.008f
        set(159, 0.40f, 0.40f - eyeH)
        set(145, 0.40f, 0.40f + eyeH * 0.4f)
        set(386, 0.60f, 0.40f - eyeH)
        set(374, 0.60f, 0.40f + eyeH * 0.4f)
        set(13, 0.50f, if (mouth) 0.62f else 0.64f)
        set(14, 0.50f, if (mouth) 0.72f else 0.655f)
        set(61, if (smile) 0.40f else 0.46f, 0.66f)
        set(291, if (smile) 0.60f else 0.54f, 0.66f)
        set(105, 0.40f, if (brow) 0.28f else 0.34f)
        set(334, 0.60f, if (brow) 0.28f else 0.34f)
        set(1, 0.50f, 0.48f)
        set(10, 0.50f, 0.28f)
        set(152, 0.50f, 0.82f)
        set(50, 0.38f, 0.55f)
        set(280, 0.62f, 0.55f)
        set(468, 0.40f, 0.40f)
        set(473, 0.60f, 0.40f)
        return FaceLandmarks(1, xy, 478, 1_000_000L, true)
    }

    @Test
    fun poseAndExpressionAreFiniteAndReactive() {
        val open = ARTrackingContext.from(face(openEyes = true))!!
        val closed = ARTrackingContext.from(face(openEyes = false))!!
        assertThat(open.allFinite()).isTrue()
        assertThat(open.pose.iod).isGreaterThan(0.05f)
        assertThat(closed.expression.blinkLeft).isGreaterThan(open.expression.blinkLeft)
        val sm = ARTrackingContext.from(face(smile = true))!!
        val ns = ARTrackingContext.from(face(smile = false))!!
        assertThat(sm.expression.smile).isGreaterThan(ns.expression.smile)
        val mo = ARTrackingContext.from(face(mouth = true))!!
        assertThat(mo.expression.mouthOpen).isGreaterThan(0.05f)
        val br = ARTrackingContext.from(face(brow = true))!!
        assertThat(br.expression.browRaise).isGreaterThan(ARTrackingContext.from(face(brow = false))!!.expression.browRaise)
        val turned = face()
        turned.xy[1 * 2] = 0.58f
        val yawed = ARTrackingContext.from(turned)!!
        assertThat(abs(yawed.pose.yaw)).isGreaterThan(0.05f)
    }

    @Test
    fun animationPoolHasNoNaNAndResets() {
        val anim = ARAnimationEngine()
        anim.step(2_000_000L, 40f, 0.5f, 0.4f, 0.2f, 0.8f, 5)
        anim.step(18_000_000L, 40f, 0.5f, 0.4f, 0.2f, 0.8f, 5)
        assertThat(anim.particles.all { it.isFinite() }).isTrue()
        assertThat(anim.timeSec).isGreaterThan(0f)
        val bounce = anim.bounce(1f)
        assertThat(bounce.isFinite()).isTrue()
        anim.reset()
        assertThat(anim.particles.sum()).isEqualTo(0f)
    }

    @Test
    fun spritesFollowAnchorsAndStayBounded() {
        val tr = ARTrackingContext.from(face())!!
        val anim = ARAnimationEngine()
        anim.step(5_000_000L, 20f, tr.mouth[0], tr.mouth[1], 0f, 1f, 5)
        val assets = ARAssetManager()
        val def = AREffectCatalog.require("ar.purrfect_match")!!
        val sprites = assets.spritesFor(def, tr, anim, 1f)
        assertThat(sprites).isNotEmpty()
        sprites.forEach {
            assertThat(it.uvx.isFinite()).isTrue()
            assertThat(it.uvy.isFinite()).isTrue()
            assertThat(it.rx).isGreaterThan(0f)
        }
    }
}

class AREngineSwitchTest {
    @Test
    fun setClearSwitchIntensity() {
        val engine = AREngine(ARRig())
        assertThat(engine.setAREffect("missing")).isFalse()
        assertThat(engine.snapshot().isIdentity()).isTrue()
        assertThat(engine.setAREffect("ar.cupid", 0.7f)).isTrue()
        assertThat(engine.snapshot().effectId).isEqualTo("ar.cupid")
        assertThat(engine.snapshot().intensity).isWithin(1e-4f).of(0.7f)
        engine.setAREffect("ar.party_hop", 0.4f)
        assertThat(engine.snapshot().effectId).isEqualTo("ar.party_hop")
        engine.setAREffectIntensity(3f)
        assertThat(engine.snapshot().intensity).isEqualTo(1f)
        engine.clear()
        assertThat(engine.snapshot().isIdentity()).isTrue()
        engine.apply { effectId = "ar.red_hero"; intensity = 0.5f }
        engine.reset()
        assertThat(engine.snapshot().isIdentity()).isTrue()
        engine.apply { intensity = Float.NaN; effectId = "ar.desert_sun" }
        assertThat(engine.snapshot().intensity.isFinite()).isTrue()
        val mgr = AREffectManager(engine)
        mgr.switchTo("ar.blush_pop", 0.9f)
        assertThat(mgr.active()?.id).isEqualTo("ar.blush_pop")
        assertThat(mgr.active()?.occlusion).isEqualTo(AROcclusionMode.PersonOnly)
    }
}
