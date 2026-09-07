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
        when (def.look) {
            0 -> { // Desert Sun
                halo(out, tr, 0.16f * bounce, p.roll, 1f, 0.72f, 0.28f, def.hairOcclude)
                cheekPaintHint(out, tr)
            }
            1, 2 -> { // cats
                val dark = def.look == 2
                val r = if (dark) 0.08f else 0.82f
                val g = if (dark) 0.07f else 0.55f
                val b = if (dark) 0.09f else 0.32f
                ear(out, tr, left = true, s = earScale, r, g, b, def.hairOcclude)
                ear(out, tr, left = false, s = earScale, r, g, b, def.hairOcclude)
                whiskers(out, tr, if (dark) 0.05f else 0.15f)
            }
            3, 5 -> { // glasses
                glasses(out, tr, bounce, if (def.look == 3) 0.15f else 0.08f, def.hairOcclude)
            }
            6 -> {
                halo(out, tr, 0.09f, p.roll, 0.85f, 0.12f, 0.1f, def.hairOcclude)
            }
            8 -> {
                halo(out, tr, 0.13f * bounce, p.roll, 1f, 0.45f, 0.55f, def.hairOcclude)
            }
            9 -> {
                moon(out, tr, 0.12f, p.roll, def.hairOcclude)
            }
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

    private fun cheekPaintHint(out: ArrayList<ARSprite>, tr: ARTrackingContext) {
        // zero-size marker unused; paint is in the compose shader
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
