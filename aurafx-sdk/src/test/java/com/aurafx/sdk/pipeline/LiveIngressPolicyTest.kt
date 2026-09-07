package com.aurafx.sdk.pipeline

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LiveIngressPolicyTest {
    @Test
    fun processFrameDeniedWhileCameraWantedOrBound() {
        assertThat(LiveIngressPolicy.denyProcessFrameIfCameraActive(cameraWanted = true, cameraBound = false))
            .isNotNull()
        assertThat(LiveIngressPolicy.denyProcessFrameIfCameraActive(cameraWanted = false, cameraBound = true))
            .isNotNull()
        assertThat(LiveIngressPolicy.denyProcessFrameIfCameraActive(cameraWanted = false, cameraBound = false))
            .isNull()
    }
}
