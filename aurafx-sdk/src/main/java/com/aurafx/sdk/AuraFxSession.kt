package com.aurafx.sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxInputFrame
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.AuraFxSessionListener
import com.aurafx.sdk.api.BeautyParameters
import com.aurafx.sdk.api.FaceShapeParameters
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.FilterParameters
import com.aurafx.sdk.api.MakeupParameters
import com.aurafx.sdk.api.MakeupPreset
import com.aurafx.sdk.api.PerformanceSnapshot
import com.aurafx.sdk.api.SessionConfig
import com.aurafx.sdk.api.SkinParameters
import com.aurafx.sdk.beauty.BeautyEngine
import com.aurafx.sdk.filter.FilterCatalog
import com.aurafx.sdk.filter.FilterDefinition
import com.aurafx.sdk.filter.FilterEngine
import com.aurafx.sdk.filter.FilterPipelineEffect
import com.aurafx.sdk.filter.FilterRig
import com.aurafx.sdk.beauty.BeautyPipelineEffect
import com.aurafx.sdk.beauty.BeautyRig
import com.aurafx.sdk.beauty.FaceShapeEngine
import com.aurafx.sdk.beauty.SkinEngine
import com.aurafx.sdk.effect.Effect
import com.aurafx.sdk.effect.EffectManager
import com.aurafx.sdk.makeup.MakeupEngine
import com.aurafx.sdk.makeup.MakeupPipelineEffect
import com.aurafx.sdk.makeup.MakeupRig
import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.internal.camera.AuraFxCameraController
import com.aurafx.sdk.internal.camera.CameraPermission
import com.aurafx.sdk.internal.device.DeviceCapabilities
import com.aurafx.sdk.internal.render.AuraFxRenderThread
import com.aurafx.sdk.performance.PerformanceManager
import com.aurafx.sdk.pipeline.FramePipeline
import com.aurafx.sdk.pipeline.LiveIngressPolicy
import com.aurafx.sdk.vision.VisionProcessor
import com.aurafx.sdk.vision.mediapipe.MediaPipeFaceLandmarkerAnalyzer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One camera + GPU pipeline. Create via [AuraFx.createSession].
 */
class AuraFxSession internal constructor(
    context: Context,
    private val config: SessionConfig,
    instrumentationEnabled: Boolean,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listener: AuraFxSessionListener? = config.listener
    private val released = AtomicBoolean(false)
    private val wantRunning = AtomicBoolean(false)
    private val pausedByLifecycle = AtomicBoolean(false)

    val pipeline = FramePipeline()
    val vision = VisionProcessor()
    val beautyRig = BeautyRig()
    val makeupRig = MakeupRig()
    val skinEngine = SkinEngine(beautyRig)
    val beautyEngine = BeautyEngine(beautyRig)
    val faceShapeEngine = FaceShapeEngine(beautyRig)
    val makeupEngine = MakeupEngine(makeupRig)
    val filterRig = FilterRig()
    val filterEngine = FilterEngine(filterRig)
    val performance = PerformanceManager(
        enabled = instrumentationEnabled,
        nativeHeapProbe = { DeviceCapabilities.nativeHeapAllocatedBytes() },
    )
    val effects = EffectManager(performance)

    private val renderer = AuraFxRenderThread(
        performance = performance,
        pipeline = pipeline,
        effects = effects,
        vision = vision,
        mirrorFrontCamera = config.mirrorFrontCamera,
        listener = listener,
        mainPoster = { mainHandler.post(it) },
    )
    private val camera = AuraFxCameraController(
        appContext,
        config,
        onPreviewResolution = { size -> renderer.setCameraBufferSize(size.width, size.height) },
    )

    @Volatile private var lifecycleOwner: LifecycleOwner? = null
    @Volatile private var previewReady = false
    @Volatile private var lastFacing: LensFacing = LensFacing.FRONT

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
            if (wantRunning.get() && camera.isBound()) {
                AuraFxLog.i("lifecycle onPause -> unbind camera")
                pausedByLifecycle.set(true)
                renderer.setLiveCameraActive(false)
                camera.stop()
            }
        }

        override fun onResume(owner: LifecycleOwner) {
            if (wantRunning.get() && pausedByLifecycle.get()) {
                if (!previewReady) {
                    AuraFxLog.i("lifecycle onResume waiting for preview surface")
                    return
                }
                AuraFxLog.i("lifecycle onResume -> rebind camera")
                pausedByLifecycle.set(false)
                startCameraInternal(owner, lastFacing)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            stopCamera()
            owner.lifecycle.removeObserver(this)
        }
    }

    private var faceAnalyzer: MediaPipeFaceLandmarkerAnalyzer? = null

    init {
        effects.register(MakeupPipelineEffect(makeupRig))
        effects.register(BeautyPipelineEffect(beautyRig))
        effects.register(FilterPipelineEffect(filterRig))
        if (config.enableFaceLandmarks) {
            val analyzer = MediaPipeFaceLandmarkerAnalyzer(
                context = appContext,
                mirrorX = { config.mirrorFrontCamera && lastFacing == LensFacing.FRONT },
                onResult = { vision.publish(it) },
            )
            faceAnalyzer = analyzer
            camera.analyzer = analyzer
        }
        renderer.start()
    }

    fun attachPreview(surface: Surface, width: Int, height: Int): AuraFxResult<Unit> {
        if (released.get()) return AuraFxResult.Err(AuraFxError.InvalidState("Session released"))
        if (!surface.isValid) {
            return AuraFxResult.Err(AuraFxError.GpuFailure("Preview surface is not valid"))
        }
        val glError = renderer.awaitReady()
        if (glError != null) return AuraFxResult.Err(glError)
        renderer.attachOutput(surface, width, height)
        previewReady = true
        AuraFxLog.i("attachPreview ${width}x${height}")
        if (wantRunning.get() && pausedByLifecycle.get()) {
            val owner = lifecycleOwner
            if (owner != null) {
                pausedByLifecycle.set(false)
                startCameraInternal(owner, lastFacing)
            }
        }
        return AuraFxResult.Ok(Unit)
    }

    fun detachPreview() {
        AuraFxLog.i("detachPreview")
        previewReady = false
        if (!released.get()) renderer.detachOutput()
    }

    fun resizePreview(width: Int, height: Int) {
        if (!released.get()) renderer.resize(width, height)
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, lensFacing: LensFacing): AuraFxResult<Unit> {
        if (released.get()) return AuraFxResult.Err(AuraFxError.InvalidState("Session released"))
        CameraPermission.denied(appContext)?.let { return AuraFxResult.Err(it) }
        DeviceCapabilities.requireSupported(appContext, requireGles3 = true).errorOrNull()?.let {
            return AuraFxResult.Err(it)
        }
        if (!DeviceCapabilities.hasLens(appContext, lensFacing)) {
            return AuraFxResult.Err(
                AuraFxError.CameraUnavailable("No ${lensFacing.name.lowercase()} camera on this device"),
            )
        }
        val glError = renderer.awaitReady()
        if (glError != null) return AuraFxResult.Err(glError)
        if (!previewReady) {
            return AuraFxResult.Err(AuraFxError.InvalidState("attachPreview() must be called before startCamera()"))
        }
        this.lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        this.lifecycleOwner = lifecycleOwner
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        wantRunning.set(true)
        lastFacing = lensFacing
        performance.markCameraStart()
        AuraFxLog.i("startCamera facing=$lensFacing")
        startCameraInternal(lifecycleOwner, lensFacing)
        return AuraFxResult.Ok(Unit)
    }

    fun switchCamera(): AuraFxResult<Unit> {
        if (released.get()) return AuraFxResult.Err(AuraFxError.InvalidState("Session released"))
        val owner = lifecycleOwner
            ?: return AuraFxResult.Err(AuraFxError.InvalidState("Camera is not started"))
        val next = if (lastFacing == LensFacing.FRONT) LensFacing.BACK else LensFacing.FRONT
        if (!DeviceCapabilities.hasLens(appContext, next)) {
            return AuraFxResult.Err(AuraFxError.CameraUnavailable("Requested camera is not available"))
        }
        performance.markCameraStart()
        try {
            renderer.setFacing(next)
            camera.switchCamera(
                lifecycleOwner = owner,
                previewSurface = renderer.cameraPreviewSurface(),
                rotation = currentDisplayRotation(),
                onBound = { facing ->
                    lastFacing = facing
                    renderer.setFacing(facing)
                    mainHandler.post { listener?.onCameraStarted(facing) }
                },
                onError = { error -> mainHandler.post { listener?.onError(error) } },
            )
            return AuraFxResult.Ok(Unit)
        } catch (t: Throwable) {
            return AuraFxResult.Err(AuraFxError.CameraUnavailable("switchCamera failed", t))
        }
    }

    fun stopCamera(): AuraFxResult<Unit> {
        wantRunning.set(false)
        pausedByLifecycle.set(false)
        renderer.setLiveCameraActive(false)
        AuraFxLog.i("stopCamera")
        val result = camera.stop()
        mainHandler.post { listener?.onCameraStopped() }
        return result
    }

    /**
     * External CPU frame ingress. Does not open a second camera.
     */
    fun processFrame(input: AuraFxInputFrame): AuraFxResult<Unit> {
        if (released.get()) return AuraFxResult.Err(AuraFxError.InvalidState("Session released"))
        LiveIngressPolicy.denyProcessFrameIfCameraActive(wantRunning.get(), camera.isBound())?.let {
            AuraFxLog.w(it.message)
            return AuraFxResult.Err(it)
        }
        if (input.width <= 0 || input.height <= 0) {
            return AuraFxResult.Err(AuraFxError.InvalidState("Invalid frame size"))
        }
        val expected = input.width * input.height * 4
        if (input.rgba8888.remaining() < expected) {
            return AuraFxResult.Err(AuraFxError.InvalidState("RGBA buffer smaller than width*height*4"))
        }
        renderer.submitExternalFrame(input)
        return AuraFxResult.Ok(Unit)
    }

    fun skin(block: SkinParameters.() -> Unit): AuraFxSession {
        skinEngine.apply(block)
        return this
    }

    fun beauty(block: BeautyParameters.() -> Unit): AuraFxSession {
        beautyEngine.apply(block)
        return this
    }

    fun faceShape(block: FaceShapeParameters.() -> Unit): AuraFxSession {
        faceShapeEngine.apply(block)
        return this
    }

    fun resetBeauty(): AuraFxSession {
        beautyRig.reset()
        return this
    }

    fun makeup(block: MakeupParameters.() -> Unit): AuraFxSession {
        makeupEngine.apply(block)
        return this
    }

    fun applyMakeupPreset(preset: MakeupPreset): AuraFxSession {
        makeupEngine.preset(preset)
        return this
    }

    fun resetMakeup(): AuraFxSession {
        makeupEngine.reset()
        return this
    }

    fun filter(block: FilterParameters.() -> Unit): AuraFxSession {
        val draft = filterEngine.parameters()
        block(draft)
        draft.clampInPlace()
        val id = draft.id
        if (id != null && FilterCatalog.require(id) == null) {
            return this
        }
        filterEngine.apply {
            this.id = draft.id
            intensity = draft.intensity
            finish = draft.finish
            finishIntensity = draft.finishIntensity
        }
        return this
    }

    fun setFilter(filterId: String, intensity: Float): AuraFxResult<Unit> {
        if (FilterCatalog.require(filterId) == null) {
            return AuraFxResult.Err(AuraFxError.UnknownFilter("Unknown filter id: $filterId"))
        }
        filterEngine.setFilter(filterId, intensity)
        return AuraFxResult.Ok(Unit)
    }

    fun clearFilter(): AuraFxSession {
        filterEngine.clear()
        return this
    }

    fun resetFilter(): AuraFxSession {
        filterEngine.reset()
        return this
    }

    fun filterParameters(): FilterParameters = filterEngine.parameters()

    fun filterCatalog(): List<FilterDefinition> = filterEngine.catalog()

    fun makeupParameters(): MakeupParameters = makeupEngine.snapshot()

    fun skinParameters(): SkinParameters = beautyRig.copySkin()

    fun beautyParameters(): BeautyParameters = beautyRig.copyBeauty()

    fun faceShapeParameters(): FaceShapeParameters = beautyRig.copyShape()

    fun registerEffect(effect: Effect) = effects.register(effect)

    fun unregisterEffect(id: String) = effects.unregister(id)

    fun performanceSnapshot(): PerformanceSnapshot = performance.snapshot()

    fun isCameraBound(): Boolean = camera.isBound()

    fun currentLensFacing(): LensFacing = lastFacing

    fun release() {
        if (!released.compareAndSet(false, true)) return
        wantRunning.set(false)
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
        AuraFxLog.i("session release")
        renderer.setLiveCameraActive(false)
        vision.clear()
        try {
            faceAnalyzer?.close()
        } catch (_: Throwable) {
        }
        faceAnalyzer = null
        camera.analyzer = null
        try {
            camera.release()
        } catch (t: Throwable) {
            listener?.onError(AuraFxError.ResourceCleanup("Camera release failed", t))
        }
        try {
            renderer.release()
        } catch (t: Throwable) {
            listener?.onError(AuraFxError.ResourceCleanup("Renderer release failed", t))
        }
        AuraFx.dropSession(this)
    }

    private fun startCameraInternal(owner: LifecycleOwner, facing: LensFacing) {
        renderer.setFacing(facing)
        renderer.setLiveCameraActive(true)
        camera.start(
            lifecycleOwner = owner,
            facing = facing,
            previewSurface = renderer.cameraPreviewSurface(),
            rotation = currentDisplayRotation(),
            onBound = { boundFacing ->
                lastFacing = boundFacing
                renderer.setFacing(boundFacing)
                mainHandler.post { listener?.onCameraStarted(boundFacing) }
            },
            onError = { error -> mainHandler.post { listener?.onError(error) } },
        )
    }

    private fun currentDisplayRotation(): Int {
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        return wm.defaultDisplay.rotation
    }
}
