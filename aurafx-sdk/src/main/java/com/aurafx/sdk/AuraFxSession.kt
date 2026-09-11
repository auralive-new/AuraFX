package com.aurafx.sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aurafx.sdk.api.ARParameters
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxInputFrame
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.AuraFxSessionListener
import com.aurafx.sdk.api.BackgroundParameters
import com.aurafx.sdk.api.BodyParameters
import com.aurafx.sdk.api.BeautyParameters
import com.aurafx.sdk.api.FaceShapeParameters
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.CapturePolicy
import com.aurafx.sdk.api.FilterParameters
import com.aurafx.sdk.api.HairParameters
import com.aurafx.sdk.api.LightingParameters
import com.aurafx.sdk.api.MakeupParameters
import com.aurafx.sdk.api.MakeupPreset
import com.aurafx.sdk.api.PerformanceSnapshot
import com.aurafx.sdk.api.SessionConfig
import com.aurafx.sdk.api.SkinParameters
import com.aurafx.sdk.ar.AREngine
import com.aurafx.sdk.ar.ARPipelineEffect
import com.aurafx.sdk.ar.ARRig
import com.aurafx.sdk.ar.AREffectCatalog
import com.aurafx.sdk.filter.FilterCatalog
import com.aurafx.sdk.filter.FilterDefinition
import com.aurafx.sdk.filter.FilterEngine
import com.aurafx.sdk.filter.FilterPipelineEffect
import com.aurafx.sdk.filter.FilterRig
import com.aurafx.sdk.gift.GiftFxCatalog
import com.aurafx.sdk.gift.GiftFxDefinition
import com.aurafx.sdk.gift.GiftFxEngine
import com.aurafx.sdk.gift.GiftPipelineEffect
import com.aurafx.sdk.beauty.BeautyEngine
import com.aurafx.sdk.beauty.BeautyPipelineEffect
import com.aurafx.sdk.beauty.BeautyRig
import com.aurafx.sdk.beauty.FaceShapeEngine
import com.aurafx.sdk.beauty.SkinEngine
import com.aurafx.sdk.effect.CameraResolveEffect
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
import com.aurafx.sdk.scene.BackgroundCatalog
import com.aurafx.sdk.scene.BackgroundEngine
import com.aurafx.sdk.scene.BackgroundPipelineEffect
import com.aurafx.sdk.scene.BackgroundRig
import com.aurafx.sdk.scene.BodyCatalog
import com.aurafx.sdk.scene.BodyEngine
import com.aurafx.sdk.scene.BodyPipelineEffect
import com.aurafx.sdk.scene.BodyRig
import com.aurafx.sdk.scene.HairCatalog
import com.aurafx.sdk.scene.HairEngine
import com.aurafx.sdk.scene.HairPipelineEffect
import com.aurafx.sdk.scene.HairRig
import com.aurafx.sdk.scene.LightingCatalog
import com.aurafx.sdk.scene.LightingEngine
import com.aurafx.sdk.scene.LightingPipelineEffect
import com.aurafx.sdk.scene.LightingRig
import com.aurafx.sdk.scene.SegmentationUploadEffect
import com.aurafx.sdk.vision.mediapipe.MediaPipeSceneAnalyzer
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
    val backgroundRig = BackgroundRig()
    val hairRig = HairRig()
    val bodyRig = BodyRig()
    val lightingRig = LightingRig()
    val backgroundEngine = BackgroundEngine(backgroundRig)
    val hairEngine = HairEngine(hairRig)
    val bodyEngine = BodyEngine(bodyRig)
    val lightingEngine = LightingEngine(lightingRig)
    val arRig = ARRig()
    val arEngine = AREngine(arRig)
    val giftEngine = GiftFxEngine()
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

    private var sceneAnalyzer: MediaPipeSceneAnalyzer? = null

    init {
        effects.register(CameraResolveEffect())
        effects.register(SegmentationUploadEffect())
        effects.register(BackgroundPipelineEffect(backgroundRig))
        effects.register(MakeupPipelineEffect(makeupRig))
        effects.register(BeautyPipelineEffect(beautyRig))
        effects.register(HairPipelineEffect(hairRig))
        effects.register(BodyPipelineEffect(bodyRig))
        effects.register(LightingPipelineEffect(lightingRig))
        effects.register(FilterPipelineEffect(filterRig))
        effects.register(ARPipelineEffect(arRig))
        effects.register(GiftPipelineEffect(giftEngine))
        if (config.enableFaceLandmarks) {
            val analyzer = MediaPipeSceneAnalyzer(
                context = appContext,
                mirrorX = { config.mirrorFrontCamera && lastFacing == LensFacing.FRONT },
                onResult = { vision.publish(it) },
            )
            sceneAnalyzer = analyzer
            camera.analyzer = analyzer
        }
        renderer.start()
    }

    fun attachPreview(surface: Surface, width: Int, height: Int): AuraFxResult<Unit> {
        if (released.get()) return AuraFxResult.Err(AuraFxError.InvalidState("Session released"))
        if (!surface.isValid) {
            return AuraFxResult.Err(AuraFxError.GpuFailure("Preview surface is not valid"))
        }
        if (width <= 0 || height <= 0) {
            return AuraFxResult.Err(
                AuraFxError.InvalidState("attachPreview requires a non-zero surface size, got ${width}x${height}"),
            )
        }
        val glError = renderer.awaitReady()
        if (glError != null) return AuraFxResult.Err(glError)
        return try {
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
            AuraFxResult.Ok(Unit)
        } catch (t: Throwable) {
            previewReady = false
            AuraFxResult.Err(AuraFxError.GpuFailure("Failed to attach preview surface", t))
        }
    }

    fun detachPreview() {
        AuraFxLog.i("detachPreview")
        previewReady = false
        if (!released.get()) renderer.detachOutput()
    }

    fun resizePreview(width: Int, height: Int) {
        if (!released.get() && width > 0 && height > 0) renderer.resize(width, height)
    }

    fun setShowUnprocessedPreview(showRaw: Boolean) {
        if (!released.get()) renderer.setShowUnprocessedPreview(showRaw)
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
        CapturePolicy.denySwitchWhileRecording(renderer.isRecording())?.let { return AuraFxResult.Err(it) }
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

    fun background(block: BackgroundParameters.() -> Unit): AuraFxSession {
        backgroundEngine.apply(block)
        return this
    }

    fun hair(block: HairParameters.() -> Unit): AuraFxSession {
        hairEngine.apply(block)
        return this
    }

    fun setHairStyle(styleId: String): AuraFxResult<Unit> {
        if (!hairEngine.setStyle(styleId)) {
            return AuraFxResult.Err(AuraFxError.UnknownEffect("Unknown hairstyle id: $styleId"))
        }
        return AuraFxResult.Ok(Unit)
    }

    fun body(block: BodyParameters.() -> Unit): AuraFxSession {
        bodyEngine.apply(block)
        return this
    }

    fun lighting(block: LightingParameters.() -> Unit): AuraFxSession {
        lightingEngine.apply(block)
        return this
    }

    fun resetBackground(): AuraFxSession {
        backgroundEngine.reset()
        return this
    }

    fun resetHair(): AuraFxSession {
        hairEngine.reset()
        return this
    }

    fun resetBody(): AuraFxSession {
        bodyEngine.reset()
        return this
    }

    fun resetLighting(): AuraFxSession {
        lightingEngine.reset()
        return this
    }

    fun backgroundParameters(): BackgroundParameters = backgroundEngine.snapshot()
    fun hairParameters(): HairParameters = hairEngine.snapshot()
    fun bodyParameters(): BodyParameters = bodyEngine.snapshot()
    fun ar(block: ARParameters.() -> Unit): AuraFxSession {
        val draft = arEngine.snapshot()
        block(draft)
        draft.clampInPlace()
        val id = draft.effectId
        if (id != null && AREffectCatalog.require(id) == null) return this
        arEngine.apply {
            effectId = draft.effectId
            intensity = draft.intensity
        }
        return this
    }

    fun setAREffect(effectId: String, intensity: Float = 0.85f): AuraFxResult<Unit> {
        if (AREffectCatalog.require(effectId) == null) {
            return AuraFxResult.Err(AuraFxError.UnknownEffect("Unknown AR effect id: $effectId"))
        }
        arEngine.setAREffect(effectId, intensity)
        return AuraFxResult.Ok(Unit)
    }

    fun setAREffectIntensity(intensity: Float): AuraFxSession {
        arEngine.setAREffectIntensity(intensity)
        return this
    }

    fun clearAREffect(): AuraFxSession {
        arEngine.clear()
        return this
    }

    fun resetAREffects(): AuraFxSession {
        arEngine.reset()
        return this
    }

    fun arParameters(): ARParameters = arEngine.snapshot()

    fun arCatalog() = arEngine.catalog()

    fun giftCatalog(): List<GiftFxDefinition> = GiftFxCatalog.samples

    /**
     * Starts a GPU gift on the existing camera graph. Does not rebind CameraX.
     */
    fun playGift(giftId: String): AuraFxResult<Long> {
        if (released.get()) return AuraFxResult.Err(AuraFxError.InvalidState("Session released"))
        val id = giftEngine.play(giftId) ?: return AuraFxResult.Err(AuraFxError.UnknownEffect("Unknown gift id: $giftId"))
        return AuraFxResult.Ok(id)
    }

    fun replayGift(): AuraFxResult<Long> {
        if (released.get()) return AuraFxResult.Err(AuraFxError.InvalidState("Session released"))
        val id = giftEngine.replay()
            ?: return AuraFxResult.Err(AuraFxError.InvalidState("No gift has been played yet"))
        return AuraFxResult.Ok(id)
    }

    fun stopGift(instanceId: Long): AuraFxSession {
        giftEngine.stop(instanceId)
        return this
    }

    fun stopAllGifts(): AuraFxSession {
        giftEngine.stopAll()
        return this
    }

    fun resetGifts(): AuraFxSession {
        giftEngine.reset()
        return this
    }

    fun activeGifts(): List<String> = giftEngine.manager.activeGiftIds()

    fun lastGiftId(): String? = giftEngine.manager.lastGiftId

    fun hairCatalog() = HairCatalog.styles
    fun backgroundCatalog() = BackgroundCatalog.items
    fun lightingCatalog() = LightingCatalog.modes
    fun bodyCatalog() = BodyCatalog.items

    fun capturePhoto(output: java.io.File, onDone: (AuraFxResult<com.aurafx.sdk.api.CapturedPhoto>) -> Unit) {
        if (released.get()) {
            onDone(AuraFxResult.Err(AuraFxError.InvalidState("Session released")))
            return
        }
        renderer.captureProcessedPhoto(output) { result ->
            mainHandler.post { onDone(result) }
        }
    }

    fun startRecording(output: java.io.File, recordAudio: Boolean = true): AuraFxResult<Unit> {
        if (released.get()) return AuraFxResult.Err(AuraFxError.InvalidState("Session released"))
        CapturePolicy.denyRecordWithoutPreview(previewReady)?.let { return AuraFxResult.Err(it) }
        return renderer.startRecording(output, recordAudio)
    }

    fun stopRecording(onDone: (AuraFxResult<com.aurafx.sdk.api.RecordedVideo>) -> Unit) {
        if (released.get()) {
            onDone(AuraFxResult.Err(AuraFxError.InvalidState("Session released")))
            return
        }
        renderer.stopRecording(onDone)
    }

    fun isRecording(): Boolean = renderer.isRecording()

    fun resetAll(): AuraFxSession {
        resetBeauty()
        resetMakeup()
        resetFilter()
        resetBackground()
        resetHair()
        resetBody()
        resetLighting()
        resetAREffects()
        resetGifts()
        return this
    }

    fun lightingParameters(): LightingParameters = lightingEngine.snapshot()

    fun filterParameters(): FilterParameters = filterEngine.parameters()

    fun visionDiagnostics(): String {
        val t = vision.latest()
        val face = if (t.landmarks != null) "face ${t.landmarks.count}pts" else "no face"
        val seg = t.segmentation?.let { "person ${"%.0f".format(it.personCoverage * 100f)}%" } ?: "no mask"
        return "$face · $seg · ${t.visionProvider.ifBlank { t.status.name.lowercase() }}"
    }

    fun filterCatalog(): List<FilterDefinition> = filterEngine.catalog()

    fun makeupParameters(): MakeupParameters = makeupEngine.snapshot()

    fun skinParameters(): SkinParameters = beautyRig.copySkin()

    fun beautyParameters(): BeautyParameters = beautyRig.copyBeauty()

    fun faceShapeParameters(): FaceShapeParameters = beautyRig.copyShape()

    fun registerEffect(effect: Effect) = effects.register(effect)

    fun unregisterEffect(id: String) = effects.unregister(id)

    fun performanceSnapshot(): PerformanceSnapshot {
        val snap = performance.snapshot()
        val track = vision.latest()
        val state = when {
            released.get() -> "released"
            camera.isBound() -> "camera_live"
            previewReady -> "preview_attached"
            else -> "idle"
        }
        return snap.copy(
            trackingStatus = track.status.name,
            trackedFaces = maxOf(track.faces.size, track.meshes.size, if (track.landmarks != null) 1 else 0),
            cameraBound = camera.isBound(),
            cameraFacing = lastFacing.name,
            sdkState = state,
        )
    }

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
            sceneAnalyzer?.close()
        } catch (_: Throwable) {
        }
        sceneAnalyzer = null
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
