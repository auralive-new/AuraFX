package com.aurafx.sdk.internal.device

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.LensFacing

internal object DeviceCapabilities {
    fun requireSupported(context: Context, requireGles3: Boolean): AuraFxResult<Unit> {
        if (requireGles3 && !hasGles3(context)) {
            return AuraFxResult.Err(
                AuraFxError.UnsupportedDevice("OpenGL ES 3.0 is required and not available"),
            )
        }
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return AuraFxResult.Err(AuraFxError.UnsupportedDevice("No camera available on this device"))
        }
        return AuraFxResult.Ok(Unit)
    }

    fun hasGles3(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.deviceConfigurationInfo.reqGlEsVersion >= 0x00030000
    }

    fun hasLens(context: Context, facing: LensFacing): Boolean {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val wanted = when (facing) {
            LensFacing.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
            LensFacing.BACK -> CameraCharacteristics.LENS_FACING_BACK
        }
        return manager.cameraIdList.any { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == wanted
        }
    }

    fun nativeHeapAllocatedBytes(): Long? {
        return if (Build.VERSION.SDK_INT >= 0) {
            android.os.Debug.getNativeHeapAllocatedSize()
        } else {
            null
        }
    }
}
