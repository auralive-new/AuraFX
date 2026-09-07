package com.aurafx.sdk.internal

import android.util.Log

/** Filter with `adb logcat -s AuraFX`. Per-frame logs are not emitted. */
internal object AuraFxLog {
    const val TAG = "AuraFX"

    fun d(message: String) = Log.d(TAG, message)

    fun i(message: String) = Log.i(TAG, message)

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(TAG, message, throwable) else Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
    }
}
