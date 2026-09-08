package com.aurafx.sdk.gift

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.aurafx.sdk.api.GiftFxLayer
import com.aurafx.sdk.api.GiftFxOcclusion
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

class GiftPipelineEffect(
    private val engine: GiftFxEngine,
) : Effect {
    override val id: String = AuraFxEffectOrder.GIFT

    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private var compose: ShaderProgram? = null
    private var sprite: ShaderProgram? = null
    private val resolved = GlFramebuffer()
    private val ping = GlFramebuffer()
    private val pong = GlFramebuffer()
    private val freeze = GlFramebuffer()
    private var quadVao = 0
    private var quadVbo = 0
    private var lastW = 0
    private var lastH = 0
    private var attached = false
    private var fallbackMask = 0

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(GiftFxShaders.VERT_RESOLVE, GiftFxShaders.FRAG_OES)
        copy2d = ShaderProgram(GiftFxShaders.VERT_BLIT, GiftFxShaders.FRAG_COPY)
        compose = ShaderProgram(GiftFxShaders.VERT_BLIT, GiftFxShaders.FRAG_COMPOSE)
        sprite = ShaderProgram(GiftFxShaders.VERT_SPRITE, GiftFxShaders.FRAG_SPRITE)
        val qv = floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
        val qBuf = ByteBuffer.allocateDirect(qv.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(qv)
        qBuf.position(0)
        val va = IntArray(1)
        val vb = IntArray(1)
        GLES30.glGenVertexArrays(1, va, 0)
        GLES30.glGenBuffers(1, vb, 0)
        quadVao = va[0]
        quadVbo = vb[0]
        GLES30.glBindVertexArray(quadVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, qv.size * 4, qBuf, GLES30.GL_STATIC_DRAW)
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
        engine.renderer.onAttach()
        AuraFxLog.i("GiftPipelineEffect attached")
        checkGl("gift attach")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached) return
        if (!engine.hasActive()) return
        val instances = engine.tick(frame.timestampNs, frame.width, frame.height, tracking)
        if (instances.isEmpty()) return
        ensure(frame.width, frame.height)
        resolveOrCopy(frame)
        maybeFreeze(instances)
        var srcTex = resolved.tex
        var dst = ping
        var usePing = true
        engine.renderer.lastDrawCount = instances.size
        for (inst in instances) {
            composeOne(frame, inst, srcTex, dst)
            drawParticles(frame, inst, dst)
            srcTex = dst.tex
            usePing = !usePing
            dst = if (usePing) ping else pong
        }
        frame.processedTextureId = srcTex
        frame.processedIsOes = false
    }

    override fun onDetach() {
        attached = false
        engine.renderer.dispose()
        resolve?.release(); copy2d?.release(); compose?.release(); sprite?.release()
        resolve = null; copy2d = null; compose = null; sprite = null
        resolved.release(); ping.release(); pong.release(); freeze.release()
        if (quadVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(quadVao), 0)
        if (quadVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadVbo), 0)
        if (fallbackMask != 0) GLES30.glDeleteTextures(1, intArrayOf(fallbackMask), 0)
        fallbackMask = 0
        quadVao = 0; quadVbo = 0
        lastW = 0; lastH = 0
    }

    private fun ensure(w: Int, h: Int) {
        if (w == lastW && h == lastH && resolved.tex != 0) return
        lastW = w; lastH = h
        resolved.ensure(w, h)
        ping.ensure(w, h)
        pong.ensure(w, h)
        freeze.ensure(w, h)
    }

    private fun maybeFreeze(instances: List<GiftFxInstance>) {
        for (inst in instances) {
            val def = inst.definition ?: continue
            if (def.shaderLook != 0) continue
            if (inst.freezeCaptured) continue
            if (inst.phaseIndex() > 2) continue
            blit(resolved.tex, freeze)
            inst.freezeCaptured = true
            engine.renderer.markFreezeCaptured()
        }
    }

    private fun blit(src: Int, dst: GlFramebuffer) {
        val program = copy2d ?: return
        dst.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, src)
        GLES30.glUniform1i(program.loc("uTexture"), 0)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun resolveOrCopy(frame: FrameContext) {
        val incoming = frame.processedTextureId
        if (incoming != 0 && !frame.processedIsOes) {
            blit(incoming, resolved)
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

    private fun composeOne(frame: FrameContext, inst: GiftFxInstance, srcTex: Int, dst: GlFramebuffer) {
        val program = compose ?: return
        val def = inst.definition ?: return
        dst.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex(frame))
        GLES30.glUniform1i(program.loc("uMask"), 1)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (engine.renderer.freezeValid) freeze.tex else srcTex)
        GLES30.glUniform1i(program.loc("uFreeze"), 2)
        GLES30.glUniform1i(program.loc("uHasFreeze"), if (engine.renderer.freezeValid && inst.freezeCaptured) 1 else 0)
        GLES30.glUniform1i(program.loc("uGiftType"), def.shaderLook)
        GLES30.glUniform1i(program.loc("uPhase"), inst.phaseIndex())
        GLES30.glUniform1i(program.loc("uLayer"), layerIndex(def.layer))
        GLES30.glUniform1i(program.loc("uOcclusion"), occIndex(def.occlusion))
        GLES30.glUniform1f(program.loc("uTime"), inst.elapsedMs * 0.001f)
        GLES30.glUniform1f(program.loc("uNorm"), inst.normalized)
        GLES30.glUniform1f(program.loc("uEnvelope"), inst.envelope)
        GLES30.glUniform1f(program.loc("uGravity"), inst.gravitySign)
        GLES30.glUniform2f(program.loc("uSubject"), inst.subjectX, inst.subjectY)
        GLES30.glUniform2f(program.loc("uClone"), inst.cloneX, inst.cloneY)
        GLES30.glUniform1f(program.loc("uIod"), inst.subjectIod)
        GLES30.glUniform2f(program.loc("uResolution"), frame.width.toFloat(), frame.height.toFloat())
        GLES30.glUniform1f(program.loc("uPortraitAspect"), GiftFxCoordinates.PORTRAIT_ASPECT)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        checkGl("gift compose")
    }

    private fun drawParticles(frame: FrameContext, inst: GiftFxInstance, dst: GlFramebuffer) {
        val program = sprite ?: return
        val def = inst.definition ?: return
        dst.bind()
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex(frame))
        GLES30.glUniform1i(program.loc("uMask"), 0)
        GLES30.glBindVertexArray(quadVao)
        val occ = if (def.occlusion == GiftFxOcclusion.None) 0f else 1f
        val cr = colorR(def.shaderLook)
        val cg = colorG(def.shaderLook)
        val cb = colorB(def.shaderLook)
        val pdata = inst.particles.data
        val stride = GiftFxParticleSystem.STRIDE
        val cap = inst.particles.capacity()
        var i = 0
        while (i < cap) {
            val o = i * stride
            val life = pdata[o + 4]
            if (life > 0f) {
                val x = pdata[o]
                val y = pdata[o + 1]
                if (x.isFinite() && y.isFinite()) {
                    val maxL = pdata[o + 5].coerceAtLeast(1e-4f)
                    val size = pdata[o + 6]
                    val kind = pdata[o + 7].toInt()
                    GLES30.glUniform2f(program.loc("uCenter"), x, y)
                    GLES30.glUniform2f(program.loc("uSize"), size, size * 1.4f)
                    GLES30.glUniform1f(program.loc("uAngle"), 0f)
                    GLES30.glUniform3f(program.loc("uColor"), cr, cg, cb)
                    GLES30.glUniform1f(program.loc("uAlpha"), (life / maxL) * inst.envelope)
                    GLES30.glUniform1i(program.loc("uKind"), kind)
                    GLES30.glUniform1f(program.loc("uOcc"), occ)
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
                }
            }
            i++
        }
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun maskTex(frame: FrameContext): Int =
        if (frame.segmentationTextureId != 0) frame.segmentationTextureId else fallbackMask

    private fun layerIndex(layer: GiftFxLayer): Int = when (layer) {
        GiftFxLayer.Background -> 0
        GiftFxLayer.BehindSubject -> 1
        GiftFxLayer.SubjectAttached -> 2
        GiftFxLayer.Foreground -> 3
        GiftFxLayer.FullFrame -> 4
    }

    private fun occIndex(o: GiftFxOcclusion): Int = when (o) {
        GiftFxOcclusion.None -> 0
        GiftFxOcclusion.PersonMask -> 1
        GiftFxOcclusion.HairMask -> 2
        GiftFxOcclusion.PersonAndHair -> 3
    }

    private fun colorR(look: Int): Float = 0.2f + 0.8f * ((look * 17) % 11).toFloat() / 10f
    private fun colorG(look: Int): Float = 0.15f + 0.85f * ((look * 9) % 13).toFloat() / 12f
    private fun colorB(look: Int): Float = 0.2f + 0.8f * ((look * 5) % 7).toFloat() / 6f
}
