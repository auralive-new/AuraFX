package com.aurafx.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
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
                is AuraFxResult.Ok -> session = created.value
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

    private fun setStatus(text: String, error: Boolean = false) {
        binding.statusText.text = text
        binding.statusText.setTextColor(
            ContextCompat.getColor(this, if (error) R.color.danger else R.color.text),
        )
    }
}
