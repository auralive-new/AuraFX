package com.aurafx.sdk.internal.camera

import android.content.Context
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
 * Single CameraX bind per session. Preview stays on the GPU path.
 * Optional ImageAnalysis (KEEP_ONLY_LATEST) shares that bind for MediaPipe;
 * it is not a second camera pipeline.
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
    private val visionExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "aurafx-vision").apply { isDaemon = true }
    }
    @Volatile var analyzer: ImageAnalysis.Analyzer? = null
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
        visionExecutor.shutdown()
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
        val attempts = listOf(
            BindAttempt(withAnalysis = analyzer != null, withFps = false, rgba = false),
            BindAttempt(withAnalysis = analyzer != null, withFps = true, rgba = false),
            BindAttempt(withAnalysis = analyzer != null, withFps = false, rgba = true),
            BindAttempt(withAnalysis = analyzer != null, withFps = true, rgba = true),
            BindAttempt(withAnalysis = false, withFps = true, rgba = false),
            BindAttempt(withAnalysis = false, withFps = false, rgba = false),
        )
        var lastError: Throwable? = null
        for (attempt in attempts) {
            try {
                cameraProvider.unbindAll()
                val preview = buildPreview(resolutionSelector, rotation, attempt.withFps)
                preview.setSurfaceProvider { request ->
                    providePreviewSurface(request, previewSurface)
                }
                val analysis = if (attempt.withAnalysis) buildAnalysis(rotation, attempt.rgba) else null
                if (analysis != null) {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                    AuraFxLog.i(
                        "CameraX bound facing=$facing useCases=Preview+ImageAnalysis " +
                            "fps=${attempt.withFps} rgba=${attempt.rgba}",
                    )
                } else {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                    AuraFxLog.i("CameraX bound facing=$facing useCases=Preview-only fps=${attempt.withFps}")
                }
                currentFacing = facing
                bound.set(true)
                return
            } catch (t: Throwable) {
                lastError = t
                AuraFxLog.w(
                    "CameraX bind failed analysis=${attempt.withAnalysis} fps=${attempt.withFps}",
                    t,
                )
            }
        }
        bound.set(false)
        throw lastError ?: IllegalStateException("CameraX bind failed")
    }

    private data class BindAttempt(val withAnalysis: Boolean, val withFps: Boolean, val rgba: Boolean)

    private fun buildPreview(
        resolutionSelector: ResolutionSelector,
        rotation: Int,
        withFps: Boolean,
    ): Preview {
        val builder = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
        if (withFps) {
            val minFps = config.targetMinFps.coerceAtLeast(1)
            val maxFps = config.targetMaxFps.coerceAtLeast(minFps)
            builder.setTargetFrameRate(Range(minFps, maxFps))
        }
        return builder.build()
    }

    private fun buildAnalysis(rotation: Int, rgba: Boolean): ImageAnalysis? {
        val boundAnalyzer = analyzer ?: return null
        val analysisSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(480, 640),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()
        val builder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(analysisSelector)
            .setTargetRotation(rotation)
        if (rgba) {
            builder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        } else {
            builder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        }
        return builder.build().also { it.setAnalyzer(visionExecutor, boundAnalyzer) }
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
