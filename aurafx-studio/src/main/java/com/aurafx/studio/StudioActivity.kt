package com.aurafx.studio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aurafx.sdk.AuraFx
import com.aurafx.sdk.AuraFxSession
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.AuraFxSessionListener
import com.aurafx.sdk.api.BackgroundTray
import com.aurafx.sdk.api.BlushStyle
import com.aurafx.sdk.api.BrowStyle
import com.aurafx.sdk.api.EyelinerStyle
import com.aurafx.sdk.api.EyeshadowStyle
import com.aurafx.sdk.api.HairColorId
import com.aurafx.sdk.api.LashStyle
import com.aurafx.sdk.api.LensStyle
import com.aurafx.sdk.api.LipLook
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.LightingMode
import com.aurafx.sdk.api.MakeupPreset
import com.aurafx.sdk.api.MaskTray
import com.aurafx.sdk.api.label
import com.aurafx.sdk.api.trayLabel
import com.aurafx.sdk.api.SessionConfig
import com.aurafx.studio.databinding.ActivityStudioBinding
import java.io.File

class StudioActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private lateinit var binding: ActivityStudioBinding
    private val mainHandler = Handler(Looper.getMainLooper())
    private var session: AuraFxSession? = null
    private var surfaceReady = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var pendingFacing = LensFacing.FRONT
    private var userWantsCamera = false
    private var beautyAdvanced = false

    private enum class Category { Beauty, FaceShape, Makeup, Filters, Background, Hair, Body, Lighting, Masks }

    private var category = Category.Beauty
    private var maskTray = com.aurafx.sdk.api.MaskTray.New
    private var bgTray = com.aurafx.sdk.api.BackgroundTray.Orbit360

    private val metricsTicker = object : Runnable {
        override fun run() {
            val snap = session?.performanceSnapshot()
            if (snap != null) {
                binding.metricsText.text = buildString {
                    append("fps=").append("%.1f".format(snap.fps))
                    append(" procMs=").append("%.2f".format(snap.frameProcessTimeMs))
                    append(" gpuMs=").append(snap.gpuTimeMs?.let { "%.2f".format(it) } ?: "n/a")
                    append(" drop=").append(snap.droppedFrames)
                    append("\nencMs=").append(snap.videoEncodeTimeMs?.let { "%.2f".format(it) } ?: "n/a")
                    append(" encFrames=").append(snap.encoderFrames)
                    append(" photoMs=").append(snap.lastPhotoCaptureMs?.let { "%.1f".format(it) } ?: "n/a")
                    append(" rec=").append(session?.isRecording())
                }
            }
            mainHandler.postDelayed(this, 400)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[Manifest.permission.CAMERA] == true) {
            ensureSessionAndMaybeStart()
        } else {
            setStatus("CAMERA permission denied", error = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.previewSurface.holder.addCallback(this)
        when (val result = AuraFx.initialize(this)) {
            is AuraFxResult.Ok -> setStatus("SDK initialized")
            is AuraFxResult.Err -> setStatus(result.error.message, error = true)
        }
        Category.entries.forEach { cat ->
            val b = Button(this)
            b.text = cat.name
            b.setOnClickListener {
                category = cat
                session?.let { bindCategory(it) }
            }
            binding.categoryHost.addView(b)
        }
        binding.startButton.setOnClickListener {
            userWantsCamera = true
            requestPermissionAndStart()
        }
        binding.stopButton.setOnClickListener {
            userWantsCamera = false
            session?.stopCamera()
        }
        binding.frontButton.setOnClickListener { startFacing(LensFacing.FRONT) }
        binding.backButton.setOnClickListener { startFacing(LensFacing.BACK) }
        binding.switchButton.setOnClickListener {
            when (val r = session?.switchCamera()) {
                is AuraFxResult.Err -> setStatus(r.error.message, error = true)
                else -> setStatus("Switch requested")
            }
        }
        binding.photoButton.setOnClickListener { takePhoto() }
        binding.recordButton.setOnClickListener { toggleRecord() }
        binding.resetButton.setOnClickListener {
            session?.resetAll()
            session?.let { bindCategory(it) }
            setStatus("All effects reset")
        }
        binding.editorButton.setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java))
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
        if (userWantsCamera) ensureSessionAndMaybeStart()
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

    private fun startFacing(facing: LensFacing) {
        pendingFacing = facing
        userWantsCamera = true
        requestPermissionAndStart()
    }

    private fun requestPermissionAndStart() {
        val cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!cam || !mic) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            return
        }
        ensureSessionAndMaybeStart()
    }

    private fun ensureSessionAndMaybeStart() {
        if (!userWantsCamera || !AuraFx.isInitialized()) return
        if (session == null) {
            when (val created = AuraFx.createSession(this, SessionConfig(listener = listener()))) {
                is AuraFxResult.Ok -> {
                    session = created.value
                    bindCategory(created.value)
                }
                is AuraFxResult.Err -> {
                    setStatus(created.error.message, error = true)
                    return
                }
            }
        }
        val current = session ?: return
        if (!surfaceReady) return
        current.attachPreview(binding.previewSurface.holder.surface, surfaceWidth, surfaceHeight)
        when (val started = current.startCamera(this, pendingFacing)) {
            is AuraFxResult.Ok -> setStatus("Camera $pendingFacing")
            is AuraFxResult.Err -> setStatus(started.error.message, error = true)
        }
    }

    private fun listener() = object : AuraFxSessionListener {
        override fun onCameraStarted(facing: LensFacing) = setStatus("Camera started $facing")
        override fun onFirstFrame(timestampNs: Long) = setStatus("First processed frame")
        override fun onError(error: AuraFxError) {
            setStatus(error.message, error = true)
            Toast.makeText(this@StudioActivity, error.message, Toast.LENGTH_SHORT).show()
        }
        override fun onCameraStopped() = setStatus("Camera stopped")
    }

    private fun takePhoto() {
        val s = session ?: return
        val file = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        s.capturePhoto(file) { result ->
            when (result) {
                is AuraFxResult.Ok -> setStatus("Photo ${result.value.width}x${result.value.height} processed=${result.value.processed} ${file.name}")
                is AuraFxResult.Err -> setStatus(result.error.message, error = true)
            }
        }
    }

    private fun toggleRecord() {
        val s = session ?: return
        if (s.isRecording()) {
            s.stopRecording { result ->
                when (result) {
                    is AuraFxResult.Ok -> setStatus("Video ${result.value.frameCount}f audio=${result.value.hasAudio} ${result.value.file.name}")
                    is AuraFxResult.Err -> setStatus(result.error.message, error = true)
                }
                binding.recordButton.text = "Record"
            }
        } else {
            val file = File(cacheDir, "clip_${System.currentTimeMillis()}.mp4")
            val audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            when (val r = s.startRecording(file, audio)) {
                is AuraFxResult.Ok -> {
                    binding.recordButton.text = "Stop rec"
                    setStatus("Recording processed frames")
                }
                is AuraFxResult.Err -> setStatus(r.error.message, error = true)
            }
        }
    }

    private fun bindCategory(target: AuraFxSession) {
        val host = binding.controlHost
        host.removeAllViews()
        fun label(text: String) {
            val t = TextView(this)
            t.setTextColor(ContextCompat.getColor(this, R.color.text))
            t.text = text
            host.addView(t)
        }
        fun slider(name: String, read: () -> Float, signed: Boolean = false, write: (Float) -> Unit) {
            label(name)
            val bar = SeekBar(this)
            bar.max = 200
            val initial = read()
            bar.progress = if (signed) ((initial + 1f) * 100f).toInt().coerceIn(0, 200) else (initial * 200f).toInt().coerceIn(0, 200)
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) write(if (signed) progress / 100f - 1f else progress / 200f)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            host.addView(bar)
        }
        fun action(title: String, block: () -> Unit) {
            val b = Button(this)
            b.text = title
            b.setOnClickListener { block() }
            host.addView(b)
        }
        when (category) {
            Category.Beauty -> {
                action(if (beautyAdvanced) "Show basic" else "Show advanced") {
                    beautyAdvanced = !beautyAdvanced
                    bindCategory(target)
                }
                if (!beautyAdvanced) {
                    slider("Fine Smooth", { target.beautyParameters().fineSmooth }) { v -> target.beauty { fineSmooth = v } }
                    slider("Tooth Whiten", { target.beautyParameters().toothWhiten }) { v -> target.beauty { toothWhiten = v } }
                    slider("Whiten", { target.beautyParameters().whiten }) { v -> target.beauty { whiten = v } }
                    slider("Ruddy", { target.beautyParameters().ruddy }) { v -> target.beauty { ruddy = v } }
                    slider("Circles", { target.beautyParameters().circles }) { v -> target.beauty { circles = v } }
                } else {
                    slider("V Face", { target.faceShapeParameters().vFace }) { v -> target.faceShape { vFace = v } }
                    slider("Cheek Thin", { target.faceShapeParameters().cheekThin }) { v -> target.faceShape { cheekThin = v } }
                    slider("Cheek Small", { target.faceShapeParameters().cheekSmall }) { v -> target.faceShape { cheekSmall = v } }
                    slider("Cheek Narrow", { target.faceShapeParameters().cheekNarrow }) { v -> target.faceShape { cheekNarrow = v } }
                    slider("Nose", { target.faceShapeParameters().nose }) { v -> target.faceShape { nose = v } }
                    slider("Eye Enlarge", { target.faceShapeParameters().eyeEnlarge }) { v -> target.faceShape { eyeEnlarge = v } }
                    slider("Eye Distance", { target.faceShapeParameters().eyeDistance }, signed = true) { v -> target.faceShape { eyeDistance = v } }
                    slider("Mouth", { target.faceShapeParameters().mouth }) { v -> target.faceShape { mouth = v } }
                }
                action("Reset beauty") { target.resetBeauty(); bindCategory(target) }
            }
            Category.FaceShape -> {
                slider("V Face", { target.faceShapeParameters().vFace }) { v -> target.faceShape { vFace = v } }
                slider("Cheek Thin", { target.faceShapeParameters().cheekThin }) { v -> target.faceShape { cheekThin = v } }
                slider("Cheek Small", { target.faceShapeParameters().cheekSmall }) { v -> target.faceShape { cheekSmall = v } }
                slider("Cheek Narrow", { target.faceShapeParameters().cheekNarrow }) { v -> target.faceShape { cheekNarrow = v } }
                slider("Nose", { target.faceShapeParameters().nose }) { v -> target.faceShape { nose = v } }
                slider("Eye Enlarge", { target.faceShapeParameters().eyeEnlarge }) { v -> target.faceShape { eyeEnlarge = v } }
                slider("Eye Distance", { target.faceShapeParameters().eyeDistance }, signed = true) { v -> target.faceShape { eyeDistance = v } }
                slider("Mouth", { target.faceShapeParameters().mouth }) { v -> target.faceShape { mouth = v } }
            }
            Category.Makeup -> {
                action("Classic") { target.applyMakeupPreset(MakeupPreset.Classic); bindCategory(target) }
                action("Bright") { target.applyMakeupPreset(MakeupPreset.Bright); bindCategory(target) }
                action("Extravagant") { target.applyMakeupPreset(MakeupPreset.Extravagant); bindCategory(target) }
                action("Next blush") {
                    val styles = BlushStyle.entries
                    val cur = target.makeupParameters().blush.style
                    val next = styles[(styles.indexOf(cur) + 1) % styles.size]
                    target.makeup { blush.enabled = true; blush.style = next; blush.intensity = blush.intensity.coerceAtLeast(0.35f) }
                    bindCategory(target)
                }
                action("Next lip look") {
                    val looks = LipLook.entries
                    val cur = target.makeupParameters().lipstick.look
                    val next = looks[(looks.indexOf(cur) + 1) % looks.size]
                    target.makeup { lipstick.enabled = true; lipstick.look = next; lipstick.intensity = lipstick.intensity.coerceAtLeast(0.45f) }
                    bindCategory(target)
                }
                action("Next brow") {
                    val styles = BrowStyle.entries
                    val cur = target.makeupParameters().eyebrow.style
                    val next = styles[(styles.indexOf(cur) + 1) % styles.size]
                    target.makeup { eyebrow.enabled = true; eyebrow.style = next; eyebrow.intensity = eyebrow.intensity.coerceAtLeast(0.4f) }
                    bindCategory(target)
                }
                action("Next liner") {
                    val styles = EyelinerStyle.entries
                    val cur = target.makeupParameters().eyeliner.style
                    val next = styles[(styles.indexOf(cur) + 1) % styles.size]
                    target.makeup { eyeliner.enabled = true; eyeliner.style = next; eyeliner.intensity = if (next == EyelinerStyle.None) 0f else eyeliner.intensity.coerceAtLeast(0.45f) }
                    bindCategory(target)
                }
                action("Next lash") {
                    val styles = LashStyle.entries
                    val cur = target.makeupParameters().eyelashes.style
                    val next = styles[(styles.indexOf(cur) + 1) % styles.size]
                    target.makeup { eyelashes.enabled = true; eyelashes.style = next; eyelashes.intensity = eyelashes.intensity.coerceAtLeast(0.4f) }
                    bindCategory(target)
                }
                action("Next shadow") {
                    val styles = EyeshadowStyle.entries
                    val cur = target.makeupParameters().eyeshadow.style
                    val next = styles[(styles.indexOf(cur) + 1) % styles.size]
                    target.makeup { eyeshadow.enabled = true; eyeshadow.style = next; eyeshadow.intensity = eyeshadow.intensity.coerceAtLeast(0.4f) }
                    bindCategory(target)
                }
                action("Next lens") {
                    val styles = LensStyle.entries
                    val cur = target.makeupParameters().lens.style
                    val next = styles[(styles.indexOf(cur) + 1) % styles.size]
                    target.makeup { lens.enabled = true; lens.style = next; lens.intensity = lens.intensity.coerceAtLeast(0.35f) }
                    bindCategory(target)
                }
                val m = target.makeupParameters()
                label("Blush ${m.blush.style}  Lip ${m.lipstick.look}")
                label("Brow ${m.eyebrow.style}  Liner ${m.eyeliner.style}")
                label("Shadow ${m.eyeshadow.style}  Lash ${m.eyelashes.style}  Lens ${m.lens.style}")
                slider("Lipstick", { target.makeupParameters().lipstick.intensity }) { v ->
                    target.makeup { lipstick.enabled = v > 0f; lipstick.intensity = v }
                }
                slider("Blush", { target.makeupParameters().blush.intensity }) { v ->
                    target.makeup { blush.enabled = v > 0f; blush.intensity = v }
                }
                action("Reset makeup") { target.resetMakeup(); bindCategory(target) }
            }
            Category.Filters -> {
                action("Next filter") {
                    val ids = target.filterCatalog().map { it.id }
                    val idx = ids.indexOf(target.filterParameters().id)
                    target.setFilter(ids[(idx + 1 + ids.size) % ids.size], 0.7f)
                    bindCategory(target)
                }
                label("Current ${target.filterParameters().id ?: "none"}  ${target.filterCatalog().firstOrNull { it.id == target.filterParameters().id }?.displayName ?: ""}")
                slider("Intensity", { target.filterParameters().intensity }) { v ->
                    val id = target.filterParameters().id ?: target.filterCatalog().first().id
                    target.setFilter(id, v)
                }
                action("Clear filter") { target.clearFilter(); bindCategory(target) }
            }
            Category.Background -> {
                action("Next tray") {
                    val trays = BackgroundTray.entries
                    bgTray = trays[(trays.indexOf(bgTray) + 1) % trays.size]
                    bindCategory(target)
                }
                val inTray = target.backgroundCatalog().filter { it.tray == bgTray }.ifEmpty { target.backgroundCatalog() }
                action("Next in ${bgTray.trayLabel()}") {
                    val ids = inTray.map { it.id }
                    val idx = ids.indexOf(target.backgroundParameters().id)
                    target.background { enabled = true; id = ids[(idx + 1 + ids.size) % ids.size]; intensity = 0.75f }
                    bindCategory(target)
                }
                label("Tray ${bgTray.trayLabel()}  ${target.backgroundCatalog().firstOrNull { it.id == target.backgroundParameters().id }?.displayName ?: "none"}")
                slider("Intensity", { target.backgroundParameters().intensity }) { v ->
                    val id = target.backgroundParameters().id ?: inTray.first().id
                    target.background { enabled = true; this.id = id; intensity = v }
                }
                action("Reset background") { target.resetBackground(); bindCategory(target) }
            }
            Category.Hair -> {
                action("Next style") {
                    val ids = target.hairCatalog().map { it.id }
                    val idx = ids.indexOf(target.hairParameters().styleId)
                    target.setHairStyle(ids[(idx + 1) % ids.size])
                    target.hair { enabled = true; intensity = target.hairParameters().intensity.coerceAtLeast(0.55f) }
                    bindCategory(target)
                }
                action("Next color") {
                    val colors = HairColorId.entries
                    val next = colors[(colors.indexOf(target.hairParameters().color) + 1) % colors.size]
                    target.hair { enabled = true; color = next; intensity = 0.6f }
                    bindCategory(target)
                }
                label("Style ${target.hairParameters().styleId}  color ${target.hairParameters().color}")
                slider("Intensity", { target.hairParameters().intensity }) { v ->
                    target.hair { enabled = true; intensity = v }
                }
                action("Reset hair") { target.resetHair(); bindCategory(target) }
            }
            Category.Body -> {
                slider("Hips", { target.bodyParameters().hips }) { v -> target.body { enabled = v > 0f; hips = v } }
                slider("Waist", { target.bodyParameters().waist }) { v -> target.body { enabled = v > 0f; waist = v } }
                slider("Slim", { target.bodyParameters().slim }) { v -> target.body { enabled = v > 0f; slim = v } }
                slider("Shoulders", { target.bodyParameters().shoulders }) { v -> target.body { enabled = v > 0f; shoulders = v } }
                action("Reset body") { target.resetBody(); bindCategory(target) }
            }
            Category.Lighting -> {
                action("Next mode") {
                    val modes = LightingMode.entries
                    val next = modes[(modes.indexOf(target.lightingParameters().mode) + 1) % modes.size]
                    target.lighting { enabled = true; mode = next; intensity = 0.5f }
                    bindCategory(target)
                }
                label("Mode ${target.lightingParameters().mode.label()}")
                slider("Intensity", { target.lightingParameters().intensity }) { v ->
                    target.lighting { enabled = v > 0f; intensity = v }
                }
                action("Reset lighting") { target.resetLighting(); bindCategory(target) }
            }
            Category.Masks -> {
                action("Next tray") {
                    val trays = MaskTray.entries
                    maskTray = trays[(trays.indexOf(maskTray) + 1) % trays.size]
                    bindCategory(target)
                }
                val inTray = target.arCatalog().filter { it.tray == maskTray }.ifEmpty { target.arCatalog() }
                action("Next in ${maskTray.trayLabel()}") {
                    val ids = inTray.map { it.id }
                    val idx = ids.indexOf(target.arParameters().effectId)
                    target.setAREffect(ids[(idx + 1 + ids.size) % ids.size], 0.85f)
                    bindCategory(target)
                }
                label("${maskTray.trayLabel()}  ${target.arCatalog().firstOrNull { it.id == target.arParameters().effectId }?.displayName ?: "none"}")
                slider("Intensity", { target.arParameters().intensity }) { v ->
                    val id = target.arParameters().effectId ?: inTray.first().id
                    target.setAREffect(id, v)
                }
                action("Clear mask") { target.clearAREffect(); bindCategory(target) }
            }
        }
    }

    private fun setStatus(text: String, error: Boolean = false) {
        Log.i("AuraFX", "studio: $text")
        binding.statusText.text = text
        binding.statusText.setTextColor(
            ContextCompat.getColor(this, if (error) R.color.danger else R.color.text),
        )
    }
}
