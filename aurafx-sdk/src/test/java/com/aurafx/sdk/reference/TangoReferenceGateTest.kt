package com.aurafx.sdk.reference

import com.aurafx.sdk.api.BlushStyle
import com.aurafx.sdk.api.BrowStyle
import com.aurafx.sdk.api.EyelinerStyle
import com.aurafx.sdk.api.LashStyle
import com.aurafx.sdk.api.LensStyle
import com.aurafx.sdk.api.MakeupPreset
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class TangoReferenceGateTest {
    @Test
    fun sourceIsUnavailableAndCompleteFlagStaysFalse() {
        assertThat(TangoReferenceGate.SOURCE_AVAILABLE).isFalse()
        assertThat(TangoReferenceGate.TANGO_COMPLETE).isFalse()
        assertThat(TangoReferenceGate.STATUS).isEqualTo("NEEDS SOURCE MATERIAL")
        assertThat(TangoReferenceGate.tangoFilterTotal()).isNull()
        assertThat(TangoReferenceGate.tangoArTotal()).isNull()
        assertThat(TangoReferenceGate.tangoBackgroundTotal()).isNull()
        assertThat(TangoReferenceGate.missingFilters()).isNull()
        assertThat(TangoReferenceGate.missingAr()).isNull()
        assertThat(TangoReferenceGate.missingBackgrounds()).isNull()
        assertThat(TangoReferenceGate.citedFilterExamplesPresentInAuraFx()).isEmpty()
        assertThat(TangoReferenceGate.auraFxFilterTotal()).isEqualTo(39)
        assertThat(TangoReferenceGate.auraFxArTotal()).isEqualTo(10)
        assertThat(TangoReferenceGate.auraFxBackgroundTotal()).isEqualTo(20)
    }

    @Test
    fun workspaceHasNoTangoMediaToInventCountsFrom() {
        val root = File(".").canonicalFile.let { dir ->
            generateSequence(dir) { it.parentFile }.firstOrNull { File(it, "settings.gradle.kts").exists() } ?: dir
        }
        val media = root.walkTopDown()
            .filter { it.isFile }
            .filter { !it.path.contains("${File.separator}build${File.separator}") }
            .filter { !it.path.contains("${File.separator}.git${File.separator}") }
            .filter { !it.path.contains("${File.separator}.gradle${File.separator}") }
            .filter {
                val n = it.name.lowercase()
                n.endsWith(".mp4") || n.endsWith(".mov") || n.endsWith(".webm") || n.endsWith(".mkv") ||
                    n.endsWith(".m4v") || n.endsWith(".gif")
            }
            .toList()
        assertThat(media).isEmpty()
    }

    @Test
    fun citedMakeupStyleEnumsStillExistAsRealSdkTypes() {
        assertThat(MakeupPreset.entries.map { it.name }).containsExactly("Classic", "Bright", "Extravagant")
        assertThat(BlushStyle.entries).hasSize(4)
        assertThat(BrowStyle.entries).hasSize(7)
        assertThat(EyelinerStyle.entries).hasSize(11)
        assertThat(LashStyle.entries).hasSize(6)
        assertThat(LensStyle.entries).hasSize(6)
    }
}
