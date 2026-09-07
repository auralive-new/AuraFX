package com.aurafx.sdk.pipeline

import com.aurafx.sdk.api.AuraFxError

/**
 * Live camera and [com.aurafx.sdk.AuraFxSession.processFrame] share one GL output.
 * They must never run as two concurrent producers.
 */
object LiveIngressPolicy {
    fun denyProcessFrameIfCameraActive(cameraWanted: Boolean, cameraBound: Boolean): AuraFxError.InvalidState? {
        if (cameraWanted || cameraBound) {
            return AuraFxError.InvalidState(
                "processFrame is disabled while the live CameraX pipeline is active; " +
                    "it is not a second camera",
            )
        }
        return null
    }
}
