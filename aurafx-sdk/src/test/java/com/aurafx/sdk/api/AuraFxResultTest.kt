package com.aurafx.sdk.api

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuraFxResultTest {
    @Test
    fun okAndErrHelpers() {
        val ok = AuraFxResult.Ok(3)
        val err = AuraFxResult.Err(AuraFxError.PermissionDenied())
        assertThat(ok.isOk).isTrue()
        assertThat(err.isErr).isTrue()
        assertThat(ok.getOrNull()).isEqualTo(3)
        assertThat(err.errorOrNull()).isInstanceOf(AuraFxError.PermissionDenied::class.java)
    }
}
