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
import com.aurafx.sdk.api.HairColorId
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.LightingMode
import com.aurafx.sdk.api.MakeupPreset
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
    private var category = Category.Beauty

    private enum class Category { Beauty, FaceShape, Makeup, Filters, Background, Hair, Body, Lighting, AR }

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
        fun slider(name: String, read: () -> Float, write: (Float) -> Unit) {
            label(name)
            val bar = SeekBar(this)
            bar.max = 200
            bar.progress = (read() * 200f).toInt().coerceIn(0, 200)
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) write(progress / 200f)
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
                slider("Fine smooth", { target.beautyParameters().fineSmooth }) { v -> target.beauty { fineSmooth = v } }
                slider("Smoothness", { target.skinParameters().smoothness }) { v -> target.skin { smoothness = v } }
                slider("Texture preserve", { target.skinParameters().texturePreserve }) { v -> target.skin { texturePreserve = v } }
                slider("Whiten", { target.beautyParameters().whiten }) { v -> target.beauty { whiten = v } }
                action("Reset beauty") { target.resetBeauty(); bindCategory(target) }
            }
            Category.FaceShape -> {
                slider("V face", { target.faceShapeParameters().vFace }) { v -> target.faceShape { vFace = v } }
                slider("Cheek thin", { target.faceShapeParameters().cheekThin }) { v -> target.faceShape { cheekThin = v } }
                slider("Eye enlarge", { target.faceShapeParameters().eyeEnlarge }) { v -> target.faceShape { eyeEnlarge = v } }
                slider("Nose", { target.faceShapeParameters().nose }) { v -> target.faceShape { nose = v } }
            }
            Category.Makeup -> {
                action("Classic") { target.applyMakeupPreset(MakeupPreset.Classic) }
                action("Bright") { target.applyMakeupPreset(MakeupPreset.Bright) }
                action("Extravagant") { target.applyMakeupPreset(MakeupPreset.Extravagant) }
                slider("Lipstick", { target.makeupParameters().lipstick.intensity }) { v ->
                    target.makeup { lipstick.enabled = v > 0f; lipstick.intensity = v }
                }
                slider("Blush", { target.makeupParameters().blush.intensity }) { v ->
                    target.makeup { blush.enabled = v > 0f; blush.intensity = v }
                }
                action("Reset makeup") { target.resetMakeup() }
            }
            Category.Filters -> {
                action("Next filter") {
                    val ids = target.filterCatalog().map { it.id }
                    val idx = ids.indexOf(target.filterParameters().id)
                    target.setFilter(ids[(idx + 1 + ids.size) % ids.size], 0.7f)
                    bindCategory(target)
                }
                label("Current ${target.filterParameters().id ?: "none"}")
                slider("Intensity", { target.filterParameters().intensity }) { v ->
                    val id = target.filterParameters().id ?: target.filterCatalog().first().id
                    target.setFilter(id, v)
                }
                action("Clear filter") { target.clearFilter(); bindCategory(target) }
            }
            Category.Background -> {
                action("Next background") {
                    val ids = target.backgroundCatalog().map { it.id }
                    val idx = ids.indexOf(target.backgroundParameters().id)
                    target.background { enabled = true; id = ids[(idx + 1 + ids.size) % ids.size]; intensity = 0.75f }
                    bindCategory(target)
                }
                label("Current ${target.backgroundParameters().id ?: "none"}")
                slider("Intensity", { target.backgroundParameters().intensity }) { v ->
                    val id = target.backgroundParameters().id ?: target.backgroundCatalog().first().id
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
                slider("Slim", { target.bodyParameters().slim }) { v -> target.body { enabled = v > 0f; slim = v } }
                slider("Waist", { target.bodyParameters().waist }) { v -> target.body { enabled = v > 0f; waist = v } }
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
                label("Mode ${target.lightingParameters().mode}")
                slider("Intensity", { target.lightingParameters().intensity }) { v ->
                    target.lighting { enabled = v > 0f; intensity = v }
                }
                action("Reset lighting") { target.resetLighting(); bindCategory(target) }
            }
            Category.AR -> {
                action("Next AR") {
                    val ids = target.arCatalog().map { it.id }
                    val idx = ids.indexOf(target.arParameters().effectId)
                    target.setAREffect(ids[(idx + 1 + ids.size) % ids.size], 0.85f)
                    bindCategory(target)
                }
                label("AR ${target.arParameters().effectId ?: "none"}")
                slider("Intensity", { target.arParameters().intensity }) { v ->
                    val id = target.arParameters().effectId ?: target.arCatalog().first().id
                    target.setAREffect(id, v)
                }
                action("Clear AR") { target.clearAREffect(); bindCategory(target) }
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
