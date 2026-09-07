package com.aurafx.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aurafx.sample.databinding.ActivitySampleBinding
import com.aurafx.sdk.AuraFx
import com.aurafx.sdk.AuraFxSession
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.AuraFxSessionListener
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.SessionConfig

/**
 * Independent SDK harness. This is not AuraLive and must not be copied into AuraLive.
 */
class SampleActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private lateinit var binding: ActivitySampleBinding
    private val mainHandler = Handler(Looper.getMainLooper())
    private var session: AuraFxSession? = null
    private var surfaceReady = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var pendingFacing = LensFacing.FRONT
    private var userWantsCamera = false
    private val metricsTicker = object : Runnable {
        override fun run() {
            val snap = session?.performanceSnapshot()
            if (snap != null) {
                binding.metricsText.text = buildString {
                    append("fps=").append("%.1f".format(snap.fps))
                    append("  processMs=").append("%.2f".format(snap.frameProcessTimeMs))
                    append("  gpuMs=").append(snap.gpuTimeMs?.let { "%.2f".format(it) } ?: "n/a")
                    append("\ndropped=").append(snap.droppedFrames)
                    append("  presented=").append(snap.presentedFrames)
                    append("  camStartMs=").append(snap.cameraStartupMs?.let { "%.0f".format(it) } ?: "n/a")
                    append("\njavaHeap=").append(snap.javaHeapUsedBytes)
                    append("  nativeHeap=").append(snap.nativeHeapAllocatedBytes ?: "n/a")
                    append("\nlastTs=").append(snap.lastCameraFrameTimestampNs ?: "n/a")
                    append("  facing=").append(session?.currentLensFacing())
                    append("  ingress=").append(snap.lastIngress)
                    append("  admitted=").append(snap.pipelineAdmitted)
                    val track = session?.vision?.latest()
                    append("\nvision=").append(track?.visionProvider ?: "none")
                    append("  status=").append(track?.status)
                    append("  lm=").append(track?.landmarks?.count ?: 0)
                }
            }
            mainHandler.postDelayed(this, 500)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            setStatus("Camera permission granted")
            ensureSessionAndMaybeStart()
        } else {
            userWantsCamera = false
            setStatus("CAMERA permission denied", error = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.previewSurface.holder.addCallback(this)

        when (val result = AuraFx.initialize(this)) {
            is AuraFxResult.Ok -> setStatus("SDK initialized")
            is AuraFxResult.Err -> setStatus("Initialize failed: ${result.error.message}", error = true)
        }

        binding.startButton.setOnClickListener {
            pendingFacing = session?.currentLensFacing() ?: LensFacing.FRONT
            userWantsCamera = true
            requestPermissionAndStart()
        }
        binding.stopButton.setOnClickListener {
            userWantsCamera = false
            session?.stopCamera()
            setStatus("Camera stop requested")
        }
        binding.switchButton.setOnClickListener {
            when (val result = session?.switchCamera()) {
                is AuraFxResult.Ok -> setStatus("Switch requested")
                is AuraFxResult.Err -> setStatus(result.error.message, error = true)
                null -> setStatus("No session", error = true)
            }
        }
        binding.cycleButton.setOnClickListener {
            userWantsCamera = true
            runStartStopCycles()
        }
        binding.releaseButton.setOnClickListener {
            userWantsCamera = false
            session?.release()
            session = null
            setStatus("Session released")
        }
        mainHandler.post(metricsTicker)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        session?.resizePreview(width, height)
        if (userWantsCamera && hasCameraPermission()) {
            ensureSessionAndMaybeStart()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        session?.detachPreview()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(metricsTicker)
        session?.release()
        session = null
        super.onDestroy()
    }

    private fun requestPermissionAndStart() {
        if (hasCameraPermission()) {
            ensureSessionAndMaybeStart()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureSessionAndMaybeStart() {
        if (!userWantsCamera) return
        if (!AuraFx.isInitialized()) {
            setStatus("SDK not initialized", error = true)
            return
        }
        if (session == null) {
            val created = AuraFx.createSession(
                this,
                SessionConfig(
                    listener = object : AuraFxSessionListener {
                        override fun onCameraStarted(facing: LensFacing) {
                            setStatus("Camera started: $facing")
                        }

                        override fun onFirstFrame(timestampNs: Long) {
                            setStatus("First GPU frame ts=$timestampNs facing=${session?.currentLensFacing()}")
                        }

                        override fun onError(error: AuraFxError) {
                            setStatus(error.message, error = true)
                            Toast.makeText(this@SampleActivity, error.message, Toast.LENGTH_SHORT).show()
                        }

                        override fun onCameraStopped() {
                            setStatus("Camera stopped")
                        }
                    },
                ),
            )
            when (created) {
                is AuraFxResult.Ok -> {
                    session = created.value
                    bindBeautySliders(created.value)
                }
                is AuraFxResult.Err -> {
                    setStatus(created.error.message, error = true)
                    return
                }
            }
        }
        val current = session ?: return
        if (!surfaceReady) {
            setStatus("Waiting for preview surface")
            return
        }
        val surface = binding.previewSurface.holder.surface
        when (val attached = current.attachPreview(surface, surfaceWidth, surfaceHeight)) {
            is AuraFxResult.Err -> {
                setStatus(attached.error.message, error = true)
                return
            }
            is AuraFxResult.Ok -> Unit
        }
        when (val started = current.startCamera(this, pendingFacing)) {
            is AuraFxResult.Ok -> setStatus("startCamera accepted ($pendingFacing)")
            is AuraFxResult.Err -> setStatus(started.error.message, error = true)
        }
    }

    private fun runStartStopCycles() {
        if (!hasCameraPermission()) {
            requestPermissionAndStart()
            return
        }
        setStatus("Running 5 start/stop cycles")
        var remaining = 5
        fun step() {
            if (remaining <= 0) {
                setStatus("Start/stop cycles finished")
                return
            }
            remaining -= 1
            session?.stopCamera()
            mainHandler.postDelayed({
                pendingFacing = session?.currentLensFacing() ?: LensFacing.FRONT
                ensureSessionAndMaybeStart()
                mainHandler.postDelayed({ step() }, 700)
            }, 400)
        }
        step()
    }

    private fun bindBeautySliders(target: AuraFxSession) {
        val host = binding.sliderHost
        host.removeAllViews()
        fun row(name: String, signed: Boolean = false, read: () -> Float, write: (Float) -> Unit) {
            val label = TextView(this)
            label.setTextColor(ContextCompat.getColor(this, R.color.text))
            label.textSize = 12f
            val bar = SeekBar(this)
            bar.max = 200
            fun display(v: Float) {
                label.text = "$name  ${"%.2f".format(v)}"
            }
            val initial = read()
            bar.progress = if (signed) ((initial + 1f) * 100f).toInt().coerceIn(0, 200) else (initial * 200f).toInt().coerceIn(0, 200)
            display(initial)
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val v = if (signed) progress / 100f - 1f else progress / 200f
                    write(v)
                    display(read())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            host.addView(label)
            host.addView(bar)
        }
        val catalog = target.filterCatalog()
        val filterLabel = TextView(this)
        filterLabel.setTextColor(ContextCompat.getColor(this, R.color.text))
        filterLabel.textSize = 12f
        fun showFilter() {
            val p = target.filterParameters()
            filterLabel.text = "Filter  ${p.id ?: "none"}  i=${"%.2f".format(p.intensity)}"
        }
        showFilter()
        host.addView(filterLabel)
        val nextFilter = android.widget.Button(this)
        nextFilter.text = "Next filter"
        nextFilter.setOnClickListener {
            val ids = catalog.map { it.id }
            val cur = target.filterParameters().id
            val idx = ids.indexOf(cur)
            val id = ids[(idx + 1 + ids.size) % ids.size]
            target.setFilter(id, target.filterParameters().intensity.takeIf { it > 0f } ?: 0.65f)
            showFilter()
        }
        host.addView(nextFilter)
        val clearF = android.widget.Button(this)
        clearF.text = "Clear filter"
        clearF.setOnClickListener {
            target.clearFilter()
            showFilter()
        }
        host.addView(clearF)
        row("Filter intensity", read = {
            target.filterParameters().intensity
        }, write = { v ->
            val id = target.filterParameters().id ?: catalog.first().id
            target.setFilter(id, v)
            showFilter()
        })
        val bgLabel = TextView(this)
        bgLabel.setTextColor(ContextCompat.getColor(this, R.color.text))
        bgLabel.textSize = 12f
        fun showBg() {
            val p = target.backgroundParameters()
            bgLabel.text = "Background  ${p.id ?: "none"}  i=${"%.2f".format(p.intensity)}"
        }
        showBg()
        host.addView(bgLabel)
        val nextBg = android.widget.Button(this)
        nextBg.text = "Next background"
        nextBg.setOnClickListener {
            val ids = com.aurafx.sdk.scene.BackgroundCatalog.items.map { it.id }
            val cur = target.backgroundParameters().id
            val idx = ids.indexOf(cur)
            val id = ids[(idx + 1 + ids.size) % ids.size]
            target.background { enabled = true; this.id = id; intensity = 0.75f }
            showBg()
        }
        host.addView(nextBg)
        val clearBg = android.widget.Button(this)
        clearBg.text = "Clear background"
        clearBg.setOnClickListener { target.resetBackground(); showBg() }
        host.addView(clearBg)
        row("BG intensity", read = { target.backgroundParameters().intensity }, write = { v ->
            val id = target.backgroundParameters().id ?: com.aurafx.sdk.scene.BackgroundCatalog.items.first().id
            target.background { enabled = true; this.id = id; intensity = v }
            showBg()
        })
        val hairBtn = android.widget.Button(this)
        hairBtn.text = "Cycle hair color"
        hairBtn.setOnClickListener {
            val colors = com.aurafx.sdk.api.HairColorId.entries
            val cur = target.hairParameters().color
            val next = colors[(colors.indexOf(cur) + 1) % colors.size]
            target.hair { enabled = true; color = next; intensity = 0.6f }
        }
        host.addView(hairBtn)
        row("Hair color", read = { target.hairParameters().intensity }, write = { v ->
            target.hair { enabled = v > 0f; intensity = v }
        })
        row("Body slim", read = { target.bodyParameters().slim }, write = { v ->
            target.body { enabled = v > 0f; slim = v }
        })
        row("Body waist", read = { target.bodyParameters().waist }, write = { v ->
            target.body { enabled = v > 0f; waist = v }
        })
        val lightBtn = android.widget.Button(this)
        lightBtn.text = "Cycle lighting"
        lightBtn.setOnClickListener {
            val modes = com.aurafx.sdk.api.LightingMode.entries
            val cur = target.lightingParameters().mode
            val next = modes[(modes.indexOf(cur) + 1) % modes.size]
            target.lighting { enabled = true; mode = next; intensity = 0.5f }
        }
        host.addView(lightBtn)
        row("Lighting", read = { target.lightingParameters().intensity }, write = { v ->
            target.lighting { enabled = v > 0f; intensity = v }
        })
        val resetMk = android.widget.Button(this)
        resetMk.text = "Classic makeup"
        resetMk.setOnClickListener {
            target.applyMakeupPreset(com.aurafx.sdk.api.MakeupPreset.Classic)
            bindBeautySliders(target)
        }
        host.addView(resetMk)
        val bright = android.widget.Button(this)
        bright.text = "Bright makeup"
        bright.setOnClickListener {
            target.applyMakeupPreset(com.aurafx.sdk.api.MakeupPreset.Bright)
            bindBeautySliders(target)
        }
        host.addView(bright)
        val extra = android.widget.Button(this)
        extra.text = "Extravagant makeup"
        extra.setOnClickListener {
            target.applyMakeupPreset(com.aurafx.sdk.api.MakeupPreset.Extravagant)
            bindBeautySliders(target)
        }
        host.addView(extra)
        row("Foundation", read = { target.makeupParameters().foundation.intensity }, write = { v -> target.makeup { foundation.enabled = v > 0f; foundation.intensity = v } })
        row("Concealer", read = { target.makeupParameters().concealer.intensity }, write = { v -> target.makeup { concealer.enabled = v > 0f; concealer.intensity = v } })
        row("Blush", read = { target.makeupParameters().blush.intensity }, write = { v -> target.makeup { blush.enabled = v > 0f; blush.intensity = v } })
        row("Contour", read = { target.makeupParameters().contour.intensity }, write = { v -> target.makeup { contour.enabled = v > 0f; contour.intensity = v } })
        row("Highlight", read = { target.makeupParameters().highlight.intensity }, write = { v -> target.makeup { highlight.enabled = v > 0f; highlight.intensity = v } })
        row("Eyebrow", read = { target.makeupParameters().eyebrow.intensity }, write = { v -> target.makeup { eyebrow.enabled = v > 0f; eyebrow.intensity = v } })
        row("Eyeshadow", read = { target.makeupParameters().eyeshadow.intensity }, write = { v -> target.makeup { eyeshadow.enabled = v > 0f; eyeshadow.intensity = v } })
        row("Eyeliner", read = { target.makeupParameters().eyeliner.intensity }, write = { v -> target.makeup { eyeliner.enabled = v > 0f; eyeliner.intensity = v } })
        row("Lashes", read = { target.makeupParameters().eyelashes.intensity }, write = { v -> target.makeup { eyelashes.enabled = v > 0f; eyelashes.intensity = v } })
        row("Lipstick", read = { target.makeupParameters().lipstick.intensity }, write = { v -> target.makeup { lipstick.enabled = v > 0f; lipstick.intensity = v } })
        row("Lip Liner", read = { target.makeupParameters().lipLiner.intensity }, write = { v -> target.makeup { lipLiner.enabled = v > 0f; lipLiner.intensity = v } })
        row("Lip Gloss", read = { target.makeupParameters().lipGloss.intensity }, write = { v -> target.makeup { lipGloss.enabled = v > 0f; lipGloss.intensity = v } })
        row("Lens", read = { target.makeupParameters().lens.intensity }, write = { v -> target.makeup { lens.enabled = v > 0f; lens.intensity = v } })
        row("Fine Smooth", read = { target.beautyParameters().fineSmooth }, write = { v -> target.beauty { fineSmooth = v } })
        row("Skin Smoothness", read = { target.skinParameters().smoothness }, write = { v -> target.skin { smoothness = v } })
        row("Skin Texture", read = { target.skinParameters().texturePreserve }, write = { v -> target.skin { texturePreserve = v } })
        row("Blemish", read = { target.skinParameters().blemishReduction }, write = { v -> target.skin { blemishReduction = v } })
        row("Evenness", read = { target.skinParameters().evenness }, write = { v -> target.skin { evenness = v } })
        row("Brightness", read = { target.skinParameters().brightness }, write = { v -> target.skin { brightness = v } })
        row("Whiten", read = { target.beautyParameters().whiten }, write = { v -> target.beauty { whiten = v } })
        row("Ruddy", read = { target.beautyParameters().ruddy }, write = { v -> target.beauty { ruddy = v } })
        row("Skin Tone", read = { target.skinParameters().tone }, write = { v -> target.skin { tone = v } })
        row("Natural Skin", read = { target.skinParameters().naturalSkin }, write = { v -> target.skin { naturalSkin = v } })
        row("Tooth Whiten", read = { target.beautyParameters().toothWhiten }, write = { v -> target.beauty { toothWhiten = v } })
        row("Circles", read = { target.beautyParameters().circles }, write = { v -> target.beauty { circles = v } })
        row("V Face", read = { target.faceShapeParameters().vFace }, write = { v -> target.faceShape { vFace = v } })
        row("Cheek Thin", read = { target.faceShapeParameters().cheekThin }, write = { v -> target.faceShape { cheekThin = v } })
        row("Cheek Small", read = { target.faceShapeParameters().cheekSmall }, write = { v -> target.faceShape { cheekSmall = v } })
        row("Cheek Narrow", read = { target.faceShapeParameters().cheekNarrow }, write = { v -> target.faceShape { cheekNarrow = v } })
        row("Nose", read = { target.faceShapeParameters().nose }, write = { v -> target.faceShape { nose = v } })
        row("Eye Enlarge", read = { target.faceShapeParameters().eyeEnlarge }, write = { v -> target.faceShape { eyeEnlarge = v } })
        row("Eye Distance", signed = true, read = { target.faceShapeParameters().eyeDistance }, write = { v -> target.faceShape { eyeDistance = v } })
        row("Mouth", read = { target.faceShapeParameters().mouth }, write = { v -> target.faceShape { mouth = v } })
        val reset = android.widget.Button(this)
        reset.text = "Reset beauty"
        reset.setOnClickListener {
            target.resetBeauty()
            bindBeautySliders(target)
        }
        host.addView(reset)
    }

    private fun setStatus(text: String, error: Boolean = false) {
        Log.i("AuraFX", "sample: $text")
        binding.statusText.text = text
        binding.statusText.setTextColor(
            ContextCompat.getColor(this, if (error) R.color.danger else R.color.text),
        )
    }
}
