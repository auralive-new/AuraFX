package com.aurafx.sdk.internal.camera

import android.content.Context
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.SessionConfig
import com.aurafx.sdk.internal.AuraFxLog
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Single CameraX Preview bind per session. ImageAnalysis is intentionally not used.
 * Rebind always calls [ProcessCameraProvider.unbindAll] first so a second camera stack
 * cannot remain open.
 */
internal class AuraFxCameraController(
    private val appContext: Context,
    private val config: SessionConfig,
    private val onPreviewResolution: (Size) -> Unit,
) {
    private val bound = AtomicBoolean(false)
    private val bindGeneration = AtomicInteger(0)
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
        val generation = bindGeneration.incrementAndGet()
        AuraFxLog.i("CameraX start requested facing=$facing rotation=$rotation gen=$generation")
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                if (generation != bindGeneration.get()) {
                    AuraFxLog.i("CameraX start ignored stale gen=$generation current=${bindGeneration.get()}")
                    return@addListener
                }
                try {
                    val cameraProvider = future.get()
                    provider = cameraProvider
                    bindLocked(cameraProvider, lifecycleOwner, facing, previewSurface, rotation)
                    onBound(facing)
                } catch (t: Throwable) {
                    AuraFxLog.e("CameraX provider/bind failed", t)
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
        val generation = bindGeneration.incrementAndGet()
        AuraFxLog.i("CameraX switch ${currentFacing} -> $next gen=$generation")
        try {
            bindLocked(cameraProvider, lifecycleOwner, next, previewSurface, rotation)
            onBound(next)
        } catch (t: Throwable) {
            AuraFxLog.e("CameraX switch failed", t)
            onError(AuraFxError.CameraUnavailable("Failed to switch camera", t))
        }
    }

    fun stop(): AuraFxResult<Unit> {
        bindGeneration.incrementAndGet()
        val cameraProvider = provider
        return try {
            AuraFxLog.i("CameraX unbindAll (stop)")
            cameraProvider?.unbindAll()
            bound.set(false)
            AuraFxResult.Ok(Unit)
        } catch (t: Throwable) {
            AuraFxLog.e("CameraX unbind failed", t)
            AuraFxResult.Err(AuraFxError.LifecycleInterrupted("Failed to unbind camera: ${t.message}"))
        }
    }

    fun release() {
        bindGeneration.incrementAndGet()
        try {
            AuraFxLog.i("CameraX release")
            provider?.unbindAll()
        } catch (t: Throwable) {
            AuraFxLog.w("CameraX release unbind", t)
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
        AuraFxLog.i("CameraX unbindAll before bind facing=$facing (single pipeline)")
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
            providePreviewSurface(request, previewSurface)
        }
        try {
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
        } catch (t: Throwable) {
            bound.set(false)
            throw t
        }
        currentFacing = facing
        bound.set(true)
        AuraFxLog.i("CameraX bound facing=$facing useCases=Preview-only")
    }

    private fun providePreviewSurface(request: SurfaceRequest, previewSurface: Surface) {
        val size = request.resolution
        AuraFxLog.i("CameraX SurfaceRequest ${size.width}x${size.height}")
        try {
            onPreviewResolution(size)
        } catch (t: Throwable) {
            AuraFxLog.e("Failed to size SurfaceTexture", t)
            request.willNotProvideSurface()
            return
        }
        if (!previewSurface.isValid) {
            AuraFxLog.e("Preview Surface invalid; willNotProvideSurface")
            request.willNotProvideSurface()
            return
        }
        request.provideSurface(previewSurface, cameraExecutor) { result ->
            AuraFxLog.i("CameraX SurfaceRequest result=${result.resultCode}")
        }
    }
}
