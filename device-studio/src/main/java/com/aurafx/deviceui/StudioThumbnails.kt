package com.aurafx.deviceui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.aurafx.sdk.ar.AREffectCatalog
import com.aurafx.sdk.ar.ARThumbnail
import com.aurafx.sdk.filter.FilterCatalog
import com.aurafx.sdk.scene.BackgroundCatalog
import com.aurafx.sdk.scene.HairCatalog
import kotlin.math.cos
import kotlin.math.sin

/**
 * Static, per-option studio thumbnails. Never samples the live camera.
 * Filter / AR / background use the SDK catalog renderers; other looks are
 * unique illustrated faces keyed by option id.
 */
internal object StudioThumbnails {
    private const val SIZE = 128
    private val cache = LinkedHashMap<String, Bitmap>(256, 0.75f, true)

    fun bitmap(item: CarouselItem): Bitmap {
        cache[item.id]?.let { if (!it.isRecycled) return it }
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        paint(canvas, SIZE, item)
        if (cache.size > 220) {
            val oldest = cache.entries.iterator()
            if (oldest.hasNext()) {
                oldest.next().value.recycle()
                oldest.remove()
            }
        }
        cache[item.id] = bmp
        return bmp
    }

    private fun paint(canvas: Canvas, size: Int, item: CarouselItem) {
        when (item.kind) {
            CarouselKind.Filter -> {
                val def = FilterCatalog.require(item.payload) ?: return face(canvas, size, item)
                blitArgb(canvas, def.thumbnailArgb(size, size), size)
            }
            CarouselKind.Ar -> {
                val def = AREffectCatalog.require(item.payload) ?: return face(canvas, size, item)
                blitArgb(canvas, ARThumbnail.argb(def, size, size), size)
                faceOverlay(canvas, size, lips = 0xAAFF6688.toInt(), glasses = true)
            }
            CarouselKind.Background -> {
                val def = BackgroundCatalog.require(item.payload)
                if (def != null) {
                    background(canvas, size, def.colorA, def.colorB, def.shaderMode)
                    silhouette(canvas, size)
                } else {
                    face(canvas, size, item)
                }
            }
            CarouselKind.HairColor -> {
                val rgb = HairCatalog.colors.firstOrNull { it.id.name == item.payload }?.rgb
                    ?: floatArrayOf(0.3f, 0.18f, 0.1f)
                face(canvas, size, item, hair = rgbColor(rgb))
            }
            else -> face(canvas, size, item)
        }
    }

    private fun blitArgb(canvas: Canvas, argb: IntArray, size: Int) {
        val bmp = Bitmap.createBitmap(argb, size, size, Bitmap.Config.ARGB_8888)
        canvas.drawBitmap(bmp, 0f, 0f, null)
        bmp.recycle()
    }

    private fun background(canvas: Canvas, size: Int, a: FloatArray, b: FloatArray, mode: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val n = size
        for (y in 0 until n step 2) {
            val t = y / (n - 1f)
            val wobble = 0.08f * sin((y + mode * 13) * 0.11f)
            paint.color = rgb(
                a[0] * (1f - t) + b[0] * t + wobble,
                a[1] * (1f - t) + b[1] * t,
                a[2] * (1f - t) + b[2] * t - wobble * 0.5f,
            )
            canvas.drawRect(0f, y.toFloat(), n.toFloat(), (y + 2).toFloat(), paint)
        }
        if (mode >= 5) {
            paint.color = rgb(a[0], a[1], a[2])
            paint.alpha = 140
            for (i in 0 until 8) {
                val cx = (18 + (i * 47 + mode * 11) % (n - 24)).toFloat()
                val cy = (12 + (i * 29) % (n / 2)).toFloat()
                canvas.drawCircle(cx, cy, 7f + (i % 3) * 3f, paint)
            }
        }
    }

    private fun silhouette(canvas: Canvas, size: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE0C4A882.toInt() }
        val cx = size * 0.5f
        val cy = size * 0.62f
        canvas.drawOval(cx - size * 0.22f, cy - size * 0.38f, cx + size * 0.22f, size * 1.05f, p)
        canvas.drawCircle(cx, size * 0.38f, size * 0.16f, p)
    }

    private fun face(
        canvas: Canvas,
        size: Int,
        item: CarouselItem,
        hair: Int? = null,
    ) {
        val seed = item.id.hashCode()
        val bg = when (item.kind) {
            CarouselKind.Gift -> rgb(0.08f + bit(seed, 0) * 0.12f, 0.05f, 0.16f + bit(seed, 3) * 0.2f)
            CarouselKind.FullLook -> 0xFF1A1420.toInt()
            else -> 0xFF1C2430.toInt()
        }
        canvas.drawColor(bg)

        val skin = when (item.kind) {
            CarouselKind.Beauty, CarouselKind.Teeth -> when (item.payload) {
                "whiten" -> 0xFFF3D7C4.toInt()
                "ruddy" -> 0xFFE8A090.toInt()
                "brightness" -> 0xFFF0C8A8.toInt()
                "toothWhiten" -> 0xFFE8C4A8.toInt()
                else -> 0xFFE0B089.toInt()
            }
            CarouselKind.MakeupElement, CarouselKind.MakeupPreset -> 0xFFE4B392.toInt()
            else -> 0xFFDDA882.toInt()
        }

        val cx = size * 0.5f
        val cy = size * 0.54f
        val rx = size * 0.32f
        val ry = size * (if (item.payload == "vFace" || item.payload == "cheekThin") 0.40f else 0.38f)

        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = hair ?: when (item.kind) {
                CarouselKind.HairStyle -> hairStyleColor(item.payload)
                else -> 0xFF2A1A12.toInt()
            }
        }
        val hairPath = Path()
        val bangs = item.payload.contains("bang", true) || item.payload.contains("pixie")
        val bob = item.payload.contains("bob")
        val pony = item.payload.contains("pony")
        hairPath.addOval(cx - rx * 1.15f, cy - ry * 1.35f, cx + rx * 1.15f, cy + ry * 0.15f, Path.Direction.CW)
        if (bob) {
            hairPath.addRect(cx - rx * 1.1f, cy, cx + rx * 1.1f, cy + ry * 0.55f, Path.Direction.CW)
        }
        canvas.drawPath(hairPath, hairPaint)
        if (pony) {
            canvas.drawOval(cx + rx * 0.7f, cy - ry * 1.1f, cx + rx * 1.25f, cy - ry * 0.2f, hairPaint)
        }

        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skin }
        val faceRx = if (item.payload == "vFace") rx * 0.86f else rx
        val cheek = when (item.payload) {
            "cheekThin", "cheekNarrow" -> 0.88f
            "cheekSmall" -> 0.90f
            else -> 1f
        }
        canvas.drawOval(cx - faceRx * cheek, cy - ry, cx + faceRx * cheek, cy + ry, facePaint)

        if (bangs) {
            canvas.drawRect(cx - rx * 0.9f, cy - ry * 1.05f, cx + rx * 0.9f, cy - ry * 0.55f, hairPaint)
        }

        val eyeY = cy - ry * 0.12f
        val eyeDx = when {
            item.payload == "eyeDistance" && item.kind == CarouselKind.Eyes -> rx * 0.48f
            item.kind == CarouselKind.ContactLens -> rx * 0.36f
            else -> rx * 0.36f
        }
        val eyeR = if (item.payload == "eyeEnlarge") size * 0.075f else size * 0.055f
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val iris = Paint(Paint.ANTI_ALIAS_FLAG)
        iris.color = when (item.kind) {
            CarouselKind.ContactLens -> lensColor(item.payload)
            else -> 0xFF3A2418.toInt()
        }
        val pupil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        for (sign in floatArrayOf(-1f, 1f)) {
            canvas.drawCircle(cx + sign * eyeDx, eyeY, eyeR, white)
            canvas.drawCircle(cx + sign * eyeDx, eyeY, eyeR * 0.62f, iris)
            canvas.drawCircle(cx + sign * eyeDx, eyeY, eyeR * 0.28f, pupil)
        }

        val brow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2A1A12.toInt()
            strokeWidth = if (item.payload == "eyebrow") 5.5f else 3.2f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        for (sign in floatArrayOf(-1f, 1f)) {
            canvas.drawLine(
                cx + sign * (eyeDx - eyeR), eyeY - eyeR * 1.55f,
                cx + sign * (eyeDx + eyeR), eyeY - eyeR * 1.85f,
                brow,
            )
        }

        if (item.payload == "eyeshadow" || item.kind == CarouselKind.MakeupPreset) {
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG)
            shadow.color = when (item.payload) {
                "Bright" -> 0x88E8B878.toInt()
                "Extravagant" -> 0xAA9B4EC7.toInt()
                else -> 0x889B5A3C.toInt()
            }
            canvas.drawOval(cx - eyeDx - eyeR * 1.3f, eyeY - eyeR * 1.4f, cx - eyeDx + eyeR * 1.3f, eyeY, shadow)
            canvas.drawOval(cx + eyeDx - eyeR * 1.3f, eyeY - eyeR * 1.4f, cx + eyeDx + eyeR * 1.3f, eyeY, shadow)
        }

        if (item.payload == "eyeliner" || item.payload == "eyelashes") {
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                strokeWidth = 3.4f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(cx - eyeDx - eyeR, eyeY + eyeR * 0.2f, cx - eyeDx + eyeR * 1.4f, eyeY - eyeR * 0.4f, line)
            canvas.drawLine(cx + eyeDx + eyeR, eyeY + eyeR * 0.2f, cx + eyeDx - eyeR * 1.4f, eyeY - eyeR * 0.4f, line)
        }

        val blush = Paint(Paint.ANTI_ALIAS_FLAG)
        blush.color = when {
            item.payload == "blush" || item.payload == "ruddy" -> 0xCCE07080.toInt()
            item.kind == CarouselKind.MakeupPreset -> 0x88E07080.toInt()
            else -> 0x55E07080.toInt()
        }
        val blushR = size * 0.08f
        canvas.drawCircle(cx - rx * 0.55f, cy + ry * 0.18f, blushR, blush)
        canvas.drawCircle(cx + rx * 0.55f, cy + ry * 0.18f, blushR, blush)

        val mouthY = cy + ry * 0.42f
        val lip = Paint(Paint.ANTI_ALIAS_FLAG)
        lip.color = when (item.payload) {
            "lipstick", "Classic" -> 0xFFB3263A.toInt()
            "Bright" -> 0xFFE24B6A.toInt()
            "Extravagant" -> 0xFF8B1230.toInt()
            "lipGloss" -> 0xFFE86A88.toInt()
            else -> 0xFFC47880.toInt()
        }
        val mw = if (item.payload == "mouth") rx * 0.55f else rx * 0.38f
        val mh = if (item.kind == CarouselKind.Teeth) ry * 0.16f else ry * 0.10f
        canvas.drawOval(cx - mw, mouthY - mh, cx + mw, mouthY + mh, lip)
        if (item.kind == CarouselKind.Teeth || item.payload == "toothWhiten") {
            val teeth = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawOval(cx - mw * 0.7f, mouthY - mh * 0.35f, cx + mw * 0.7f, mouthY + mh * 0.55f, teeth)
        }

        if (item.payload == "nose") {
            val n = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66A06040.toInt() }
            canvas.drawOval(cx - rx * 0.08f, cy - ry * 0.02f, cx + rx * 0.08f, cy + ry * 0.28f, n)
        }

        if (item.kind == CarouselKind.Gift) {
            val spark = Paint(Paint.ANTI_ALIAS_FLAG)
            spark.color = Color.argb(220, 80 + (seed and 127), 180, 255)
            for (i in 0 until 6) {
                val a = i * 1.05f
                canvas.drawCircle(
                    cx + cos(a) * rx * 0.95f,
                    cy + sin(a) * ry * 0.9f,
                    4f + (i % 3),
                    spark,
                )
            }
        }

        if (item.kind == CarouselKind.Body) {
            canvas.drawColor(0x66101820)
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skin }
            canvas.drawCircle(cx, size * 0.28f, size * 0.12f, body)
            val torso = RectF(cx - size * 0.16f, size * 0.38f, cx + size * 0.16f, size * 0.92f)
            when (item.payload) {
                "waist" -> torso.inset(size * 0.04f, 0f)
                "hips" -> torso.right += size * 0.04f
                "slim" -> torso.inset(size * 0.05f, 0f)
                "shoulders" -> torso.left -= size * 0.03f
            }
            canvas.drawRoundRect(torso, 18f, 18f, body)
        }

        if (item.kind == CarouselKind.Outfit) {
            val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = size * 0.14f
            }
            canvas.drawText("Soon", cx, cy, t)
        }
    }

    private fun faceOverlay(canvas: Canvas, size: Int, lips: Int, glasses: Boolean) {
        if (!glasses) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEE111111.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val y = size * 0.46f
        canvas.drawCircle(size * 0.36f, y, size * 0.11f, p)
        canvas.drawCircle(size * 0.64f, y, size * 0.11f, p)
        canvas.drawLine(size * 0.47f, y, size * 0.53f, y, p)
        val lip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lips }
        canvas.drawOval(size * 0.38f, size * 0.68f, size * 0.62f, size * 0.78f, lip)
    }

    private fun lensColor(id: String): Int = when (id) {
        "PureTone" -> 0xFF8B5A3C.toInt()
        "GoldenGlint" -> 0xFFC9A227.toInt()
        "SapphireInk" -> 0xFF1E4D8C.toInt()
        "WarmGlint" -> 0xFFB86B3A.toInt()
        "KiwiPop" -> 0xFF5A9A2A.toInt()
        "SilverMist" -> 0xFF8A9AAA.toInt()
        "AmberGlow" -> 0xFFD07A20.toInt()
        "BlueDew" -> 0xFF3A8EC8.toInt()
        else -> 0xFF3A6A9A.toInt()
    }

    private fun hairStyleColor(id: String): Int {
        val h = id.hashCode()
        return rgb(0.12f + (h and 7) / 40f, 0.07f, 0.04f)
    }

    private fun rgbColor(rgb: FloatArray): Int = rgb(rgb[0], rgb[1], rgb[2])

    private fun rgb(r: Float, g: Float, b: Float): Int {
        val ri = (r.coerceIn(0f, 1f) * 255f).toInt()
        val gi = (g.coerceIn(0f, 1f) * 255f).toInt()
        val bi = (b.coerceIn(0f, 1f) * 255f).toInt()
        return Color.rgb(ri, gi, bi)
    }

    private fun bit(seed: Int, i: Int): Float = if ((seed shr i) and 1 == 1) 1f else 0f
}

internal enum class CarouselKind {
    Beauty, Face, Eyes, ContactLens, Nose, Mouth, Teeth, Body,
    MakeupPreset, MakeupElement, Filter, Ar, Gift, HairStyle, HairColor,
    FullLook, Background, Outfit,
}

internal data class CarouselItem(
    val id: String,
    val title: String,
    val kind: CarouselKind,
    val payload: String,
    val defaultStrength: Float = 0.5f,
    val signed: Boolean = false,
    val intensityEnabled: Boolean = true,
)
