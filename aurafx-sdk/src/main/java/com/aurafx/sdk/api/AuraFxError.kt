package com.aurafx.sdk.api

/**
 * Typed failures. None of these are thrown across the public API; they are returned as
 * [AuraFxResult.Err] or delivered to [AuraFxSessionListener.onError].
 */
sealed class AuraFxError {
    abstract val message: String
    open val cause: Throwable? get() = null

    data class NotInitialized(
        override val message: String = "AuraFx.initialize() must be called first",
    ) : AuraFxError()

    data class InvalidState(
        override val message: String,
    ) : AuraFxError()

    data class PermissionDenied(
        override val message: String = "CAMERA permission is not granted",
    ) : AuraFxError()

    data class CameraUnavailable(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AuraFxError()

    data class UnsupportedDevice(
        override val message: String,
    ) : AuraFxError()

    data class GpuFailure(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AuraFxError()

    data class LifecycleInterrupted(
        override val message: String,
    ) : AuraFxError()

    data class ResourceCleanup(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AuraFxError()

    data class UnknownFilter(
        override val message: String,
    ) : AuraFxError()

    data class UnknownEffect(
        override val message: String,
    ) : AuraFxError()
}
