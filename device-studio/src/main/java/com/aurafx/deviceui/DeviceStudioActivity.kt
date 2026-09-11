package com.aurafx.deviceui

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.aurafx.sdk.AuraFx
import com.aurafx.sdk.AuraFxSession
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.AuraFxSessionListener
import com.aurafx.sdk.api.BackgroundTray
import com.aurafx.sdk.api.HairColorId
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.LensStyle
import com.aurafx.sdk.api.MakeupPreset
import com.aurafx.sdk.api.MaskTray
import com.aurafx.sdk.api.SessionConfig
import com.aurafx.sdk.api.trayLabel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import java.io.File

/**
 * Physical-device Beauty/Effects Studio. Lives in both aurafx-sample and aurafx-studio.
 * Wires live camera frames into the existing AuraFX GPU pipeline — no mock effects.
 */
class DeviceStudioActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private enum class StudioCategory(val label: String) {
        Beauty("Beauty / Skin"),
        Face("Face"),
        Eyes("Eyes"),
        ContactLens("Contact Lens"),
        Nose("Nose"),
        Mouth("Mouth"),
        Teeth("Teeth"),
        Body("Body"),
        Makeup("Makeup"),
        Filters("Filters"),
        MagicAr("Magic / AR"),
        Gifts("Gifts"),
        Hair("Hair"),
        FullLook("Full Look"),
        Outfit("Outfit"),
        Background("Background"),
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var textureView: TextureView
    private lateinit var fpsChip: TextView
    private lateinit var statusChip: TextView
    private lateinit var qaPanel: TextView
    private lateinit var categoryRow: LinearLayout
    private lateinit var controlsHost: LinearLayout
    private lateinit var sheet: LinearLayout
    private lateinit var beforeButton: MaterialButton
    private lateinit var recordButton: MaterialButton
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>

    private var session: AuraFxSession? = null
    private var previewWindow: Surface? = null
    private var surfaceReady = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var pendingFacing = LensFacing.FRONT
    private var category = StudioCategory.Beauty
    private var qaVisible = false
    private var recording = false
    private var maskTray = MaskTray.New
    private var bgTray = BackgroundTray.Orbit360
    private var selectedGiftId: String? = null
    private var navInsetPx = 0
    private var statusInsetPx = 0

    private val developerMode: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            setStatus("Camera permission granted")
            tryStartCamera()
        } else {
            setStatus("CAMERA permission is required for live preview", error = true)
        }
    }

    private val metricsTicker = object : Runnable {
        override fun run() {
            val snap = session?.performanceSnapshot()
            if (snap != null) {
                fpsChip.visibility = View.VISIBLE
                fpsChip.text = "%.0f FPS".format(snap.fps)
                if (qaVisible) {
                    val track = session?.vision?.latest()
                    qaPanel.text = buildString {
                        appendLine("FPS ${"%.1f".format(snap.fps)}   frame ${"%.2f".format(snap.frameProcessTimeMs)} ms")
                        appendLine("dropped ${snap.droppedFrames}   presented ${snap.presentedFrames}")
                        appendLine("camera ${snap.cameraWidth}x${snap.cameraHeight}   ${snap.cameraFacing}")
                        appendLine("GPU ${snap.gpuRenderer ?: "n/a"}")
                        appendLine("java heap ${snap.javaHeapUsedBytes}   native ${snap.nativeHeapAllocatedBytes ?: "n/a"}")
                        appendLine("tracking ${snap.trackingStatus} faces=${snap.trackedFaces}  ${track?.visionProvider ?: ""}")
                        appendLine("effect load ${snap.lastEffectLoadMs?.let { "%.1f ms".format(it) } ?: "n/a"}")
                        appendLine("camera bound=${snap.cameraBound}   sdk=${snap.sdkState}")
                        appendLine("cam start ${snap.cameraStartupMs?.let { "%.0f ms".format(it) } ?: "n/a"}")
                    }
                }
            }
            mainHandler.postDelayed(this, 400)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val root = CoordinatorLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
            setBackgroundColor(COL_BG)
        }

        textureView = TextureView(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(MATCH, MATCH)
            surfaceTextureListener = this@DeviceStudioActivity
            isOpaque = false
            elevation = 0f
        }
        root.addView(textureView)

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        topBar.addView(iconButton("Close") { finish() })
        topBar.addView(space())
        fpsChip = TextView(this).apply {
            text = "—"
            setTextColor(COL_TEXT)
            setBackgroundColor(COL_CHIP)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            textSize = 12f
            visibility = View.VISIBLE
            setOnLongClickListener {
                qaVisible = !qaVisible
                qaPanel.visibility = if (qaVisible) View.VISIBLE else View.GONE
                true
            }
        }
        topBar.addView(fpsChip)
        topBar.addView(iconButton("Cam") { switchCamera() })

        statusChip = TextView(this).apply {
            setTextColor(COL_MUTED)
            textSize = 12f
            setPadding(dp(12), 0, dp(12), dp(4))
            text = "Starting…"
        }
        qaPanel = TextView(this).apply {
            setTextColor(COL_MUTED)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setBackgroundColor(COL_PANEL)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            visibility = View.GONE
        }
        val topStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = CoordinatorLayout.LayoutParams(MATCH, WRAP)
            elevation = dp(12).toFloat()
            addView(topBar)
            addView(statusChip)
            addView(qaPanel)
        }
        root.addView(topStack)

        sheet = buildSheet()
        val sheetLp = CoordinatorLayout.LayoutParams(MATCH, MATCH).apply {
            behavior = BottomSheetBehavior<LinearLayout>().also { sheetBehavior = it }
            gravity = Gravity.BOTTOM
        }
        sheet.layoutParams = sheetLp
        sheet.elevation = dp(24).toFloat()
        sheet.translationZ = dp(24).toFloat()
        sheet.isClickable = true
        sheet.isFocusable = true
        root.addView(sheet)
        sheetBehavior.isHideable = false
        sheetBehavior.skipCollapsed = false
        sheetBehavior.isFitToContents = false
        sheetBehavior.halfExpandedRatio = 0.42f
        sheetBehavior.isDraggable = true
        sheetBehavior.peekHeight = dp(168)
        sheetBehavior.expandedOffset = dp(72)
        sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            statusInsetPx = bars.top
            navInsetPx = bars.bottom
            topStack.setPadding(bars.left, bars.top, bars.right, 0)
            sheet.setPadding(bars.left, dp(8), bars.right, bars.bottom + dp(12))
            sheetBehavior.expandedOffset = bars.top + dp(8)
            sheetBehavior.peekHeight = dp(168) + bars.bottom
            insets
        }
        ViewCompat.requestApplyInsets(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    finish()
                }
            }
        })

        when (val result = AuraFx.initialize(this)) {
            is AuraFxResult.Ok -> setStatus("AuraFX Studio ready")
            is AuraFxResult.Err -> setStatus(result.error.message, error = true)
        }
        rebuildCategories()
        requestCameraThenStart()
        mainHandler.post(metricsTicker)
    }

    private fun buildSheet(): LinearLayout {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COL_PANEL)
            elevation = dp(24).toFloat()
            isClickable = true
        }
        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
            setBackgroundColor(COL_MUTED)
        }
        column.addView(handle)
        val hint = TextView(this).apply {
            text = "Swipe up for Beauty Studio"
            setTextColor(COL_MUTED)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        column.addView(hint)

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        beforeButton = MaterialButton(this).apply {
            text = "Before"
            minimumHeight = dp(48)
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        session?.setShowUnprocessedPreview(true)
                        setStatus("Before — raw camera")
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        session?.setShowUnprocessedPreview(false)
                        setStatus("Processed preview")
                    }
                }
                true
            }
        }
        recordButton = MaterialButton(this).apply {
            text = "Record"
            minimumHeight = dp(48)
            setOnClickListener { toggleRecord() }
        }
        dock.addView(beforeButton)
        dock.addView(recordButton)
        dock.addView(MaterialButton(this).apply {
            text = "Reset"
            minimumHeight = dp(48)
            setOnClickListener {
                session?.resetAll()
                session?.let { bindCategory(it) }
                setStatus("All effects reset")
            }
        })
        column.addView(dock)

        val catScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        categoryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        catScroll.addView(categoryRow)
        column.addView(catScroll)

        val scroll = NestedScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
            isFillViewport = true
        }
        controlsHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }
        scroll.addView(controlsHost)
        column.addView(scroll)
        return column
    }

    private fun rebuildCategories() {
        categoryRow.removeAllViews()
        StudioCategory.entries.forEach { cat ->
            val chip = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = cat.label
                isAllCaps = false
                minimumHeight = dp(48)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnClickListener {
                    category = cat
                    sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    session?.let { bindCategory(it) } ?: bindCategoryPlaceholder()
                    rebuildCategories()
                }
            }
            if (cat == category) chip.setBackgroundColor(COL_ACCENT_DIM)
            categoryRow.addView(chip)
        }
        session?.let { bindCategory(it) } ?: bindCategoryPlaceholder()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        previewWindow?.release()
        previewWindow = Surface(surface)
        surfaceReady = true
        surfaceWidth = width
        surfaceHeight = height
        tryStartCamera()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        if (width > 0 && height > 0) {
            session?.resizePreview(width, height)
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        surfaceReady = false
        session?.detachPreview()
        previewWindow?.release()
        previewWindow = null
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    override fun onDestroy() {
        mainHandler.removeCallbacks(metricsTicker)
        session?.setShowUnprocessedPreview(false)
        session?.release()
        session = null
        previewWindow?.release()
        previewWindow = null
        super.onDestroy()
    }

    private fun requestCameraThenStart() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        tryStartCamera()
    }

    private fun tryStartCamera() {
        if (!AuraFx.isInitialized()) return
        if (!surfaceReady) return
        val window = previewWindow ?: return
        val w = if (surfaceWidth > 0) surfaceWidth else textureView.width
        val h = if (surfaceHeight > 0) surfaceHeight else textureView.height
        if (w <= 0 || h <= 0) {
            textureView.post { tryStartCamera() }
            return
        }
        if (session == null) {
            val config = SessionConfig(
                preferredPreviewWidth = 720,
                preferredPreviewHeight = 1280,
                targetMinFps = 30,
                targetMaxFps = 60,
                listener = listener(),
            )
            when (val created = AuraFx.createSession(this, config)) {
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
        if (current.isCameraBound()) {
            current.resizePreview(w, h)
            return
        }
        when (val attached = current.attachPreview(window, w, h)) {
            is AuraFxResult.Err -> {
                setStatus(attached.error.message, error = true)
                return
            }
            is AuraFxResult.Ok -> Unit
        }
        when (val started = current.startCamera(this, pendingFacing)) {
            is AuraFxResult.Ok -> setStatus("Starting ${pendingFacing.name.lowercase()} camera")
            is AuraFxResult.Err -> setStatus(started.error.message, error = true)
        }
    }

    private fun switchCamera() {
        val current = session
        if (current == null || !current.isCameraBound()) {
            pendingFacing = if (pendingFacing == LensFacing.FRONT) LensFacing.BACK else LensFacing.FRONT
            tryStartCamera()
            return
        }
        when (val result = current.switchCamera()) {
            is AuraFxResult.Ok -> setStatus("Switching camera")
            is AuraFxResult.Err -> setStatus(result.error.message, error = true)
        }
    }

    private fun listener() = object : AuraFxSessionListener {
        override fun onCameraStarted(facing: LensFacing) {
            pendingFacing = facing
            setStatus("Live ${facing.name.lowercase()} camera")
        }

        override fun onFirstFrame(timestampNs: Long) {
            setStatus("AuraFX pipeline receiving frames")
        }

        override fun onError(error: AuraFxError) {
            setStatus(error.message, error = true)
            Toast.makeText(this@DeviceStudioActivity, error.message, Toast.LENGTH_SHORT).show()
        }

        override fun onCameraStopped() = setStatus("Camera stopped")
    }

    private fun toggleRecord() {
        val s = session ?: return
        if (s.isRecording()) {
            s.stopRecording { result ->
                recording = false
                recordButton.text = "Record"
                when (result) {
                    is AuraFxResult.Ok -> setStatus("Recorded ${result.value.frameCount} processed frames")
                    is AuraFxResult.Err -> setStatus(result.error.message, error = true)
                }
            }
            return
        }
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val file = File(cacheDir, "clip_${System.currentTimeMillis()}.mp4")
        when (val r = s.startRecording(file, mic)) {
            is AuraFxResult.Ok -> {
                recording = true
                recordButton.text = "Stop"
                setStatus("Recording processed output")
            }
            is AuraFxResult.Err -> setStatus(r.error.message, error = true)
        }
    }

    private fun bindCategoryPlaceholder() {
        controlsHost.removeAllViews()
        note("Waiting for camera session…")
    }

    private fun bindCategory(target: AuraFxSession) {
        controlsHost.removeAllViews()
        heading(category.label)
        when (category) {
            StudioCategory.Beauty -> {
                slider("Fine smooth", { target.beautyParameters().fineSmooth }) { v ->
                    target.beauty { fineSmooth = v }
                    target.skin { fineSmooth = v }
                }
                slider("Smoothness", { target.skinParameters().smoothness }) { v -> target.skin { smoothness = v } }
                slider("Blemish", { target.skinParameters().blemishReduction }) { v -> target.skin { blemishReduction = v } }
                slider("Evenness", { target.skinParameters().evenness }) { v -> target.skin { evenness = v } }
                slider("Brightness", { target.skinParameters().brightness }) { v -> target.skin { brightness = v } }
                slider("Whiten", { target.beautyParameters().whiten }) { v ->
                    target.beauty { whiten = v }
                    target.skin { whiten = v }
                }
                slider("Ruddy", { target.beautyParameters().ruddy }) { v ->
                    target.beauty { ruddy = v }
                    target.skin { ruddy = v }
                }
                slider("Dark circles", { target.beautyParameters().circles }) { v -> target.beauty { circles = v } }
                action("Reset beauty") { target.resetBeauty(); bindCategory(target) }
            }
            StudioCategory.Face -> {
                slider("V-face", { target.faceShapeParameters().vFace }) { v -> target.faceShape { vFace = v } }
                slider("Cheek thin", { target.faceShapeParameters().cheekThin }) { v -> target.faceShape { cheekThin = v } }
                slider("Cheek small", { target.faceShapeParameters().cheekSmall }) { v -> target.faceShape { cheekSmall = v } }
                slider("Cheek narrow", { target.faceShapeParameters().cheekNarrow }) { v -> target.faceShape { cheekNarrow = v } }
                action("Reset face") { target.resetBeauty(); bindCategory(target) }
            }
            StudioCategory.Eyes -> {
                slider("Enlarge", { target.faceShapeParameters().eyeEnlarge }) { v -> target.faceShape { eyeEnlarge = v } }
                signedSlider("Distance", { target.faceShapeParameters().eyeDistance }) { v ->
                    target.faceShape { eyeDistance = v }
                }
            }
            StudioCategory.ContactLens -> {
                note("Real makeup lens engine — not a sticker overlay.")
                LensStyle.entries.forEach { style ->
                    action(style.name) {
                        target.makeup {
                            lens.enabled = true
                            lens.style = style
                            lens.intensity = lens.intensity.coerceAtLeast(0.45f)
                        }
                        bindCategory(target)
                    }
                }
                slider("Intensity", { target.makeupParameters().lens.intensity }) { v ->
                    target.makeup { lens.enabled = v > 0f; lens.intensity = v }
                }
                action("Clear lenses") {
                    target.makeup { lens.enabled = false; lens.intensity = 0f }
                    bindCategory(target)
                }
            }
            StudioCategory.Nose -> {
                slider("Nose", { target.faceShapeParameters().nose }) { v -> target.faceShape { nose = v } }
            }
            StudioCategory.Mouth -> {
                slider("Mouth", { target.faceShapeParameters().mouth }) { v -> target.faceShape { mouth = v } }
            }
            StudioCategory.Teeth -> {
                slider("Tooth whiten", { target.beautyParameters().toothWhiten }) { v ->
                    target.beauty { toothWhiten = v }
                }
            }
            StudioCategory.Body -> {
                slider("Hips", { target.bodyParameters().hips }) { v -> target.body { enabled = v > 0f; hips = v } }
                slider("Waist", { target.bodyParameters().waist }) { v -> target.body { enabled = v > 0f; waist = v } }
                slider("Slim", { target.bodyParameters().slim }) { v -> target.body { enabled = v > 0f; slim = v } }
                slider("Shoulders", { target.bodyParameters().shoulders }) { v ->
                    target.body { enabled = v > 0f; shoulders = v }
                }
                slider("Legs", { target.bodyParameters().legs }) { v -> target.body { enabled = v > 0f; legs = v } }
                slider("Arms", { target.bodyParameters().arms }) { v -> target.body { enabled = v > 0f; arms = v } }
                slider("Torso", { target.bodyParameters().torso }) { v -> target.body { enabled = v > 0f; torso = v } }
                action("Reset body") { target.resetBody(); bindCategory(target) }
            }
            StudioCategory.Makeup -> {
                action("Classic") { target.applyMakeupPreset(MakeupPreset.Classic); bindCategory(target) }
                action("Bright") { target.applyMakeupPreset(MakeupPreset.Bright); bindCategory(target) }
                action("Extravagant") { target.applyMakeupPreset(MakeupPreset.Extravagant); bindCategory(target) }
                val m = target.makeupParameters()
                note("Lip ${m.lipstick.look} · Blush ${m.blush.style} · Shadow ${m.eyeshadow.style}")
                slider("Lipstick", { target.makeupParameters().lipstick.intensity }) { v ->
                    target.makeup { lipstick.enabled = v > 0f; lipstick.intensity = v }
                }
                slider("Blush", { target.makeupParameters().blush.intensity }) { v ->
                    target.makeup { blush.enabled = v > 0f; blush.intensity = v }
                }
                slider("Foundation", { target.makeupParameters().foundation.intensity }) { v ->
                    target.makeup { foundation.enabled = v > 0f; foundation.intensity = v }
                }
                action("Reset makeup") { target.resetMakeup(); bindCategory(target) }
            }
            StudioCategory.Filters -> {
                target.filterCatalog().forEach { def ->
                    action(def.displayName) {
                        target.setFilter(def.id, 0.5f)
                        bindCategory(target)
                    }
                }
                note("Active: ${target.filterParameters().id ?: "none"}")
                slider("Intensity", { target.filterParameters().intensity }) { v ->
                    val id = target.filterParameters().id ?: target.filterCatalog().firstOrNull()?.id ?: return@slider
                    target.setFilter(id, v)
                }
                action("Clear filter") { target.clearFilter(); bindCategory(target) }
            }
            StudioCategory.MagicAr -> {
                note("GPU AR / masks on the live pipeline. Gifts play on the same camera graph.")
                MaskTray.entries.forEach { tray ->
                    action(tray.trayLabel()) {
                        maskTray = tray
                        bindCategory(target)
                    }
                }
                val inTray = target.arCatalog().filter { it.tray == maskTray }.ifEmpty { target.arCatalog() }
                inTray.take(24).forEach { item ->
                    action(item.displayName) {
                        target.setAREffect(item.id, 0.85f)
                        bindCategory(target)
                    }
                }
                slider("AR intensity", { target.arParameters().intensity }) { v ->
                    val id = target.arParameters().effectId ?: inTray.firstOrNull()?.id ?: return@slider
                    target.setAREffect(id, v)
                }
                action("Clear AR") { target.clearAREffect(); bindCategory(target) }
            }
            StudioCategory.Gifts -> {
                note("GiftFX plays on the live camera graph. Play does not rebind CameraX.")
                target.giftCatalog().forEach { gift ->
                    action("${gift.displayName} · ${gift.category.name}") {
                        selectedGiftId = gift.giftId
                        when (val r = target.playGift(gift.giftId)) {
                            is AuraFxResult.Ok -> setStatus("Gift ${gift.displayName}")
                            is AuraFxResult.Err -> setStatus(r.error.message, error = true)
                        }
                    }
                }
                action("Replay last") {
                    when (val r = target.replayGift()) {
                        is AuraFxResult.Ok -> setStatus("Gift replay")
                        is AuraFxResult.Err -> setStatus(r.error.message, error = true)
                    }
                }
                action("Stop gifts") { target.stopAllGifts(); setStatus("Gifts stopped") }
            }
            StudioCategory.Hair -> {
                target.hairCatalog().forEach { style ->
                    action(style.displayName) {
                        target.setHairStyle(style.id)
                        target.hair { enabled = true; intensity = intensity.coerceAtLeast(0.55f) }
                        bindCategory(target)
                    }
                }
                HairColorId.entries.take(10).forEach { color ->
                    action(color.name) {
                        target.hair { enabled = true; this.color = color; intensity = 0.6f }
                        bindCategory(target)
                    }
                }
                slider("Intensity", { target.hairParameters().intensity }) { v ->
                    target.hair { enabled = v > 0f; intensity = v }
                }
                action("Reset hair") { target.resetHair(); bindCategory(target) }
            }
            StudioCategory.FullLook -> {
                note("Full Look applies real makeup presets through the makeup engine.")
                action("Classic look") { target.applyMakeupPreset(MakeupPreset.Classic); bindCategory(target) }
                action("Bright look") { target.applyMakeupPreset(MakeupPreset.Bright); bindCategory(target) }
                action("Extravagant look") { target.applyMakeupPreset(MakeupPreset.Extravagant); bindCategory(target) }
                action("Clear look") { target.resetMakeup(); bindCategory(target) }
            }
            StudioCategory.Outfit -> {
                comingSoon("Outfit")
            }
            StudioCategory.Background -> {
                BackgroundTray.entries.forEach { tray ->
                    action(tray.trayLabel()) {
                        bgTray = tray
                        bindCategory(target)
                    }
                }
                val inTray = target.backgroundCatalog().filter { it.tray == bgTray }.ifEmpty { target.backgroundCatalog() }
                inTray.take(24).forEach { item ->
                    action(item.displayName) {
                        target.background { enabled = true; id = item.id; intensity = 0.75f }
                        bindCategory(target)
                    }
                }
                slider("Intensity", { target.backgroundParameters().intensity }) { v ->
                    val id = target.backgroundParameters().id ?: inTray.firstOrNull()?.id ?: return@slider
                    target.background { enabled = true; this.id = id; intensity = v }
                }
                action("Reset background") { target.resetBackground(); bindCategory(target) }
            }
        }
        if (developerMode) {
            action("Open diagnostic harness") { openDiagnostic() }
        }
    }

    private fun comingSoon(name: String) {
        note("$name is coming in SDK. This control is not a fake overlay.")
    }

    private fun heading(text: String) {
        controlsHost.addView(TextView(this).apply {
            this.text = text
            setTextColor(COL_ACCENT)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, dp(8))
        })
    }

    private fun note(text: String) {
        controlsHost.addView(TextView(this).apply {
            this.text = text
            setTextColor(COL_MUTED)
            textSize = 13f
            setPadding(0, 0, 0, dp(8))
        })
    }

    private fun action(title: String, block: () -> Unit) {
        controlsHost.addView(MaterialButton(this).apply {
            text = title
            isAllCaps = false
            minimumHeight = dp(48)
            setOnClickListener { block() }
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(6) }
        })
    }

    private fun slider(name: String, read: () -> Float, write: (Float) -> Unit) {
        val label = TextView(this).apply {
            setTextColor(COL_TEXT)
            textSize = 13f
        }
        fun refresh(v: Float) {
            label.text = "$name  ${uiFromEngine(v)}"
        }
        val initial = read()
        refresh(initial)
        val bar = SeekBar(this).apply {
            max = 100
            progress = uiFromEngine(initial)
            minimumHeight = dp(48)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val value = engineFromUi(progress)
                    write(value)
                    refresh(value)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        controlsHost.addView(label)
        controlsHost.addView(bar)
    }

    private fun signedSlider(name: String, read: () -> Float, write: (Float) -> Unit) {
        val label = TextView(this).apply { setTextColor(COL_TEXT); textSize = 13f }
        fun refresh(v: Float) {
            label.text = "$name  ${((v + 1f) * 50f).toInt().coerceIn(0, 100)}"
        }
        val initial = read()
        refresh(initial)
        val bar = SeekBar(this).apply {
            max = 100
            progress = ((initial + 1f) * 50f).toInt().coerceIn(0, 100)
            minimumHeight = dp(48)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val value = progress / 50f - 1f
                    write(value)
                    refresh(value)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        controlsHost.addView(label)
        controlsHost.addView(bar)
    }

    private fun engineFromUi(progress: Int): Float {
        return when {
            progress <= 0 -> 0f
            progress >= 100 -> 1f
            else -> progress / 100f
        }
    }

    private fun uiFromEngine(value: Float): Int = (value * 100f).toInt().coerceIn(0, 100)

    private fun openDiagnostic() {
        val diagnostic = when (packageName) {
            "com.aurafx.sample" -> "com.aurafx.sample.SampleActivity"
            else -> "com.aurafx.studio.StudioActivity"
        }
        startActivity(Intent().setClassName(this, diagnostic))
    }

    private fun setStatus(text: String, error: Boolean = false) {
        statusChip.text = text
        statusChip.setTextColor(if (error) COL_DANGER else COL_MUTED)
    }

    private fun iconButton(label: String, onClick: () -> Unit): Button {
        return MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            isAllCaps = false
            minimumWidth = dp(48)
            minimumHeight = dp(48)
            setOnClickListener { onClick() }
        }
    }

    private fun space(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        private const val COL_BG = 0xFF0B0F14.toInt()
        private const val COL_PANEL = 0xF0111827.toInt()
        private const val COL_TEXT = 0xFFF3F4F6.toInt()
        private const val COL_MUTED = 0xFF9CA3AF.toInt()
        private const val COL_ACCENT = 0xFF38BDF8.toInt()
        private const val COL_ACCENT_DIM = 0x6638BDF8.toInt()
        private const val COL_CHIP = 0x990B0F14.toInt()
        private const val COL_DANGER = 0xFFF87171.toInt()
    }
}
