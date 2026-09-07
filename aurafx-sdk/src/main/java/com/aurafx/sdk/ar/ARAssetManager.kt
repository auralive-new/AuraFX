package com.aurafx.sdk.ar

import kotlin.math.cos
import kotlin.math.sin

enum class ARSpriteKind {
    Ear, Glasses, Halo, Moon, Whisker, Heart, Star, Confetti, Sparkle, Dust,
}

data class ARSprite(
    val uvx: Float,
    val uvy: Float,
    val rx: Float,
    val ry: Float,
    val angle: Float,
    val kind: Int,
    val cr: Float,
    val cg: Float,
    val cb: Float,
    val occ: Float,
)

/**
 * Procedural GPU assets (meshes/sprites), not PNG stickers.
 * Templates are generated once; per-frame instances are transformed from landmarks.
 */
class ARAssetManager {
    fun spritesFor(def: AREffectDefinition, tr: ARTrackingContext, anim: ARAnimationEngine, intensity: Float): ArrayList<ARSprite> {
        val out = ArrayList<ARSprite>(24)
        val p = tr.pose
        val bounce = if (def.animation.scalePulse) anim.bounce(tr.expression.smile) else 1f
        val earScale = (0.11f + 0.04f * tr.expression.browRaise) * bounce * p.iod * 2.4f
        val r = def.paint[0]
        val g = def.paint[1]
        val b = def.paint[2]
        when (def.family) {
            0 -> halo(out, tr, 0.16f * bounce, p.roll, r, g, b, def.hairOcclude)
            1 -> {
                ear(out, tr, left = true, s = earScale, r, g, b, def.hairOcclude)
                ear(out, tr, left = false, s = earScale, r, g, b, def.hairOcclude)
                whiskers(out, tr, (r + g + b) / 6f)
            }
            2 -> glasses(out, tr, bounce, r * 0.35f, def.hairOcclude)
            3 -> { /* face paint is compose-shader */ }
            4 -> moon(out, tr, 0.12f * bounce, p.roll, def.hairOcclude)
            5 -> earrings(out, tr, r, g, b, def.hairOcclude)
            6 -> {
                ear(out, tr, left = true, s = earScale * 0.85f, r, g, b, def.hairOcclude)
                ear(out, tr, left = false, s = earScale * 0.85f, r, g, b, def.hairOcclude)
            }
            7 -> halo(out, tr, 0.11f * bounce, p.roll, r, g, b, def.hairOcclude)
            8 -> halo(out, tr, 0.14f * bounce, p.roll, r, g, b, def.hairOcclude)
            9 -> irisGlow(out, tr, r, g, b)
        }
        appendParticles(out, anim, intensity)
        return out
    }

    fun requiredAssetsPresent(def: AREffectDefinition): Boolean =
        def.assets.isNotEmpty() && def.assets.all { it.procedural && it.valid() }

    private fun ear(out: ArrayList<ARSprite>, tr: ARTrackingContext, left: Boolean, s: Float, r: Float, g: Float, b: Float, occ: Float) {
        val sign = if (left) -1f else 1f
        val base = rotate(tr.forehead[0], tr.forehead[1], sign * tr.pose.iod * 0.42f, -tr.pose.iod * 0.55f, tr.pose.roll, tr.pose.yaw * 0.04f)
        out.add(ARSprite(base[0], base[1], s, s * 1.25f, tr.pose.roll + sign * 0.35f, ARSpriteKind.Ear.ordinal, r, g, b, occ))
    }

    private fun glasses(out: ArrayList<ARSprite>, tr: ARTrackingContext, bounce: Float, tint: Float, occ: Float) {
        val midX = (tr.leftEye[0] + tr.rightEye[0]) * 0.5f
        val midY = (tr.leftEye[1] + tr.rightEye[1]) * 0.5f
        val w = tr.pose.iod * 1.15f * bounce
        out.add(ARSprite(midX, midY, w, w * 0.28f, tr.pose.roll, ARSpriteKind.Glasses.ordinal, tint, tint, tint + 0.05f, occ))
    }

    private fun halo(out: ArrayList<ARSprite>, tr: ARTrackingContext, rad: Float, roll: Float, r: Float, g: Float, b: Float, occ: Float) {
        val c = rotate(tr.forehead[0], tr.forehead[1], 0f, -tr.pose.iod * 0.85f, roll, 0f)
        out.add(ARSprite(c[0], c[1], rad, rad * 0.45f, roll, ARSpriteKind.Halo.ordinal, r, g, b, occ))
    }

    private fun moon(out: ArrayList<ARSprite>, tr: ARTrackingContext, rad: Float, roll: Float, occ: Float) {
        val c = rotate(tr.forehead[0], tr.forehead[1], tr.pose.iod * 0.15f, -tr.pose.iod * 0.9f, roll, 0f)
        out.add(ARSprite(c[0], c[1], rad, rad, roll, ARSpriteKind.Moon.ordinal, 0.82f, 0.86f, 0.95f, occ))
    }

    private fun whiskers(out: ArrayList<ARSprite>, tr: ARTrackingContext, gray: Float) {
        for (side in floatArrayOf(-1f, 1f)) {
            val cheek = if (side < 0f) tr.leftCheek else tr.rightCheek
            for (k in 0..2) {
                val y = cheek[1] + (k - 1) * tr.pose.iod * 0.08f
                out.add(
                    ARSprite(
                        cheek[0] + side * tr.pose.iod * 0.18f, y,
                        tr.pose.iod * 0.22f, tr.pose.iod * 0.018f,
                        tr.pose.roll + side * 0.08f * k,
                        ARSpriteKind.Whisker.ordinal, gray, gray, gray, 0.1f,
                    ),
                )
            }
        }
    }

    private fun earrings(out: ArrayList<ARSprite>, tr: ARTrackingContext, r: Float, g: Float, b: Float, occ: Float) {
        val s = tr.pose.iod * 0.08f
        val drop = tr.pose.iod * 0.55f
        val left = rotate(tr.leftCheek[0], tr.leftCheek[1], -tr.pose.iod * 0.15f, drop, tr.pose.roll, tr.pose.yaw * 0.03f)
        val right = rotate(tr.rightCheek[0], tr.rightCheek[1], tr.pose.iod * 0.15f, drop, tr.pose.roll, tr.pose.yaw * 0.03f)
        out.add(ARSprite(left[0], left[1], s, s * 1.6f, tr.pose.roll, ARSpriteKind.Sparkle.ordinal, r, g, b, occ * 0.4f))
        out.add(ARSprite(right[0], right[1], s, s * 1.6f, tr.pose.roll, ARSpriteKind.Sparkle.ordinal, r, g, b, occ * 0.4f))
    }

    private fun irisGlow(out: ArrayList<ARSprite>, tr: ARTrackingContext, r: Float, g: Float, b: Float) {
        val s = tr.pose.iod * 0.16f * (1f - 0.7f * tr.expression.blinkLeft)
        val t = tr.pose.iod * 0.16f * (1f - 0.7f * tr.expression.blinkRight)
        out.add(ARSprite(tr.leftIris[0], tr.leftIris[1], s, s, 0f, ARSpriteKind.Sparkle.ordinal, r, g, b, 0.05f))
        out.add(ARSprite(tr.rightIris[0], tr.rightIris[1], t, t, 0f, ARSpriteKind.Sparkle.ordinal, r, g, b, 0.05f))
    }

    private fun appendParticles(out: ArrayList<ARSprite>, anim: ARAnimationEngine, intensity: Float) {
        if (intensity <= 0.01f) return
        var i = 0
        while (i < ARAnimationEngine.MAX_PARTICLES) {
            val o = i * ARAnimationEngine.STRIDE
            val life = anim.particles[o + 4]
            if (life > 0f) {
                val maxL = anim.particles[o + 5].coerceAtLeast(0.05f)
                val a = (life / maxL).coerceIn(0f, 1f)
                val kind = anim.particles[o + 7].toInt()
                val s = anim.particles[o + 6] * (0.5f + a)
                out.add(
                    ARSprite(
                        anim.particles[o], anim.particles[o + 1], s, s, 0f, kind,
                        1f, 0.85f, 0.9f, 0f,
                    ),
                )
            }
            i++
        }
    }

    private fun rotate(cx: Float, cy: Float, lx: Float, ly: Float, roll: Float, yawOff: Float): FloatArray {
        val c = cos(roll)
        val s = sin(roll)
        return floatArrayOf(cx + lx * c - ly * s + yawOff, cy + lx * s + ly * c)
    }
}

class ARRenderContext {
    var lastSpriteCount: Int = 0
}
