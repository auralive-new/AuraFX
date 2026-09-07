package com.aurafx.sdk.api

/**
 * Per-session capture and present configuration.
 *
 * [preferredPreviewWidth]/[preferredPreviewHeight] are hints; CameraX may pick the closest
 * supported size. They are not a guarantee of exact resolution.
 */
data class SessionConfig(
    val preferredPreviewWidth: Int = 1280,
    val preferredPreviewHeight: Int = 720,
    val mirrorFrontCamera: Boolean = true,
    val enableFaceLandmarks: Boolean = true,
    val listener: AuraFxSessionListener? = null,
)
