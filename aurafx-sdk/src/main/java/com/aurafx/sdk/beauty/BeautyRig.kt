package com.aurafx.sdk.beauty

import com.aurafx.sdk.api.BeautyParameters
import com.aurafx.sdk.api.FaceShapeParameters
import com.aurafx.sdk.api.SkinParameters

/**
 * Thread-safe rig. GPU thread reads [snapshot]; API thread writes via copy/apply.
 * Shared fields (whiten, vFace, …) live once so beauty/skin/shape cannot diverge.
 */
class BeautyRig {
    private val lock = Any()
    private val skin = SkinParameters()
    private val shape = FaceShapeParameters()
    private val beauty = BeautyParameters()

    fun applySkin(block: SkinParameters.() -> Unit) {
        synchronized(lock) {
            block(skin)
            skin.clampInPlace()
            syncFromSkin()
        }
    }

    fun applyBeauty(block: BeautyParameters.() -> Unit) {
        synchronized(lock) {
            block(beauty)
            beauty.clampInPlace()
            syncFromBeauty()
        }
    }

    fun applyFaceShape(block: FaceShapeParameters.() -> Unit) {
        synchronized(lock) {
            block(shape)
            shape.clampInPlace()
            syncFromShape()
        }
    }

    fun reset() {
        synchronized(lock) {
            skin.reset()
            shape.reset()
            beauty.reset()
            skin.naturalSkin = 0.4f
            skin.texturePreserve = 0.7f
            skin.tone = 0.5f
        }
    }

    fun copySkin(): SkinParameters = synchronized(lock) { skin.copy() }

    fun copyBeauty(): BeautyParameters = synchronized(lock) { beauty.copy() }

    fun copyShape(): FaceShapeParameters = synchronized(lock) { shape.copy() }

    fun snapshot(): BeautySnapshot = synchronized(lock) {
        BeautySnapshot(
            fineSmooth = maxOf(skin.fineSmooth, beauty.fineSmooth),
            smoothness = skin.smoothness,
            texturePreserve = skin.texturePreserve,
            blemishReduction = skin.blemishReduction,
            evenness = skin.evenness,
            brightness = skin.brightness,
            whiten = maxOf(skin.whiten, beauty.whiten),
            ruddy = maxOf(skin.ruddy, beauty.ruddy),
            tone = skin.tone,
            naturalSkin = skin.naturalSkin,
            toothWhiten = beauty.toothWhiten,
            circles = beauty.circles,
            vFace = maxOf(shape.vFace, beauty.vFace),
            cheekThin = maxOf(shape.cheekThin, beauty.cheekThin),
            cheekSmall = maxOf(shape.cheekSmall, beauty.cheekSmall),
            cheekNarrow = maxOf(shape.cheekNarrow, beauty.cheekNarrow),
            nose = maxOf(shape.nose, beauty.nose),
            eyeEnlarge = maxOf(shape.eyeEnlarge, beauty.eyeEnlarge),
            eyeDistance = if (kotlin.math.abs(shape.eyeDistance) >= kotlin.math.abs(beauty.eyeDistance)) {
                shape.eyeDistance
            } else {
                beauty.eyeDistance
            },
            mouth = maxOf(shape.mouth, beauty.mouth),
        )
    }

    private fun syncFromSkin() {
        beauty.fineSmooth = skin.fineSmooth
        beauty.whiten = skin.whiten
        beauty.ruddy = skin.ruddy
    }

    private fun syncFromBeauty() {
        skin.fineSmooth = beauty.fineSmooth
        skin.whiten = beauty.whiten
        skin.ruddy = beauty.ruddy
        shape.vFace = beauty.vFace
        shape.cheekThin = beauty.cheekThin
        shape.cheekSmall = beauty.cheekSmall
        shape.cheekNarrow = beauty.cheekNarrow
        shape.nose = beauty.nose
        shape.eyeEnlarge = beauty.eyeEnlarge
        shape.eyeDistance = beauty.eyeDistance
        shape.mouth = beauty.mouth
    }

    private fun syncFromShape() {
        beauty.vFace = shape.vFace
        beauty.cheekThin = shape.cheekThin
        beauty.cheekSmall = shape.cheekSmall
        beauty.cheekNarrow = shape.cheekNarrow
        beauty.nose = shape.nose
        beauty.eyeEnlarge = shape.eyeEnlarge
        beauty.eyeDistance = shape.eyeDistance
        beauty.mouth = shape.mouth
    }
}

data class BeautySnapshot(
    val fineSmooth: Float,
    val smoothness: Float,
    val texturePreserve: Float,
    val blemishReduction: Float,
    val evenness: Float,
    val brightness: Float,
    val whiten: Float,
    val ruddy: Float,
    val tone: Float,
    val naturalSkin: Float,
    val toothWhiten: Float,
    val circles: Float,
    val vFace: Float,
    val cheekThin: Float,
    val cheekSmall: Float,
    val cheekNarrow: Float,
    val nose: Float,
    val eyeEnlarge: Float,
    val eyeDistance: Float,
    val mouth: Float,
) {
    fun skinIdentity(): Boolean =
        fineSmooth == 0f && smoothness == 0f && blemishReduction == 0f &&
            evenness == 0f && brightness == 0f && whiten == 0f && ruddy == 0f &&
            tone == 0.5f && toothWhiten == 0f && circles == 0f

    fun shapeIdentity(): Boolean =
        vFace == 0f && cheekThin == 0f && cheekSmall == 0f && cheekNarrow == 0f &&
            nose == 0f && eyeEnlarge == 0f && eyeDistance == 0f && mouth == 0f

    fun isIdentity(): Boolean = skinIdentity() && shapeIdentity()
}
