package com.aurafx.sdk.internal

import android.util.Log
import com.aurafx.sdk.BuildConfig

/** Filter with `adb logcat -s AuraFX`. Per-frame logs are not emitted. */
internal object AuraFxLog {
    const val TAG = "AuraFX"
    @Volatile private var lastDebugNs = 0L

    fun d(message: String) = Log.d(TAG, message)

    fun i(message: String) = Log.i(TAG, message)

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(TAG, message, throwable) else Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
    }

    /** DEBUG builds only, at most once per second. */
    fun debugThrottled(message: String) {
        if (!BuildConfig.DEBUG) return
        val now = System.nanoTime()
        if (now - lastDebugNs < 1_000_000_000L) return
        lastDebugNs = now
        Log.d(TAG, message)
    }
}
