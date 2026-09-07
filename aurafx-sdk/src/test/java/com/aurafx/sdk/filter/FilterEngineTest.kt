package com.aurafx.sdk.filter

import com.aurafx.sdk.api.FilterCategory
import com.aurafx.sdk.api.FilterFinish
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class FilterCatalogTest {
    @Test
    fun catalogIsValidWithUniqueIdsAndEveryCategory() {
        val errors = FilterCatalog.validate()
        assertThat(errors).isEmpty()
        val ids = FilterCatalog.filters.map { it.id }
        assertThat(ids.toSet()).hasSize(ids.size)
        for (cat in FilterCategory.entries) {
            assertThat(FilterCatalog.inCategory(cat)).isNotEmpty()
        }
        assertThat(FilterCatalog.filters).hasSize(39)
    }

    @Test
    fun parameterRangesAreFiniteAndClamped() {
        val wild = ColorGrade(
            exposure = Float.NaN,
            contrast = 99f,
            saturation = -4f,
            gamma = 0f,
            bloom = Float.POSITIVE_INFINITY,
        ).clamp()
        assertThat(wild.allFinite()).isTrue()
        assertThat(wild.exposure).isEqualTo(0f)
        assertThat(wild.contrast).isEqualTo(1f)
        assertThat(wild.saturation).isEqualTo(0f)
        assertThat(wild.gamma).isAtLeast(0.4f)
        assertThat(wild.bloom).isEqualTo(0f)
        for (f in FilterCatalog.filters) {
            assertThat(f.intensityMin).isEqualTo(0f)
            assertThat(f.intensityMax).isEqualTo(1f)
            assertThat(f.grade.allFinite()).isTrue()
            assertThat(f.capabilities()).contains("intensity")
        }
    }

    @Test
    fun shadersAndResourcesArePresent() {
        assertThat(FilterShaders.sourcesAvailable()).isTrue()
        assertThat(FilterPipelineEffect.ID).isEqualTo("aurafx.filter.engine")
    }
}

class LutPipelineTest {
    @Test
    fun identityLutSamplesInBounds() {
        val lut = Lut3d.identity(8)
        when (val v = lut.validate()) {
            is LutValidation.Invalid -> throw AssertionError(v.reason)
            is LutValidation.Ok -> {
                assertThat(v.min).isAtLeast(0f)
                assertThat(v.max).isAtMost(1f)
            }
        }
        val c = lut.sample(0.25f, 0.5f, 0.75f)
        assertThat(c[0]).isWithin(0.02f).of(0.25f)
        assertThat(c[1]).isWithin(0.02f).of(0.5f)
        assertThat(c[2]).isWithin(0.02f).of(0.75f)
        val clamped = lut.sample(-2f, 4f, Float.NaN)
        assertThat(clamped.all { it.isFinite() }).isTrue()
        assertThat(clamped[0]).isWithin(1e-4f).of(0f)
        assertThat(clamped[1]).isWithin(1e-4f).of(1f)
    }

    @Test
    fun bakedLutsAreRealTransforms() {
        val warm = FilterCatalog.require("warm.golden")!!.bakedLut()
        val cool = FilterCatalog.require("cool.arctic")!!.bakedLut()
        assertThat(warm.isIdentity(0.02f)).isFalse()
        assertThat(cool.isIdentity(0.02f)).isFalse()
        val skin = floatArrayOf(0.72f, 0.52f, 0.42f)
        val w = warm.sample(skin[0], skin[1], skin[2])
        val c = cool.sample(skin[0], skin[1], skin[2])
        assertThat(w[0] - w[2]).isGreaterThan(c[0] - c[2])
        for (f in FilterCatalog.filters.filter { it.usesLut }) {
            val v = f.bakedLut().validate()
            assertThat(v).isInstanceOf(LutValidation.Ok::class.java)
            assertThat(f.bakedLut().isIdentity(0.015f)).isFalse()
        }
    }

    @Test
    fun cubeLoaderValidatesAndRejectsJunk() {
        val cube = buildString {
            appendLine("TITLE \"grade\"")
            appendLine("LUT_3D_SIZE 2")
            appendLine("0 0 0")
            appendLine("1 0 0")
            appendLine("0 1 0")
            appendLine("1 1 0")
            appendLine("0 0 1")
            appendLine("1 0 1")
            appendLine("0 1 1")
            appendLine("1 1 1")
        }
        val lut = LutAssetLoader.parseCube(cube)
        assertThat(lut.size).isEqualTo(2)
        val bad = "LUT_3D_SIZE 2\n0 0 0\n"
        try {
            LutAssetLoader.parseCube(bad)
            throw AssertionError("expected failure")
        } catch (_: IllegalArgumentException) {
        }
    }
}

class FilterEngineBehaviorTest {
    @Test
    fun intensityZeroIsOriginalPath() {
        val src = floatArrayOf(0.4f, 0.5f, 0.6f)
        val grade = FilterCatalog.require("vibe.punch")!!.grade
        val dst = ColorProcessor.applyGrade(src[0], src[1], src[2], grade)
        val mixed = ColorProcessor.mix(src, dst, 0f)
        assertThat(mixed[0]).isWithin(1e-5f).of(src[0])
        assertThat(mixed[1]).isWithin(1e-5f).of(src[1])
        assertThat(mixed[2]).isWithin(1e-5f).of(src[2])
        assertThat(ColorProcessor.mix(src, dst, 0.5f)[0]).isNotEqualTo(src[0])
        val rig = FilterRig()
        rig.setFilter("vibe.punch", 0f)
        assertThat(rig.snapshot().isIdentity()).isTrue()
    }

    @Test
    fun nonZeroIntensityChangesParameters() {
        val src = floatArrayOf(0.55f, 0.48f, 0.40f)
        val a = ColorProcessor.applyGrade(src[0], src[1], src[2], FilterCatalog.require("warm.sunset")!!.grade)
        val b = ColorProcessor.applyGrade(src[0], src[1], src[2], FilterCatalog.require("cool.moonlight")!!.grade)
        assertThat(abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])).isGreaterThan(0.05f)
        val mid = ColorProcessor.mix(src, a, 0.5f)
        val full = ColorProcessor.mix(src, a, 1f)
        assertThat(abs(mid[0] - src[0])).isLessThan(abs(full[0] - src[0]) + 1e-4f)
    }

    @Test
    fun resetAndSwitchPreserveCatalogAndRejectUnknown() {
        val engine = FilterEngine(FilterRig())
        assertThat(engine.setFilter("portrait.studio", 0.65f)).isTrue()
        assertThat(engine.snapshot().definition?.id).isEqualTo("portrait.studio")
        assertThat(engine.snapshot().intensity).isWithin(1e-4f).of(0.65f)
        assertThat(engine.setFilter("mood.noir", 0.4f)).isTrue()
        assertThat(engine.snapshot().definition?.id).isEqualTo("mood.noir")
        assertThat(engine.setFilter("does.not.exist", 1f)).isFalse()
        assertThat(engine.snapshot().definition?.id).isEqualTo("mood.noir")
        engine.reset()
        assertThat(engine.snapshot().isIdentity()).isTrue()
        engine.setFilter("classic.print", 1f)
        engine.clear()
        assertThat(engine.snapshot().isIdentity()).isTrue()
        engine.apply {
            id = "glow.pearl"
            intensity = 0.75f
            finish = FilterFinish.Grain
            finishIntensity = 0.5f
        }
        assertThat(engine.snapshot().grain()).isGreaterThan(0f)
        engine.apply { id = "bogus.filter" }
        assertThat(engine.snapshot().definition).isNull()
    }

    @Test
    fun thumbnailsMatchDefinitionAndDifferAcrossLooks() {
        val warm = FilterCatalog.require("warm.golden")!!.thumbnailArgb(16, 16)
        val cool = FilterCatalog.require("cool.arctic")!!.thumbnailArgb(16, 16)
        assertThat(warm.size).isEqualTo(256)
        assertThat(cool.size).isEqualTo(256)
        assertThat(warm.contentEquals(cool)).isFalse()
        val identityProbe = FilterThumbnail.render(
            FilterDefinition(
                id = "test.identity",
                category = FilterCategory.Natural,
                displayName = "x",
                grade = ColorGrade(),
                usesLut = false,
                defaultIntensity = 0f,
            ),
            8,
            8,
        )
        val graded = FilterCatalog.require("natural.true")!!.thumbnailArgb(8, 8)
        assertThat(identityProbe.contentEquals(graded)).isFalse()
    }

    @Test
    fun noNanFromProcessor() {
        val out = ColorProcessor.applyGrade(Float.NaN, 2f, -1f, ColorGrade(exposure = 1.2f))
        assertThat(out.all { it.isFinite() }).isTrue()
    }
}
