package com.aurafx.sdk.makeup

import com.aurafx.sdk.api.BlushStyle
import com.aurafx.sdk.api.BrowStyle
import com.aurafx.sdk.api.EyelinerStyle
import com.aurafx.sdk.api.LashStyle
import com.aurafx.sdk.api.MakeupParameters
import com.aurafx.sdk.beauty.FaceLandmarks
import com.aurafx.sdk.beauty.FaceTopology
import com.aurafx.sdk.beauty.LandmarkSmoother
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Stamp(
    val x: Float,
    val y: Float,
    val rx: Float,
    val ry: Float,
    val kind: Int,
) {
    fun finite(): Boolean = x.isFinite() && y.isFinite() && rx.isFinite() && ry.isFinite() &&
        rx > 0f && ry > 0f && x in 0f..1f && y in 0f..1f
}

data class Polyline(val points: List<Pair<Float, Float>>, val widthStart: Float, val widthEnd: Float)
data class Strand(
    val ax: Float, val ay: Float,
    val bx: Float, val by: Float,
    val cx: Float, val cy: Float,
    val widthRoot: Float, val widthTip: Float,
)

data class MakeupGeometry(
    val stamps: List<Stamp>,
    val liners: List<Polyline>,
    val lashes: List<Strand>,
    val leftIris: FloatArray,
    val rightIris: FloatArray,
) {
    fun allFinite(): Boolean =
        stamps.all { it.finite() } &&
            liners.all { it.points.all { p -> p.first.isFinite() && p.second.isFinite() } } &&
            lashes.all { it.ax.isFinite() && it.cx.isFinite() } &&
            leftIris.all { it.isFinite() } && rightIris.all { it.isFinite() }
}

object MakeupGeometryBuilder {
    const val KIND_BLUSH = 0
    const val KIND_CONTOUR = 1
    const val KIND_HIGHLIGHT = 2
    const val KIND_CONCEALER = 3

    fun build(lm: FaceLandmarks, params: MakeupParameters): MakeupGeometry {
        val scale = LandmarkSmoother.interOcular(lm)
        val stamps = ArrayList<Stamp>(16)
        if (params.blush.intensity > 0f) {
            val (sx, sy) = blushScale(params.blush.style, scale)
            stamps += cheekStamp(lm, left = true, sx, sy, KIND_BLUSH)
            stamps += cheekStamp(lm, left = false, sx, sy, KIND_BLUSH)
        }
        if (params.contour.intensity > 0f) {
            stamps += Stamp(lm.x(FaceTopology.LEFT_JAW), lm.y(FaceTopology.LEFT_JAW), scale * 0.28f, scale * 0.42f, KIND_CONTOUR)
            stamps += Stamp(lm.x(FaceTopology.RIGHT_JAW), lm.y(FaceTopology.RIGHT_JAW), scale * 0.28f, scale * 0.42f, KIND_CONTOUR)
            if (lm.has(50)) stamps += Stamp(lm.x(50), lm.y(50) + scale * 0.08f, scale * 0.22f, scale * 0.18f, KIND_CONTOUR)
            if (lm.has(280)) stamps += Stamp(lm.x(280), lm.y(280) + scale * 0.08f, scale * 0.22f, scale * 0.18f, KIND_CONTOUR)
            if (lm.has(FaceTopology.LEFT_ALA)) {
                stamps += Stamp(lm.x(FaceTopology.LEFT_ALA), lm.y(FaceTopology.LEFT_ALA), scale * 0.08f, scale * 0.16f, KIND_CONTOUR)
                stamps += Stamp(lm.x(FaceTopology.RIGHT_ALA), lm.y(FaceTopology.RIGHT_ALA), scale * 0.08f, scale * 0.16f, KIND_CONTOUR)
            }
            stamps += Stamp(lm.x(FaceTopology.FOREHEAD), lm.y(FaceTopology.FOREHEAD) + scale * 0.05f, scale * 0.4f, scale * 0.12f, KIND_CONTOUR)
        }
        if (params.highlight.intensity > 0f) {
            if (lm.has(50)) stamps += Stamp(lm.x(50), lm.y(50) - scale * 0.06f, scale * 0.16f, scale * 0.08f, KIND_HIGHLIGHT)
            if (lm.has(280)) stamps += Stamp(lm.x(280), lm.y(280) - scale * 0.06f, scale * 0.16f, scale * 0.08f, KIND_HIGHLIGHT)
            stamps += Stamp(lm.x(6), lm.y(6), scale * 0.06f, scale * 0.18f, KIND_HIGHLIGHT)
            stamps += Stamp(lm.x(FaceTopology.FOREHEAD), lm.y(FaceTopology.FOREHEAD) + scale * 0.12f, scale * 0.18f, scale * 0.08f, KIND_HIGHLIGHT)
            stamps += Stamp(lm.x(0), lm.y(0) - scale * 0.015f, scale * 0.06f, scale * 0.03f, KIND_HIGHLIGHT)
        }
        if (params.concealer.intensity > 0f) {
            underEye(lm, true, scale)?.let { stamps += it }
            underEye(lm, false, scale)?.let { stamps += it }
        }
        val liners = if (params.eyeliner.intensity > 0f) linerPolylines(lm, params.eyeliner.style, scale) else emptyList()
        val lashes = if (params.eyelashes.intensity > 0f) lashStrands(lm, params.eyelashes.style, scale) else emptyList()
        return MakeupGeometry(
            stamps = stamps.filter { it.finite() },
            liners = liners,
            lashes = lashes,
            leftIris = iris(lm, true, scale),
            rightIris = iris(lm, false, scale),
        )
    }

    fun browWidth(style: BrowStyle, scale: Float): Float = scale * when (style) {
        BrowStyle.BoldArch, BrowStyle.FullDefinition -> 0.055f
        BrowStyle.Feathered, BrowStyle.Natural -> 0.038f
        BrowStyle.Flat, BrowStyle.SoftCurve -> 0.034f
        BrowStyle.Angled -> 0.042f
    }

    private fun blushScale(style: BlushStyle, scale: Float): Pair<Float, Float> = when (style) {
        BlushStyle.SoftTouch -> scale * 0.28f to scale * 0.18f
        BlushStyle.Airbrush -> scale * 0.34f to scale * 0.22f
        BlushStyle.BlushBomb -> scale * 0.22f to scale * 0.22f
        BlushStyle.SunKissed -> scale * 0.40f to scale * 0.16f
    }

    private fun cheekStamp(lm: FaceLandmarks, left: Boolean, sx: Float, sy: Float, kind: Int): Stamp {
        val idx = if (left) 50 else 280
        val x = if (lm.has(idx)) lm.x(idx) else if (left) 0.35f else 0.65f
        val y = if (lm.has(idx)) lm.y(idx) else 0.52f
        return Stamp(x, y, sx, sy, kind)
    }

    private fun underEye(lm: FaceLandmarks, left: Boolean, scale: Float): Stamp? {
        val lower = if (left) FaceTopology.LEFT_EYE else FaceTopology.RIGHT_EYE
        if (lower.size < 4) return null
        val i0 = lower[3]
        val i1 = lower[4]
        if (!lm.has(i0) || !lm.has(i1)) return null
        return Stamp((lm.x(i0) + lm.x(i1)) * 0.5f, (lm.y(i0) + lm.y(i1)) * 0.5f + scale * 0.04f, scale * 0.16f, scale * 0.08f, KIND_CONCEALER)
    }

    private fun iris(lm: FaceLandmarks, left: Boolean, scale: Float): FloatArray {
        val centerIdx = if (left) 468 else 473
        val cx: Float
        val cy: Float
        if (lm.has(centerIdx)) {
            cx = lm.x(centerIdx)
            cy = lm.y(centerIdx)
        } else {
            val inner = if (left) FaceTopology.LEFT_EYE_INNER else FaceTopology.RIGHT_EYE_INNER
            val outer = if (left) FaceTopology.LEFT_EYE_OUTER else FaceTopology.RIGHT_EYE_OUTER
            cx = (lm.x(inner) + lm.x(outer)) * 0.5f
            cy = (lm.y(inner) + lm.y(outer)) * 0.5f
        }
        val r = scale * 0.13f
        return floatArrayOf(cx.coerceIn(0f, 1f), cy.coerceIn(0f, 1f), r, r * 0.38f)
    }

    private fun upperLashLine(lm: FaceLandmarks, left: Boolean): List<Pair<Float, Float>> {
        val idx = if (left) {
            intArrayOf(33, 246, 161, 160, 159, 158, 157, 173, 133)
        } else {
            intArrayOf(263, 466, 388, 387, 386, 385, 384, 398, 362)
        }
        return idx.filter { lm.has(it) }.map { lm.x(it) to lm.y(it) }
    }

    private fun linerPolylines(lm: FaceLandmarks, style: EyelinerStyle, scale: Float): List<Polyline> {
        if (style == EyelinerStyle.None) return emptyList()
        val left = upperLashLine(lm, true)
        val right = upperLashLine(lm, false)
        val thick = when (style) {
            EyelinerStyle.Bold, EyelinerStyle.Glam, EyelinerStyle.Smokey -> scale * 0.028f
            EyelinerStyle.Graphic, EyelinerStyle.CatEye, EyelinerStyle.Winged -> scale * 0.022f
            else -> scale * 0.016f
        }
        val wing = when (style) {
            EyelinerStyle.CatEye, EyelinerStyle.Winged, EyelinerStyle.Flick, EyelinerStyle.Goldie -> scale * 0.12f
            EyelinerStyle.Glam, EyelinerStyle.Retro -> scale * 0.08f
            EyelinerStyle.Graphic -> scale * 0.1f
            else -> 0f
        }
        fun withWing(pts: List<Pair<Float, Float>>, outward: Float): List<Pair<Float, Float>> {
            if (pts.size < 2 || wing == 0f) return pts
            val (x0, y0) = pts.first()
            val (x1, y1) = pts[1]
            val ang = atan2(y0 - y1, x0 - x1)
            val wx = x0 + cos(ang) * wing * outward
            val wy = y0 + sin(ang) * wing * 0.35f - wing * 0.25f
            return listOf(wx.coerceIn(0f, 1f) to wy.coerceIn(0f, 1f)) + pts
        }
        val out = ArrayList<Polyline>(4)
        if (left.size >= 2) out += Polyline(withWing(left, -1f), thick, thick * 0.35f)
        if (right.size >= 2) out += Polyline(withWing(right, 1f), thick, thick * 0.35f)
        if (style == EyelinerStyle.Retro || style == EyelinerStyle.Smokey) {
            val ll = FaceTopology.LEFT_EYE.filter { lm.has(it) }.map { lm.x(it) to lm.y(it) }.take(6)
            val rl = FaceTopology.RIGHT_EYE.filter { lm.has(it) }.map { lm.x(it) to lm.y(it) }.take(6)
            if (ll.size >= 2) out += Polyline(ll, thick * 0.45f, thick * 0.2f)
            if (rl.size >= 2) out += Polyline(rl, thick * 0.45f, thick * 0.2f)
        }
        return out
    }

    private fun lashStrands(lm: FaceLandmarks, style: LashStyle, scale: Float): List<Strand> {
        val count = when (style) {
            LashStyle.FullFan, LashStyle.SoftVolume -> 14
            LashStyle.DollEyes, LashStyle.Lifted -> 12
            else -> 9
        }
        val len = scale * when (style) {
            LashStyle.DollEyes, LashStyle.FullFan -> 0.09f
            LashStyle.Lifted -> 0.08f
            LashStyle.SoftVolume -> 0.07f
            else -> 0.055f
        }
        val curl = when (style) {
            LashStyle.NaturalCurl, LashStyle.Lifted, LashStyle.DollEyes -> 1.15f
            else -> 0.75f
        }
        val strands = ArrayList<Strand>(count * 2)
        fun addEye(line: List<Pair<Float, Float>>, outerSign: Float) {
            if (line.size < 3) return
            for (i in 0 until count) {
                val t = i / (count - 1).toFloat()
                val idx = (t * (line.size - 1)).toInt().coerceIn(0, line.size - 2)
                val (x0, y0) = line[idx]
                val (x1, y1) = line[idx + 1]
                val x = x0 + (x1 - x0) * (t * (line.size - 1) - idx)
                val y = y0 + (y1 - y0) * (t * (line.size - 1) - idx)
                val ang = atan2(y0 - y1, x0 - x1) + (if (outerSign < 0) -1.2f else 1.2f) + (0.5f - t) * 0.4f
                val lift = len * (0.65f + 0.55f * t) * curl
                val mx = x + cos(ang - 1.2f * outerSign) * lift * 0.45f
                val my = y - lift * 0.85f
                val tx = x + cos(ang - 1.4f * outerSign) * lift
                val ty = y - lift
                val root = scale * 0.007f
                val tip = scale * 0.0015f
                if (listOf(x, y, mx, my, tx, ty).all { it.isFinite() }) {
                    strands += Strand(x, y, mx, my, tx.coerceIn(0f, 1f), ty.coerceIn(0f, 1f), root, tip)
                }
            }
        }
        addEye(upperLashLine(lm, true), -1f)
        addEye(upperLashLine(lm, false), 1f)
        return strands
    }

    fun polylineLength(line: Polyline): Float {
        var s = 0f
        for (i in 1 until line.points.size) {
            s += hypot(
                line.points[i].first - line.points[i - 1].first,
                line.points[i].second - line.points[i - 1].second,
            )
        }
        return s
    }
}
