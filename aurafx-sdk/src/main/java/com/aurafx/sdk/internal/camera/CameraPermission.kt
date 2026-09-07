package com.aurafx.sdk.internal.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.core.content.ContextCompat
import com.aurafx.sdk.api.AuraFxError

internal object CameraPermission {
    fun denied(context: Context): AuraFxError.PermissionDenied? {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) null else AuraFxError.PermissionDenied()
    }
}
