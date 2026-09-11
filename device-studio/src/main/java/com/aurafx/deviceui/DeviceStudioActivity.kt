package com.aurafx.deviceui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Outline
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
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.aurafx.sdk.AuraFx
import com.aurafx.sdk.AuraFxSession
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.AuraFxSessionListener
import com.aurafx.sdk.api.HairColorId
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.LensStyle
import com.aurafx.sdk.api.MakeupPreset
import com.aurafx.sdk.api.SessionConfig
import com.google.android.material.button.MaterialButton
import java.io.File

/**
 * Physical-device Beauty/Effects Studio. Compact bottom carousel only —
 * the camera stays visible. SDK engines are unchanged.
 */
class DeviceStudioActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private enum class StudioCategory(val chip: String) {
        Beauty("Beauty"),
        Face("Face"),
        Eyes("Eyes"),
        ContactLens("Lens"),
        Nose("Nose"),
        Mouth("Mouth"),
        Teeth("Teeth"),
        Body("Body"),
        Makeup("Makeup"),
        Filters("Filter"),
        MagicAr("AR"),
        Gifts("Gifts"),
        Hair("Hair"),
        FullLook("Look"),
        Outfit("Outfit"),
        Background("BG"),
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var textureView: TextureView
    private lateinit var fpsChip: TextView
    private lateinit var statusChip: TextView
    private lateinit var qaPanel: TextView
    private lateinit var categoryRow: LinearLayout
    private lateinit var thumbRow: LinearLayout
    private lateinit var intensityBar: SeekBar
    private lateinit var intensityLabel: TextView
    private lateinit var intensityRow: LinearLayout
    private lateinit var beforeButton: MaterialButton
    private lateinit var recordButton: MaterialButton
    private lateinit var tray: LinearLayout
    private lateinit var previewHost: FrameLayout

    private var session: AuraFxSession? = null
    private var previewWindow: Surface? = null
    private var surfaceReady = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var pendingFacing = LensFacing.FRONT
    private var category = StudioCategory.Beauty
    private var qaVisible = false
    private var recording = false
    private var selectedId: String? = null
    private var applyingSlider = false
    private val selectedByCategory = HashMap<StudioCategory, String>()

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
                val track = session?.vision?.latest()
                val face = if (track?.landmarks != null) "face" else "no face"
                fpsChip.visibility = View.VISIBLE
                fpsChip.text = "%.0f FPS · %s".format(snap.fps, face)
                if (qaVisible) {
                    qaPanel.text = buildString {
                        appendLine("FPS ${"%.1f".format(snap.fps)}   frame ${"%.2f".format(snap.frameProcessTimeMs)} ms")
                        appendLine("dropped ${snap.droppedFrames}   presented ${snap.presentedFrames}")
                        appendLine("camera ${snap.cameraWidth}x${snap.cameraHeight}   ${snap.cameraFacing}")
                        appendLine("GPU ${snap.gpuRenderer ?: "n/a"}")
                        appendLine(session?.visionDiagnostics() ?: "")
                        appendLine("camera bound=${snap.cameraBound}   sdk=${snap.sdkState}")
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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
            setBackgroundColor(COL_BG)
        }

        textureView = TextureView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
            surfaceTextureListener = this@DeviceStudioActivity
            isOpaque = true
        }
        val previewFrame = PortraitPreviewFrame(this).apply {
            layoutParams = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER)
            setBackgroundColor(COL_BG)
            addView(textureView)
        }
        previewHost = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
            addView(previewFrame)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(4))
        }
        topBar.addView(iconButton("Close") { finish() })
        topBar.addView(space())
        fpsChip = TextView(this).apply {
            text = "—"
            setTextColor(COL_TEXT)
            setBackgroundColor(COL_CHIP)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            textSize = 12f
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
            textSize = 11f
            setPadding(dp(12), 0, dp(12), dp(2))
            text = "Starting…"
            maxLines = 1
        }
        qaPanel = TextView(this).apply {
            setTextColor(COL_MUTED)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setBackgroundColor(COL_PANEL)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            visibility = View.GONE
        }

        tray = buildTray()

        root.addView(topBar)
        root.addView(statusChip)
        root.addView(qaPanel)
        root.addView(previewHost)
        root.addView(tray)
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            topBar.setPadding(bars.left + dp(8), bars.top + dp(4), bars.right + dp(8), dp(4))
            tray.setPadding(bars.left, dp(6), bars.right, bars.bottom + dp(8))
            insets
        }
        ViewCompat.requestApplyInsets(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })

        when (val result = AuraFx.initialize(this)) {
            is AuraFxResult.Ok -> setStatus("AuraFX Studio ready")
            is AuraFxResult.Err -> setStatus(result.error.message, error = true)
        }
        rebuildCategories()
        bindCarousel()
        requestCameraThenStart()
        mainHandler.post(metricsTicker)
    }

    private fun buildTray(): LinearLayout {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COL_PANEL)
            elevation = dp(10).toFloat()
        }

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        beforeButton = compactButton("Before").apply {
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
        recordButton = compactButton("Record").apply {
            setOnClickListener { toggleRecord() }
        }
        dock.addView(beforeButton)
        dock.addView(recordButton)
        dock.addView(compactButton("Reset").apply {
            setOnClickListener {
                session?.resetAll()
                selectedId = null
                selectedByCategory.clear()
                intensityBar.progress = 0
                bindCarousel()
                setStatus("All effects reset")
            }
        })
        column.addView(dock)

        intensityLabel = TextView(this).apply {
            text = "intensity"
            setTextColor(COL_MUTED)
            textSize = 10f
            gravity = Gravity.CENTER
        }
        column.addView(intensityLabel)
        intensityRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(10), 0)
        }
        intensityRow.addView(TextView(this).apply {
            text = "0"
            setTextColor(COL_MUTED)
            textSize = 11f
        })
        intensityBar = SeekBar(this).apply {
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(0, dp(28), 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser || applyingSlider) return
                    applyIntensity(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        intensityRow.addView(intensityBar)
        intensityRow.addView(TextView(this).apply {
            text = "100"
            setTextColor(COL_MUTED)
            textSize = 11f
        })
        column.addView(intensityRow)

        val thumbScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            setPadding(dp(8), dp(4), dp(8), dp(2))
        }
        thumbRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        thumbScroll.addView(thumbRow)
        column.addView(thumbScroll)

        val catScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(8), dp(2), dp(8), dp(4))
        }
        categoryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        catScroll.addView(categoryRow)
        column.addView(catScroll)
        return column
    }

    private fun rebuildCategories() {
        categoryRow.removeAllViews()
        StudioCategory.entries.forEach { cat ->
            val chip = TextView(this).apply {
                text = cat.chip
                setTextColor(if (cat == category) COL_TEXT else COL_MUTED)
                textSize = 13f
                typeface = if (cat == category) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setPadding(dp(12), dp(8), dp(12), dp(8))
                if (cat == category) setBackgroundColor(COL_ACCENT_DIM)
                setOnClickListener {
                    category = cat
                    selectedId = selectedByCategory[cat]
                    rebuildCategories()
                    bindCarousel()
                }
            }
            categoryRow.addView(chip)
        }
    }

    private fun bindCarousel() {
        thumbRow.removeAllViews()
        val items = itemsFor(category)
        if (items.isEmpty()) {
            intensityRow.visibility = View.GONE
            intensityLabel.visibility = View.GONE
            thumbRow.addView(TextView(this).apply {
                text = if (category == StudioCategory.Outfit) "Outfit is coming in SDK" else "Waiting…"
                setTextColor(COL_MUTED)
                setPadding(dp(12), dp(16), dp(12), dp(16))
            })
            return
        }
        if (selectedId == null || items.none { it.id == selectedId }) {
            selectedId = selectedByCategory[category] ?: items.first().id
        }
        val current = items.firstOrNull { it.id == selectedId } ?: items.first()
        selectedId = current.id
        intensityRow.visibility = if (current.intensityEnabled) View.VISIBLE else View.GONE
        intensityLabel.visibility = intensityRow.visibility
        applyingSlider = true
        intensityBar.progress = readUiProgress(current)
        applyingSlider = false
        val thumbPx = dp(64)
        items.forEach { item ->
            thumbRow.addView(thumbCell(item, item.id == selectedId, thumbPx))
        }
    }

    private fun thumbCell(item: CarouselItem, selected: Boolean, sizePx: Int): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(6), 0, dp(6), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { onSelect(item) }
        }
        val ring = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx + dp(6), sizePx + dp(6))
        }
        val image = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx, Gravity.CENTER)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(StudioThumbnails.bitmap(item))
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        ring.addView(image)
        if (selected) {
            ring.setBackgroundColor(COL_RING)
            ring.clipToOutline = true
            ring.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        col.addView(ring)
        col.addView(TextView(this).apply {
            text = item.title
            setTextColor(if (selected) COL_TEXT else COL_MUTED)
            textSize = 10f
            gravity = Gravity.CENTER
            maxWidth = sizePx + dp(12)
            maxLines = 1
            setPadding(0, dp(4), 0, 0)
        })
        return col
    }

    private fun onSelect(item: CarouselItem) {
        selectedId = item.id
        selectedByCategory[category] = item.id
        val target = session
        if (target == null) {
            bindCarousel()
            return
        }
        if (item.kind == CarouselKind.Outfit) {
            setStatus("Outfit is coming in SDK")
            bindCarousel()
            return
        }
        applyingSlider = true
        val progress = if (intensityBar.progress == 0) 50 else intensityBar.progress
        intensityBar.progress = progress
        applyingSlider = false
        applyItem(target, item, engineFromUi(progress, item.defaultStrength), playGift = true)
        bindCarousel()
        setStatus("${item.title} · ${target.visionDiagnostics()}")
    }

    private fun applyIntensity(progress: Int) {
        val target = session ?: return
        val item = itemsFor(category).firstOrNull { it.id == selectedId } ?: return
        applyItem(target, item, engineFromUi(progress, item.defaultStrength), playGift = progress > 0)
    }

    private fun readUiProgress(item: CarouselItem): Int {
        val target = session ?: return 0
        val value = readEngine(target, item) ?: return intensityBar.progress
        return if (item.signed) signedToUi(value) else uiFromEngine(value, item.defaultStrength)
    }

    private fun applyItem(target: AuraFxSession, item: CarouselItem, amount: Float, playGift: Boolean) {
        val on = amount > 0.001f
        when (item.kind) {
            CarouselKind.Beauty -> when (item.payload) {
                "fineSmooth" -> {
                    target.beauty { fineSmooth = amount }
                    target.skin { fineSmooth = amount }
                }
                "smoothness" -> target.skin { smoothness = amount }
                "blemish" -> target.skin { blemishReduction = amount }
                "evenness" -> target.skin { evenness = amount }
                "brightness" -> target.skin { brightness = amount }
                "whiten" -> {
                    target.beauty { whiten = amount }
                    target.skin { whiten = amount }
                }
                "ruddy" -> {
                    target.beauty { ruddy = amount }
                    target.skin { ruddy = amount }
                }
                "circles" -> target.beauty { circles = amount }
            }
            CarouselKind.Face -> when (item.payload) {
                "vFace" -> target.faceShape { vFace = amount }
                "cheekThin" -> target.faceShape { cheekThin = amount }
                "cheekSmall" -> target.faceShape { cheekSmall = amount }
                "cheekNarrow" -> target.faceShape { cheekNarrow = amount }
            }
            CarouselKind.Eyes -> when (item.payload) {
                "eyeEnlarge" -> target.faceShape { eyeEnlarge = amount }
                "eyeDistance" -> target.faceShape { eyeDistance = signedFromUi(intensityBar.progress) }
            }
            CarouselKind.ContactLens -> target.makeup {
                lens.enabled = on
                lens.style = LensStyle.valueOf(item.payload)
                lens.intensity = amount
            }
            CarouselKind.Nose -> target.faceShape { nose = amount }
            CarouselKind.Mouth -> target.faceShape { mouth = amount }
            CarouselKind.Teeth -> target.beauty { toothWhiten = amount }
            CarouselKind.Body -> target.body {
                enabled = on
                when (item.payload) {
                    "hips" -> hips = amount
                    "waist" -> waist = amount
                    "slim" -> slim = amount
                    "shoulders" -> shoulders = amount
                    "legs" -> legs = amount
                    "arms" -> arms = amount
                    "torso" -> torso = amount
                }
            }
            CarouselKind.MakeupPreset -> {
                if (!on) {
                    target.resetMakeup()
                } else {
                    target.applyMakeupPreset(MakeupPreset.valueOf(item.payload))
                    target.makeup {
                        lipstick.intensity = amount
                        blush.intensity = amount
                        foundation.intensity = amount
                        lipstick.enabled = on
                        blush.enabled = on
                        foundation.enabled = on
                    }
                }
            }
            CarouselKind.MakeupElement -> target.makeup {
                when (item.payload) {
                    "lipstick" -> { lipstick.enabled = on; lipstick.intensity = amount }
                    "blush" -> { blush.enabled = on; blush.intensity = amount }
                    "foundation" -> { foundation.enabled = on; foundation.intensity = amount }
                    "concealer" -> { concealer.enabled = on; concealer.intensity = amount }
                    "contour" -> { contour.enabled = on; contour.intensity = amount }
                    "highlight" -> { highlight.enabled = on; highlight.intensity = amount }
                    "eyebrow" -> { eyebrow.enabled = on; eyebrow.intensity = amount }
                    "eyeshadow" -> { eyeshadow.enabled = on; eyeshadow.intensity = amount }
                    "eyeliner" -> { eyeliner.enabled = on; eyeliner.intensity = amount }
                    "eyelashes" -> { eyelashes.enabled = on; eyelashes.intensity = amount }
                    "lipLiner" -> { lipLiner.enabled = on; lipLiner.intensity = amount }
                    "lipGloss" -> { lipGloss.enabled = on; lipGloss.intensity = amount }
                }
            }
            CarouselKind.Filter -> {
                if (!on) target.clearFilter() else target.setFilter(item.payload, amount)
            }
            CarouselKind.Ar -> {
                if (!on) target.clearAREffect() else target.setAREffect(item.payload, amount)
            }
            CarouselKind.Gift -> {
                if (!playGift || !on) {
                    target.stopAllGifts()
                } else {
                    when (val r = target.playGift(item.payload)) {
                        is AuraFxResult.Ok -> Unit
                        is AuraFxResult.Err -> setStatus(r.error.message, error = true)
                    }
                }
            }
            CarouselKind.HairStyle -> {
                target.setHairStyle(item.payload)
                target.hair { enabled = on; intensity = amount.coerceAtLeast(if (on) 0.35f else 0f) }
            }
            CarouselKind.HairColor -> {
                val color = HairColorId.valueOf(item.payload)
                target.hair { enabled = on; this.color = color; intensity = amount }
            }
            CarouselKind.FullLook -> {
                if (!on) {
                    target.resetMakeup()
                    target.resetBeauty()
                    target.clearFilter()
                } else when (item.payload) {
                    "classic" -> {
                        target.applyMakeupPreset(MakeupPreset.Classic)
                        target.beauty { fineSmooth = 0.35f * amount; whiten = 0.2f * amount }
                        target.setFilter("natural.true", 0.7f * amount)
                    }
                    "bright" -> {
                        target.applyMakeupPreset(MakeupPreset.Bright)
                        target.beauty { fineSmooth = 0.4f * amount; toothWhiten = 0.25f * amount }
                        target.setFilter("glow.pearl", 0.75f * amount)
                    }
                    "extravagant" -> {
                        target.applyMakeupPreset(MakeupPreset.Extravagant)
                        target.faceShape { vFace = 0.2f * amount; eyeEnlarge = 0.15f * amount }
                        target.setFilter("warm.golden", 0.8f * amount)
                    }
                }
            }
            CarouselKind.Background -> {
                if (!on) target.resetBackground()
                else target.background { enabled = true; id = item.payload; intensity = amount }
            }
            CarouselKind.Outfit -> Unit
        }
    }

    private fun readEngine(target: AuraFxSession, item: CarouselItem): Float? = when (item.kind) {
        CarouselKind.Beauty -> when (item.payload) {
            "fineSmooth" -> target.beautyParameters().fineSmooth
            "smoothness" -> target.skinParameters().smoothness
            "blemish" -> target.skinParameters().blemishReduction
            "evenness" -> target.skinParameters().evenness
            "brightness" -> target.skinParameters().brightness
            "whiten" -> target.beautyParameters().whiten
            "ruddy" -> target.beautyParameters().ruddy
            "circles" -> target.beautyParameters().circles
            else -> null
        }
        CarouselKind.Face -> when (item.payload) {
            "vFace" -> target.faceShapeParameters().vFace
            "cheekThin" -> target.faceShapeParameters().cheekThin
            "cheekSmall" -> target.faceShapeParameters().cheekSmall
            "cheekNarrow" -> target.faceShapeParameters().cheekNarrow
            else -> null
        }
        CarouselKind.Eyes -> when (item.payload) {
            "eyeEnlarge" -> target.faceShapeParameters().eyeEnlarge
            "eyeDistance" -> target.faceShapeParameters().eyeDistance
            else -> null
        }
        CarouselKind.ContactLens -> target.makeupParameters().lens.intensity.takeIf {
            target.makeupParameters().lens.style.name == item.payload
        } ?: 0f
        CarouselKind.Nose -> target.faceShapeParameters().nose
        CarouselKind.Mouth -> target.faceShapeParameters().mouth
        CarouselKind.Teeth -> target.beautyParameters().toothWhiten
        CarouselKind.Body -> when (item.payload) {
            "hips" -> target.bodyParameters().hips
            "waist" -> target.bodyParameters().waist
            "slim" -> target.bodyParameters().slim
            "shoulders" -> target.bodyParameters().shoulders
            "legs" -> target.bodyParameters().legs
            "arms" -> target.bodyParameters().arms
            "torso" -> target.bodyParameters().torso
            else -> null
        }
        CarouselKind.MakeupPreset -> target.makeupParameters().lipstick.intensity
        CarouselKind.MakeupElement -> when (item.payload) {
            "lipstick" -> target.makeupParameters().lipstick.intensity
            "blush" -> target.makeupParameters().blush.intensity
            "foundation" -> target.makeupParameters().foundation.intensity
            "concealer" -> target.makeupParameters().concealer.intensity
            "contour" -> target.makeupParameters().contour.intensity
            "highlight" -> target.makeupParameters().highlight.intensity
            "eyebrow" -> target.makeupParameters().eyebrow.intensity
            "eyeshadow" -> target.makeupParameters().eyeshadow.intensity
            "eyeliner" -> target.makeupParameters().eyeliner.intensity
            "eyelashes" -> target.makeupParameters().eyelashes.intensity
            "lipLiner" -> target.makeupParameters().lipLiner.intensity
            "lipGloss" -> target.makeupParameters().lipGloss.intensity
            else -> null
        }
        CarouselKind.Filter ->
            if (target.filterParameters().id == item.payload) target.filterParameters().intensity else 0f
        CarouselKind.Ar ->
            if (target.arParameters().effectId == item.payload) target.arParameters().intensity else 0f
        CarouselKind.Gift -> if (target.lastGiftId() == item.payload) 0.5f else 0f
        CarouselKind.HairStyle ->
            if (target.hairParameters().styleId == item.payload) target.hairParameters().intensity else 0f
        CarouselKind.HairColor ->
            if (target.hairParameters().color.name == item.payload) target.hairParameters().intensity else 0f
        CarouselKind.FullLook -> target.filterParameters().intensity
        CarouselKind.Background ->
            if (target.backgroundParameters().id == item.payload) target.backgroundParameters().intensity else 0f
        CarouselKind.Outfit -> 0f
    }

    private fun itemsFor(cat: StudioCategory): List<CarouselItem> {
        val s = session
        return when (cat) {
            StudioCategory.Beauty -> listOf(
                item("beauty.fine", "Fine smooth", CarouselKind.Beauty, "fineSmooth"),
                item("beauty.smooth", "Smoothness", CarouselKind.Beauty, "smoothness"),
                item("beauty.blemish", "Blemish", CarouselKind.Beauty, "blemish"),
                item("beauty.even", "Evenness", CarouselKind.Beauty, "evenness"),
                item("beauty.bright", "Brightness", CarouselKind.Beauty, "brightness"),
                item("beauty.whiten", "Whiten", CarouselKind.Beauty, "whiten"),
                item("beauty.ruddy", "Ruddy", CarouselKind.Beauty, "ruddy"),
                item("beauty.circles", "Dark circles", CarouselKind.Beauty, "circles"),
            )
            StudioCategory.Face -> listOf(
                item("face.v", "V-face", CarouselKind.Face, "vFace"),
                item("face.thin", "Cheek thin", CarouselKind.Face, "cheekThin"),
                item("face.small", "Cheek small", CarouselKind.Face, "cheekSmall"),
                item("face.narrow", "Cheek narrow", CarouselKind.Face, "cheekNarrow"),
            )
            StudioCategory.Eyes -> listOf(
                item("eyes.enlarge", "Enlarge", CarouselKind.Eyes, "eyeEnlarge"),
                item("eyes.dist", "Distance", CarouselKind.Eyes, "eyeDistance", signed = true),
            )
            StudioCategory.ContactLens -> LensStyle.entries.map {
                item("lens.${it.name}", it.name, CarouselKind.ContactLens, it.name)
            }
            StudioCategory.Nose -> listOf(item("nose", "Nose", CarouselKind.Nose, "nose"))
            StudioCategory.Mouth -> listOf(item("mouth", "Mouth", CarouselKind.Mouth, "mouth"))
            StudioCategory.Teeth -> listOf(item("teeth", "Tooth whiten", CarouselKind.Teeth, "toothWhiten"))
            StudioCategory.Body -> listOf(
                item("body.hips", "Hips", CarouselKind.Body, "hips"),
                item("body.waist", "Waist", CarouselKind.Body, "waist"),
                item("body.slim", "Slim", CarouselKind.Body, "slim"),
                item("body.shoulders", "Shoulders", CarouselKind.Body, "shoulders"),
                item("body.legs", "Legs", CarouselKind.Body, "legs"),
                item("body.arms", "Arms", CarouselKind.Body, "arms"),
                item("body.torso", "Torso", CarouselKind.Body, "torso"),
            )
            StudioCategory.Makeup -> buildList {
                MakeupPreset.entries.forEach {
                    add(item("makeup.preset.${it.name}", it.name, CarouselKind.MakeupPreset, it.name))
                }
                listOf(
                    "lipstick" to "Lipstick",
                    "blush" to "Blush",
                    "foundation" to "Foundation",
                    "concealer" to "Concealer",
                    "contour" to "Contour",
                    "highlight" to "Highlight",
                    "eyebrow" to "Eyebrow",
                    "eyeshadow" to "Eyeshadow",
                    "eyeliner" to "Eyeliner",
                    "eyelashes" to "Lashes",
                    "lipLiner" to "Lip liner",
                    "lipGloss" to "Lip gloss",
                ).forEach { (id, title) ->
                    add(item("makeup.el.$id", title, CarouselKind.MakeupElement, id))
                }
            }
            StudioCategory.Filters -> (s?.filterCatalog() ?: emptyList()).map {
                item("filter.${it.id}", it.displayName, CarouselKind.Filter, it.id, it.defaultIntensity)
            }
            StudioCategory.MagicAr -> (s?.arCatalog() ?: emptyList()).map {
                item("ar.${it.id}", it.displayName, CarouselKind.Ar, it.id, it.defaultIntensity)
            }
            StudioCategory.Gifts -> (s?.giftCatalog() ?: emptyList()).map {
                item("gift.${it.giftId}", it.displayName, CarouselKind.Gift, it.giftId)
            }
            StudioCategory.Hair -> buildList {
                s?.hairCatalog()?.forEach {
                    add(item("hair.style.${it.id}", it.displayName, CarouselKind.HairStyle, it.id))
                }
                HairColorId.entries.filter { it != HairColorId.Custom }.forEach {
                    add(item("hair.color.${it.name}", it.name, CarouselKind.HairColor, it.name))
                }
            }
            StudioCategory.FullLook -> listOf(
                item("look.classic", "Classic", CarouselKind.FullLook, "classic"),
                item("look.bright", "Bright", CarouselKind.FullLook, "bright"),
                item("look.extra", "Extravagant", CarouselKind.FullLook, "extravagant"),
            )
            StudioCategory.Outfit -> listOf(
                item("outfit.soon", "Coming soon", CarouselKind.Outfit, "soon", intensityEnabled = false),
            )
            StudioCategory.Background -> (s?.backgroundCatalog() ?: emptyList()).map {
                item("bg.${it.id}", it.displayName, CarouselKind.Background, it.id, 0.75f)
            }
        }
    }

    private fun item(
        id: String,
        title: String,
        kind: CarouselKind,
        payload: String,
        defaultStrength: Float = 0.5f,
        signed: Boolean = false,
        intensityEnabled: Boolean = true,
    ) = CarouselItem(id, title, kind, payload, defaultStrength, signed, intensityEnabled)

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
        if (width > 0 && height > 0) session?.resizePreview(width, height)
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
                    bindCarousel()
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

    private fun engineFromUi(progress: Int, default: Float): Float {
        val d = default.coerceIn(0.05f, 0.95f)
        return when {
            progress <= 0 -> 0f
            progress >= 100 -> 1f
            progress == 50 -> d
            progress < 50 -> d * (progress / 50f)
            else -> d + (1f - d) * ((progress - 50) / 50f)
        }
    }

    private fun uiFromEngine(value: Float, default: Float): Int {
        val d = default.coerceIn(0.05f, 0.95f)
        return when {
            value <= 0.001f -> 0
            value >= 0.999f -> 100
            value <= d -> ((value / d) * 50f).toInt().coerceIn(0, 50)
            else -> (50f + ((value - d) / (1f - d)) * 50f).toInt().coerceIn(50, 100)
        }
    }

    private fun signedFromUi(progress: Int): Float = progress / 50f - 1f
    private fun signedToUi(value: Float): Int = ((value + 1f) * 50f).toInt().coerceIn(0, 100)

    private fun setStatus(text: String, error: Boolean = false) {
        statusChip.text = text
        statusChip.setTextColor(if (error) COL_DANGER else COL_MUTED)
    }

    private fun compactButton(label: String): MaterialButton {
        return MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            isAllCaps = false
            minimumHeight = dp(36)
            minHeight = dp(36)
            insetTop = 0
            insetBottom = 0
            textSize = 12f
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
    }

    private fun iconButton(label: String, onClick: () -> Unit): Button {
        return MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            isAllCaps = false
            minimumWidth = dp(48)
            minimumHeight = dp(40)
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
        private const val COL_PANEL = 0xE0111827.toInt()
        private const val COL_TEXT = 0xFFF3F4F6.toInt()
        private const val COL_MUTED = 0xFF9CA3AF.toInt()
        private const val COL_ACCENT_DIM = 0x6638BDF8.toInt()
        private const val COL_CHIP = 0x990B0F14.toInt()
        private const val COL_DANGER = 0xFFF87171.toInt()
        private const val COL_RING = 0xFFF8FAFC.toInt()
    }
}
