package com.aurafx.sdk.ar

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.effect.Effect
import com.aurafx.sdk.effect.EffectContext
import com.aurafx.sdk.effect.FrameContext
import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.internal.render.GlFramebuffer
import com.aurafx.sdk.internal.render.ShaderProgram
import com.aurafx.sdk.internal.render.checkGl
import com.aurafx.sdk.scene.AuraFxEffectOrder
import com.aurafx.sdk.vision.TrackingData
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ARPipelineEffect(
    private val rig: ARRig,
) : Effect {
    override val id: String = AuraFxEffectOrder.AR

    private val assets = ARAssetManager()
    private val anim = ARAnimationEngine()
    val renderContext = ARRenderContext()

    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private var compose: ShaderProgram? = null
    private var sprite: ShaderProgram? = null
    private val resolved = GlFramebuffer()
    private val painted = GlFramebuffer()
    private var quadVao = 0
    private var quadVbo = 0
    private var sprVao = 0
    private var sprVbo = 0
    private var lastW = 0
    private var lastH = 0
    private var attached = false
    private var lastEffect: String? = null
    private var fallbackMask = 0

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(ARShaders.VERT_RESOLVE, ARShaders.FRAG_OES)
        copy2d = ShaderProgram(ARShaders.VERT_BLIT, ARShaders.FRAG_COPY)
        compose = ShaderProgram(ARShaders.VERT_BLIT, ARShaders.FRAG_COMPOSE)
        sprite = ShaderProgram(ARShaders.VERT_SPRITE, ARShaders.FRAG_SPRITE)
        val qv = floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
        val qBuf = ByteBuffer.allocateDirect(qv.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(qv)
        qBuf.position(0)
        val va = IntArray(2)
        val vb = IntArray(2)
        GLES30.glGenVertexArrays(2, va, 0)
        GLES30.glGenBuffers(2, vb, 0)
        quadVao = va[0]
        sprVao = va[1]
        quadVbo = vb[0]
        sprVbo = vb[1]
        GLES30.glBindVertexArray(quadVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, qv.size * 4, qBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        val sv = floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
        val sBuf = ByteBuffer.allocateDirect(sv.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(sv)
        sBuf.position(0)
        GLES30.glBindVertexArray(sprVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, sprVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, sv.size * 4, sBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glBindVertexArray(0)
        val ft = IntArray(1)
        GLES30.glGenTextures(1, ft, 0)
        fallbackMask = ft[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fallbackMask)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        val px = ByteBuffer.allocateDirect(4).put(byteArrayOf(0, 0, 0, 0)).rewind()
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, px)
        attached = true
        AuraFxLog.i("ARPipelineEffect attached")
        checkGl("ar attach")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached) return
        val snap = rig.snapshot()
        if (snap.isIdentity()) return
        val def = snap.effectId?.let { AREffectCatalog.require(it) } ?: return
        val tr = ARTrackingContext.from(tracking.landmarks) ?: return
        if (!tr.allFinite()) return
        if (lastEffect != def.id) {
            anim.reset()
            lastEffect = def.id
        }
        val origin = when (def.animation.expressionBoost) {
            "smile", "mouthOpen" -> tr.mouth
            else -> tr.forehead
        }
        val boost = when (def.animation.expressionBoost) {
            "smile" -> tr.expression.smile
            "mouthOpen" -> tr.expression.mouthOpen
            "browRaise" -> tr.expression.browRaise
            else -> 0.35f
        }
        anim.step(
            tracking.frameTimestampNs,
            def.animation.emitPerSec * snap.intensity,
            origin[0],
            origin[1],
            tr.pose.yaw,
            boost,
            def.animation.particleKind,
        )
        ensure(frame.width, frame.height)
        resolveOrCopy(frame)
        paint(frame, def, tr, snap.intensity)
        val sprites = assets.spritesFor(def, tr, anim, snap.intensity)
        renderContext.lastSpriteCount = sprites.size
        drawSprites(frame, sprites, snap.intensity)
        frame.processedTextureId = painted.tex
        frame.processedIsOes = false
    }

    override fun onDetach() {
        attached = false
        anim.reset()
        resolve?.release(); copy2d?.release(); compose?.release(); sprite?.release()
        resolve = null; copy2d = null; compose = null; sprite = null
        resolved.release(); painted.release()
        if (quadVao != 0) GLES30.glDeleteVertexArrays(2, intArrayOf(quadVao, sprVao), 0)
        if (quadVbo != 0) GLES30.glDeleteBuffers(2, intArrayOf(quadVbo, sprVbo), 0)
        if (fallbackMask != 0) GLES30.glDeleteTextures(1, intArrayOf(fallbackMask), 0)
        fallbackMask = 0
        quadVao = 0; sprVao = 0; quadVbo = 0; sprVbo = 0
    }

    private fun ensure(w: Int, h: Int) {
        if (w == lastW && h == lastH && resolved.tex != 0) return
        lastW = w; lastH = h
        resolved.ensure(w, h); painted.ensure(w, h)
    }

    private fun resolveOrCopy(frame: FrameContext) {
        val incoming = frame.processedTextureId
        if (incoming != 0 && !frame.processedIsOes) {
            val program = copy2d ?: return
            resolved.bind()
            program.use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, incoming)
            GLES30.glUniform1i(program.loc("uTexture"), 0)
            GLES30.glBindVertexArray(quadVao)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        } else {
            val program = resolve ?: return
            resolved.bind()
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            program.use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, frame.inputTextureId)
            GLES30.glUniform1i(program.loc("uTexture"), 0)
            GLES30.glUniformMatrix4fv(program.loc("uTexMatrix"), 1, false, frame.texMatrix, 0)
            val mirror = if (frame.lensFacing == LensFacing.FRONT) -1f else 1f
            GLES30.glUniform1f(program.loc("uMirror"), mirror)
            GLES30.glBindVertexArray(quadVao)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }
    }

    private fun paint(frame: FrameContext, def: AREffectDefinition, tr: ARTrackingContext, intensity: Float) {
        val program = compose ?: return
        painted.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, resolved.tex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex(frame))
        GLES30.glUniform1i(program.loc("uMask"), 1)
        GLES30.glUniform1i(program.loc("uLook"), def.look)
        GLES30.glUniform1i(program.loc("uFamily"), def.family)
        GLES30.glUniform3f(program.loc("uPaint"), def.paint[0], def.paint[1], def.paint[2])
        GLES30.glUniform1f(program.loc("uIntensity"), intensity)
        GLES30.glUniform1f(program.loc("uTime"), anim.timeSec)
        GLES30.glUniform1f(program.loc("uSmile"), tr.expression.smile)
        GLES30.glUniform1f(program.loc("uMouth"), tr.expression.mouthOpen)
        GLES30.glUniform1f(program.loc("uBlinkL"), tr.expression.blinkLeft)
        GLES30.glUniform1f(program.loc("uBlinkR"), tr.expression.blinkRight)
        GLES30.glUniform1f(program.loc("uBrow"), tr.expression.browRaise)
        GLES30.glUniform2f(program.loc("uEyeL"), tr.leftEye[0], tr.leftEye[1])
        GLES30.glUniform2f(program.loc("uEyeR"), tr.rightEye[0], tr.rightEye[1])
        GLES30.glUniform2f(program.loc("uIrisL"), tr.leftIris[0], tr.leftIris[1])
        GLES30.glUniform2f(program.loc("uIrisR"), tr.rightIris[0], tr.rightIris[1])
        GLES30.glUniform2f(program.loc("uCheekL"), tr.leftCheek[0], tr.leftCheek[1])
        GLES30.glUniform2f(program.loc("uCheekR"), tr.rightCheek[0], tr.rightCheek[1])
        GLES30.glUniform2f(program.loc("uForehead"), tr.forehead[0], tr.forehead[1])
        GLES30.glUniform2f(program.loc("uMouthP"), tr.mouth[0], tr.mouth[1])
        GLES30.glUniform1f(program.loc("uIod"), tr.pose.iod)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        checkGl("ar paint")
    }

    private fun drawSprites(frame: FrameContext, sprites: List<ARSprite>, intensity: Float) {
        val program = sprite ?: return
        if (sprites.isEmpty()) return
        painted.bind()
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex(frame))
        GLES30.glUniform1i(program.loc("uMask"), 0)
        GLES30.glBindVertexArray(sprVao)
        for (s in sprites) {
            if (!s.uvx.isFinite() || !s.uvy.isFinite()) continue
            GLES30.glUniform2f(program.loc("uCenter"), s.uvx, s.uvy)
            GLES30.glUniform2f(program.loc("uSize"), s.rx, s.ry)
            GLES30.glUniform1f(program.loc("uAngle"), s.angle)
            GLES30.glUniform3f(program.loc("uColor"), s.cr, s.cg, s.cb)
            GLES30.glUniform1i(program.loc("uKind"), s.kind)
            GLES30.glUniform1f(program.loc("uOcc"), s.occ)
            GLES30.glUniform1f(program.loc("uAlpha"), intensity)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        checkGl("ar sprites")
    }

    private fun maskTex(frame: FrameContext): Int =
        if (frame.segmentationTextureId != 0) frame.segmentationTextureId else fallbackMask
}
