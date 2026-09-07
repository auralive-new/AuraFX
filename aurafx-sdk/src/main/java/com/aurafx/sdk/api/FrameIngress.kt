package com.aurafx.sdk.api

/**
 * Which producer last presented to the host Surface. Used to prove the live path is
 * Camera OES and not [com.aurafx.sdk.AuraFxSession.processFrame].
 */
enum class FrameIngress {
    NONE,
    CAMERA_OES,
    PROCESS_FRAME,
}
