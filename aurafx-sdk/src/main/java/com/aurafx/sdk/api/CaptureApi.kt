package com.aurafx.sdk.api

import java.io.File

data class CapturedPhoto(
    val file: File,
    val width: Int,
    val height: Int,
    val timestampNs: Long,
    val processed: Boolean,
)

data class RecordedVideo(
    val file: File,
    val width: Int,
    val height: Int,
    val durationUs: Long,
    val hasAudio: Boolean,
    val frameCount: Long,
)

enum class CameraSwitchDuringRecord {
    /** Switch is refused; recording and the current camera stay running. */
    Refuse,
}

object CapturePolicy {
    val cameraSwitchDuringRecord: CameraSwitchDuringRecord = CameraSwitchDuringRecord.Refuse

    fun denySwitchWhileRecording(recording: Boolean): AuraFxError? {
        if (!recording) return null
        return AuraFxError.InvalidState(
            "Camera switch is refused while recording processed video. Stop recording first. " +
                "Policy=${cameraSwitchDuringRecord.name}",
        )
    }

    fun denyRecordWithoutPreview(previewReady: Boolean): AuraFxError? {
        if (previewReady) return null
        return AuraFxError.InvalidState("attachPreview() must succeed before recording")
    }
}
