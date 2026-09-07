package com.aurafx.sdk.reference

import com.aurafx.sdk.api.BeautyParameters
import com.aurafx.sdk.api.BlushStyle
import com.aurafx.sdk.api.BrowStyle
import com.aurafx.sdk.api.EyelinerStyle
import com.aurafx.sdk.api.FilterCategory
import com.aurafx.sdk.api.LashStyle
import com.aurafx.sdk.api.LensStyle
import com.aurafx.sdk.api.LipLook
import com.aurafx.sdk.api.MakeupParameters
import com.aurafx.sdk.api.MakeupPreset
import com.aurafx.sdk.api.trayLabel
import com.aurafx.sdk.ar.AREffectCatalog
import com.aurafx.sdk.filter.FilterCatalog
import com.aurafx.sdk.filter.FilterShaders
import com.aurafx.sdk.scene.BackgroundCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReferenceFeatureSpecTest {
    @Test
    fun uniqueFeatureIdsAcrossCatalogs() {
        assertThat(ReferenceFeatureSpec.duplicateIds()).isEmpty()
        val filterIds = FilterCatalog.filters.map { it.id }
        val arIds = AREffectCatalog.effects.map { it.id }
        val bgIds = BackgroundCatalog.items.map { it.id }
        assertThat(filterIds.toSet()).hasSize(filterIds.size)
        assertThat(arIds.toSet()).hasSize(arIds.size)
        assertThat(bgIds.toSet()).hasSize(bgIds.size)
    }

    @Test
    fun requiredNamedFiltersExistWithGpuGrades() {
        assertThat(ReferenceFeatureSpec.missingRequiredFilters()).isEmpty()
        assertThat(ReferenceFeatureSpec.missingRequiredTrays()).isEmpty()
        assertThat(FilterCatalog.validate()).isEmpty()
        assertThat(FilterCatalog.filters.size).isEqualTo(51)
        for (name in ReferenceFeatureSpec.requiredFilterDisplayNames) {
            val def = FilterCatalog.filters.first { it.displayName == name }
            assertThat(def.grade.isIdentity()).isFalse()
            assertThat(def.grade.allFinite()).isTrue()
            assertThat(def.capabilities()).contains("parametric")
            assertThat(def.capabilities()).contains("intensity")
        }
        for (cat in FilterCategory.entries) {
            assertThat(FilterCatalog.inCategory(cat)).isNotEmpty()
        }
        assertThat(FilterCategory.entries.map { it.trayLabel() })
            .containsAtLeastElementsIn(ReferenceFeatureSpec.requiredFilterTrayLabels)
        assertThat(FilterShaders.FRAG_GRADE).contains("uSceneMode")
        for (mode in 1..8) {
            assertThat(FilterShaders.FRAG_GRADE).contains("uSceneMode == $mode")
        }
        assertThat(FilterCatalog.require("scenery.rainy_street")!!.sceneMode).isEqualTo(1)
        assertThat(FilterCatalog.require("live.neon_clouds")!!.sceneMode).isEqualTo(2)
        assertThat(FilterCatalog.require("patterns.neon_pattern")!!.sceneMode).isEqualTo(3)
        assertThat(FilterCatalog.require("nature.desert")!!.sceneMode).isEqualTo(4)
        assertThat(FilterCatalog.require("rooms.canopy_bed")!!.sceneMode).isEqualTo(5)
    }

    @Test
    fun noPlaceholderImplementations() {
        for (f in FilterCatalog.filters) {
            assertThat(f.grade.isIdentity()).isFalse()
            assertThat(f.displayName.isNotBlank()).isTrue()
            assertThat(f.id.startsWith("placeholder")).isFalse()
        }
        for (fx in AREffectCatalog.effects) {
            assertThat(fx.productionRendered).isTrue()
            assertThat(fx.assets.all { it.procedural }).isTrue()
            assertThat(fx.anchors).isNotEmpty()
        }
        assertThat(BackgroundCatalog.items.size).isAtLeast(36)
        assertThat(BackgroundCatalog.validate()).isEmpty()
    }

    @Test
    fun filterArBackgroundRegistration() {
        assertThat(FilterCatalog.validate()).isEmpty()
        assertThat(AREffectCatalog.validate()).isEmpty()
        assertThat(BackgroundCatalog.validate()).isEmpty()
        val arNames = AREffectCatalog.effects.map { it.displayName }
        assertThat(arNames).containsAtLeastElementsIn(ReferenceFeatureSpec.requiredArDisplayNames)
        assertThat(ReferenceFeatureSpec.filterCount()).isEqualTo(51)
        assertThat(ReferenceFeatureSpec.arCount()).isAtLeast(61)
        assertThat(ReferenceFeatureSpec.backgroundCount()).isAtLeast(36)
    }

    @Test
    fun beautyRegistrationAndReset() {
        val beauty = BeautyParameters()
        beauty.fineSmooth = 0.2f
        beauty.toothWhiten = 0.2f
        beauty.whiten = 0.2f
        beauty.ruddy = 0.2f
        beauty.vFace = 0.2f
        beauty.cheekThin = 0.2f
        beauty.cheekSmall = 0.2f
        beauty.cheekNarrow = 0.2f
        beauty.nose = 0.2f
        beauty.eyeEnlarge = 0.2f
        beauty.eyeDistance = 0.2f
        beauty.mouth = 0.2f
        beauty.circles = 0.2f
        beauty.clampInPlace()
        assertThat(beauty.fineSmooth).isWithin(1e-4f).of(0.2f)
        assertThat(beauty.eyeDistance).isWithin(1e-4f).of(0.2f)
        beauty.reset()
        assertThat(beauty.fineSmooth).isEqualTo(0f)
        assertThat(beauty.eyeDistance).isEqualTo(0f)
        assertThat(beauty.circles).isEqualTo(0f)
        assertThat(ReferenceFeatureSpec.requiredBeautyKeys).containsExactly(
            "fineSmooth", "toothWhiten", "whiten", "ruddy", "vFace", "cheekThin",
            "cheekSmall", "cheekNarrow", "nose", "eyeEnlarge", "eyeDistance", "mouth", "circles",
        )
    }

    @Test
    fun makeupRegistrationMatchesRequiredNames() {
        assertThat(ReferenceFeatureSpec.makeupEnumsPresent()).isTrue()
        assertThat(MakeupPreset.entries.map { it.name })
            .containsExactlyElementsIn(ReferenceFeatureSpec.requiredMakeupPresets)
        assertThat(BlushStyle.entries.map { ReferenceFeatureSpec.blushLabel(it) })
            .containsExactlyElementsIn(ReferenceFeatureSpec.requiredBlushNames)
        assertThat(LipLook.entries.map { ReferenceFeatureSpec.lipLookLabel(it) })
            .containsAtLeastElementsIn(ReferenceFeatureSpec.requiredLipLookNames)
        assertThat(BrowStyle.entries.map { ReferenceFeatureSpec.browLabel(it) })
            .containsExactlyElementsIn(ReferenceFeatureSpec.requiredBrowNames)
        assertThat(EyelinerStyle.entries.map { ReferenceFeatureSpec.eyelinerLabel(it) })
            .containsAtLeastElementsIn(ReferenceFeatureSpec.requiredEyelinerCoreNames)
        assertThat(LashStyle.entries.map { ReferenceFeatureSpec.lashLabel(it) })
            .containsExactlyElementsIn(ReferenceFeatureSpec.requiredLashNames)
        assertThat(LensStyle.entries.map { ReferenceFeatureSpec.lensLabel(it) })
            .containsAtLeastElementsIn(ReferenceFeatureSpec.requiredLensNames)
        val p = MakeupParameters()
        p.applyPreset(MakeupPreset.Classic)
        assertThat(p.lipstick.look).isEqualTo(LipLook.GlossyPop)
        p.reset()
        assertThat(p.isIdentity()).isTrue()
    }
}
