package com.aurafx.sdk.reference

import com.aurafx.sdk.ar.AREffectCatalog
import com.aurafx.sdk.filter.FilterCatalog
import com.aurafx.sdk.scene.BackgroundCatalog
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class ReferenceVideoAuditTest {
    @Test
    fun doesNotInventReferenceTotalsWhenVideosAreMissing() {
        assertThat(ReferenceVideoAudit.videoCount()).isEqualTo(0)
        assertThat(ReferenceVideoAudit.sourceStatus())
            .isEqualTo(ReferenceVideoAudit.STATUS_SOURCE_MISSING)
        assertThat(ReferenceVideoAudit.referenceFilterTotal()).isNull()
        assertThat(ReferenceVideoAudit.referenceArTotal()).isNull()
        assertThat(ReferenceVideoAudit.referenceMaskTotal()).isNull()
        assertThat(ReferenceVideoAudit.referenceBackgroundTotal()).isNull()
        assertThat(ReferenceVideoAudit.parityStatus())
            .isEqualTo(ReferenceVideoAudit.PARITY_INCOMPLETE)
    }

    @Test
    fun attachedStillHasNoCarouselTilesToCount() {
        val still = ReferenceVideoAudit.attachedStillEvidence()
        assertThat(still.carouselItemsVisible).isEqualTo(0)
        assertThat(still.readableLabels).contains("Go Live")
        assertThat(still.notes).contains("out of scope")
    }

    @Test
    fun baselineCatalogsUnchangedAndUnique() {
        assertThat(FilterCatalog.validate()).isEmpty()
        assertThat(FilterCatalog.filters).hasSize(51)
        assertThat(AREffectCatalog.validate()).isEmpty()
        assertThat(AREffectCatalog.effects.size).isAtLeast(61)
        assertThat(BackgroundCatalog.validate()).isEmpty()
        assertThat(BackgroundCatalog.items.size).isAtLeast(36)
        val ids = FilterCatalog.filters.map { it.id } +
            AREffectCatalog.effects.map { it.id } +
            BackgroundCatalog.items.map { it.id }
        assertThat(ids.toSet()).hasSize(ids.size)
        assertThat(ReferenceFeatureSpec.duplicateIds()).isEmpty()
        assertThat(ReferenceFeatureSpec.missingRequiredFilters()).isEmpty()
    }

    @Test
    fun productionPackagesDoNotUseForbiddenReferenceBrand() {
        val roots = listOf(
            File("src/main/java"),
            File("../aurafx-sdk/src/main/java"),
        ).filter { it.exists() }
        val hits = ArrayList<String>()
        for (root in roots) {
            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val text = file.readText()
                if (text.contains("Tango", ignoreCase = false) ||
                    Regex("""(?i)(?<![A-Za-z])tango(?![A-Za-z])""").containsMatchIn(text)
                ) {
                    hits += file.path
                }
            }
        }
        assertThat(hits).isEmpty()
    }
}
