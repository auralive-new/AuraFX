package com.aurafx.sdk.reference

import com.aurafx.sdk.api.BodyParameters
import com.aurafx.sdk.api.EyeshadowStyle
import com.aurafx.sdk.api.LightingMode
import com.aurafx.sdk.api.label
import com.aurafx.sdk.ar.AREffectCatalog
import com.aurafx.sdk.filter.FilterCatalog
import com.aurafx.sdk.scene.BackgroundCatalog
import com.aurafx.sdk.scene.BodyCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InventoryCompletionTest {
    @Test
    fun uniqueIdsAndNoPlaceholders() {
        assertThat(ReferenceFeatureSpec.duplicateIds()).isEmpty()
        assertThat(FilterCatalog.validate()).isEmpty()
        assertThat(AREffectCatalog.validate()).isEmpty()
        assertThat(BackgroundCatalog.validate()).isEmpty()
        for (fx in AREffectCatalog.effects) {
            assertThat(fx.productionRendered).isTrue()
            assertThat(fx.assets.all { it.procedural }).isTrue()
            assertThat(fx.displayName.isNotBlank()).isTrue()
        }
        for (bg in BackgroundCatalog.items) {
            assertThat(bg.displayName.isNotBlank()).isTrue()
            assertThat(bg.imageAsset).isNull()
        }
        for (f in FilterCatalog.filters) {
            assertThat(f.grade.isIdentity()).isFalse()
        }
    }

    @Test
    fun bodyHipsAndWaistRegistered() {
        val names = BodyCatalog.items.map { it.displayName }
        assertThat(names).containsAtLeastElementsIn(ReferenceFeatureSpec.requiredBodyNames)
        val p = BodyParameters(enabled = true, hips = 0.4f, waist = 0.3f)
        p.clampInPlace()
        assertThat(p.hips).isWithin(1e-4f).of(0.4f)
        assertThat(p.waist).isWithin(1e-4f).of(0.3f)
        p.reset()
        assertThat(p.hips).isEqualTo(0f)
        assertThat(p.isIdentity()).isTrue()
    }

    @Test
    fun beautyMakeupLightingRegistration() {
        assertThat(ReferenceFeatureSpec.requiredBeautyKeys).hasSize(13)
        assertThat(ReferenceFeatureSpec.makeupEnumsPresent()).isTrue()
        assertThat(LipLookLabels()).containsAtLeastElementsIn(ReferenceFeatureSpec.requiredLipLookNames)
        assertThat(EyeshadowStyle.entries.map { ReferenceFeatureSpec.eyeshadowLabel(it) })
            .containsExactlyElementsIn(ReferenceFeatureSpec.requiredEyeshadowNames)
        assertThat(ReferenceFeatureSpec.requiredMakeupPresets).hasSize(3)
        val lighting = LightingMode.entries.map { it.label() }
        assertThat(lighting).containsAtLeastElementsIn(ReferenceFeatureSpec.requiredLightingNames)
        val makeupCount =
            ReferenceFeatureSpec.requiredMakeupPresets.size +
                ReferenceFeatureSpec.requiredLipLookNames.size +
                ReferenceFeatureSpec.requiredBlushNames.size +
                ReferenceFeatureSpec.requiredBrowNames.size +
                ReferenceFeatureSpec.requiredEyeshadowNames.size +
                ReferenceFeatureSpec.requiredEyelinerCoreNames.size +
                ReferenceFeatureSpec.requiredLashNames.size +
                ReferenceFeatureSpec.requiredLensNames.size
        assertThat(makeupCount).isEqualTo(44)
    }

    @Test
    fun everyRequiredMaskIsRegisteredAndReachable() {
        assertThat(ReferenceFeatureSpec.requiredMaskNames).hasSize(61)
        val names = AREffectCatalog.effects.map { it.displayName }
        val missing = ReferenceFeatureSpec.requiredMaskNames.filter { it !in names }
        assertThat(missing).isEmpty()
        assertThat(AREffectCatalog.effects.size).isAtLeast(61)
        val ids = AREffectCatalog.effects.map { it.id }
        assertThat(ids.toSet()).hasSize(ids.size)
        for (name in ReferenceFeatureSpec.requiredMaskNames) {
            val fx = AREffectCatalog.effects.first { it.displayName == name }
            assertThat(AREffectCatalog.require(fx.id)).isNotNull()
            assertThat(fx.productionRendered).isTrue()
        }
    }

    @Test
    fun everyRequiredBackgroundIsRegistered() {
        assertThat(ReferenceFeatureSpec.requiredBackgroundNames).hasSize(36)
        val names = BackgroundCatalog.items.map { it.displayName }
        val missing = ReferenceFeatureSpec.requiredBackgroundNames.filter { it !in names }
        assertThat(missing).isEmpty()
        assertThat(BackgroundCatalog.items.size).isAtLeast(36)
        assertThat(FilterCatalog.filters).hasSize(51)
    }

    private fun LipLookLabels() =
        com.aurafx.sdk.api.LipLook.entries.map { ReferenceFeatureSpec.lipLookLabel(it) }
}
