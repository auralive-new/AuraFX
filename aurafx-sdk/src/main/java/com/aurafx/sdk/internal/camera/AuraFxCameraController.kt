package com.aurafx.sdk.internal.camera

import android.content.Context
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.SessionConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single CameraX bind per session. Rebind replaces the previous use-cases; a second
 * ProcessCameraProvider stack is never created.
 */
internal class AuraFxCameraController(
    private val appContext: Context,
    private val config: SessionConfig,
) {
    private val bound = AtomicBoolean(false)
    private val cameraExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "aurafx-camera").apply { isDaemon = true }
    }
    @Volatile private var provider: ProcessCameraProvider? = null
    @Volatile var currentFacing: LensFacing = LensFacing.FRONT
        private set

    fun start(
        lifecycleOwner: LifecycleOwner,
        facing: LensFacing,
        previewSurface: Surface,
        rotation: Int,
        onBound: (LensFacing) -> Unit,
        onError: (AuraFxError) -> Unit,
    ) {
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                try {
                    val cameraProvider = future.get()
                    provider = cameraProvider
                    bindLocked(cameraProvider, lifecycleOwner, facing, previewSurface, rotation)
                    onBound(facing)
                } catch (t: Throwable) {
                    onError(AuraFxError.CameraUnavailable("Failed to obtain CameraX provider", t))
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        previewSurface: Surface,
        rotation: Int,
        onBound: (LensFacing) -> Unit,
        onError: (AuraFxError) -> Unit,
    ) {
        val next = if (currentFacing == LensFacing.FRONT) LensFacing.BACK else LensFacing.FRONT
        val cameraProvider = provider
        if (cameraProvider == null) {
            onError(AuraFxError.InvalidState("Camera is not started"))
            return
        }
        try {
            bindLocked(cameraProvider, lifecycleOwner, next, previewSurface, rotation)
            onBound(next)
        } catch (t: Throwable) {
            onError(AuraFxError.CameraUnavailable("Failed to switch camera", t))
        }
    }

    fun stop(): AuraFxResult<Unit> {
        val cameraProvider = provider
        return try {
            cameraProvider?.unbindAll()
            bound.set(false)
            AuraFxResult.Ok(Unit)
        } catch (t: Throwable) {
            AuraFxResult.Err(AuraFxError.LifecycleInterrupted("Failed to unbind camera: ${t.message}"))
        }
    }

    fun release() {
        try {
            provider?.unbindAll()
        } catch (_: Throwable) {
        }
        bound.set(false)
        provider = null
        cameraExecutor.shutdown()
    }

    fun isBound(): Boolean = bound.get()

    private fun bindLocked(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        facing: LensFacing,
        previewSurface: Surface,
        rotation: Int,
    ) {
        cameraProvider.unbindAll()
        val selector = when (facing) {
            LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(config.preferredPreviewWidth, config.preferredPreviewHeight),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()
        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
        preview.setSurfaceProvider { request ->
            request.provideSurface(previewSurface, cameraExecutor) { }
        }
        try {
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
        } catch (t: Throwable) {
            bound.set(false)
            throw t
        }
        currentFacing = facing
        bound.set(true)
    }
}

internal fun displayRotationToSurfaceRotation(degrees: Int): Int = when (degrees) {
    0 -> Surface.ROTATION_0
    90 -> Surface.ROTATION_90
    180 -> Surface.ROTATION_180
    270 -> Surface.ROTATION_270
    else -> Surface.ROTATION_0
}
