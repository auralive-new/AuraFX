package com.aurafx.sdk

import android.content.Context
import com.aurafx.sdk.api.AuraFxConfig
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.SessionConfig
import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.internal.device.DeviceCapabilities
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Process-wide AuraFX entry point. The SDK has no dependency on AuraLive types.
 *
 * Typical use:
 * 1. [initialize]
 * 2. [createSession]
 * 3. [AuraFxSession.attachPreview]
 * 4. [AuraFxSession.startCamera]
 * 5. [AuraFxSession.stopCamera] / [AuraFxSession.release]
 * 6. [release] when the host process is done with AuraFX
 */
object AuraFx {
    @Volatile private var appContext: Context? = null
    @Volatile private var config: AuraFxConfig = AuraFxConfig()
    private val initialized = AtomicBoolean(false)
    private val sessions = AtomicReference(emptyList<AuraFxSession>())

    fun initialize(context: Context, config: AuraFxConfig = AuraFxConfig()): AuraFxResult<Unit> {
        val app = context.applicationContext
        DeviceCapabilities.requireSupported(app, config.requireGles3).errorOrNull()?.let {
            return AuraFxResult.Err(it)
        }
        this.appContext = app
        this.config = config
        initialized.set(true)
        AuraFxLog.i("AuraFx.initialize ok gles3=${config.requireGles3}")
        return AuraFxResult.Ok(Unit)
    }

    fun isInitialized(): Boolean = initialized.get()

    fun createSession(context: Context, sessionConfig: SessionConfig = SessionConfig()): AuraFxResult<AuraFxSession> {
        if (!initialized.get()) {
            return AuraFxResult.Err(AuraFxError.NotInitialized())
        }
        val session = AuraFxSession(
            context = context,
            config = sessionConfig,
            instrumentationEnabled = config.enablePerformanceInstrumentation,
        )
        sessions.updateAndGet { it + session }
        AuraFxLog.i("createSession")
        return AuraFxResult.Ok(session)
    }

    fun release() {
        val open = sessions.getAndSet(emptyList())
        for (session in open) {
            session.release()
        }
        initialized.set(false)
        appContext = null
    }

    internal fun dropSession(session: AuraFxSession) {
        sessions.updateAndGet { list -> list.filterNot { it === session } }
    }
}
