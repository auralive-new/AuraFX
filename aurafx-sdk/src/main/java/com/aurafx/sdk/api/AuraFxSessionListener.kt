package com.aurafx.sdk.api

/**
 * Optional session callbacks. All methods are invoked on the main thread.
 */
interface AuraFxSessionListener {
    fun onCameraStarted(facing: LensFacing) {}
    fun onFirstFrame(timestampNs: Long) {}
    fun onError(error: AuraFxError) {}
    fun onCameraStopped() {}
}
