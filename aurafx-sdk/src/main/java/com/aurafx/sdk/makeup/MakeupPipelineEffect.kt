package com.aurafx.sdk.makeup

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.LensStyle
import com.aurafx.sdk.api.MakeupColor
import com.aurafx.sdk.effect.Effect
import com.aurafx.sdk.effect.EffectContext
import com.aurafx.sdk.effect.FrameContext
import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.internal.render.GlFramebuffer
import com.aurafx.sdk.internal.render.ShaderProgram
import com.aurafx.sdk.internal.render.checkGl
import com.aurafx.sdk.vision.TrackingData
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MakeupPipelineEffect(
    private val rig: MakeupRig,
) : Effect {
    override val id: String = ID

    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private var makeup: ShaderProgram? = null
    private val resolved = GlFramebuffer()
    private val composed = GlFramebuffer()
    private var maskA = 0
    private var maskB = 0
    private var quadVao = 0
    private var quadVbo = 0
    private var bufA: ByteBuffer? = null
    private var bufB: ByteBuffer? = null
    private var attached = false
    private var lastW = 0
    private var lastH = 0

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(MakeupShaders.VERT, MakeupShaders.FRAG_OES)
        copy2d = ShaderProgram(MakeupShaders.VERT_BLIT, MakeupShaders.FRAG_COPY_2D)
        makeup = ShaderProgram(MakeupShaders.VERT, MakeupShaders.FRAG_MAKEUP)
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
        val mt = IntArray(2)
        GLES30.glGenTextures(2, mt, 0)
        maskA = mt[0]
        maskB = mt[1]
        for (t in mt) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, t)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        }
        val bytes = MakeupMaskBuilder.SIZE * MakeupMaskBuilder.SIZE * 4
        bufA = ByteBuffer.allocateDirect(bytes)
        bufB = ByteBuffer.allocateDirect(bytes)
        GLES30.glBindVertexArray(0)
        attached = true
        AuraFxLog.i("MakeupPipelineEffect attached")
        checkGl("makeup attach")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached || frame.width <= 0 || frame.height <= 0) return
        val snap = rig.snapshot()
        val lm = tracking.landmarks
        if (snap.isIdentity() || lm == null) return
        val geo = MakeupGeometryBuilder.build(lm, snap)
        ensure(frame.width, frame.height)
        val incoming = frame.processedTextureId
        if (incoming != 0 && !frame.processedIsOes) {
            copyPrevious(incoming)
        } else {
            resolveOes(frame)
        }
        uploadMasks(lm, geo, snap)
        compose(frame, snap, geo)
        frame.processedTextureId = composed.tex
        frame.processedIsOes = false
    }

    override fun onDetach() {
        attached = false
        resolve?.release()
        copy2d?.release()
        makeup?.release()
        resolve = null
        copy2d = null
        makeup = null
        resolved.release()
        composed.release()
        if (maskA != 0) GLES30.glDeleteTextures(2, intArrayOf(maskA, maskB), 0)
        if (quadVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(quadVao), 0)
        if (quadVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadVbo), 0)
        maskA = 0
        maskB = 0
        quadVao = 0
        quadVbo = 0
    }

    private fun ensure(w: Int, h: Int) {
        if (w == lastW && h == lastH && resolved.tex != 0) return
        lastW = w
        lastH = h
        resolved.ensure(w, h)
        composed.ensure(w, h)
    }

    private fun resolveOes(frame: FrameContext) {
        val program = resolve ?: return
        resolved.bind()
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
        checkGl("makeup resolve")
    }

    private fun copyPrevious(tex: Int) {
        val program = copy2d ?: return
        resolved.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glUniform1i(program.loc("uTexture"), 0)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        checkGl("makeup copy2d")
    }

    private fun uploadMasks(lm: com.aurafx.sdk.beauty.FaceLandmarks, geo: MakeupGeometry, params: com.aurafx.sdk.api.MakeupParameters) {
        val (pa, pb) = MakeupMaskBuilder.build(lm, geo, params)
        fun up(tex: Int, data: ByteArray, buf: ByteBuffer?) {
            val b = buf ?: return
            b.rewind()
            b.put(data)
            b.rewind()
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                MakeupMaskBuilder.SIZE, MakeupMaskBuilder.SIZE, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, b,
            )
        }
        up(maskA, pa, bufA)
        up(maskB, pb, bufB)
    }

    private fun compose(frame: FrameContext, snap: com.aurafx.sdk.api.MakeupParameters, geo: MakeupGeometry) {
        val program = makeup ?: return
        composed.bind()
        program.use()
        GLES30.glUniform1f(program.loc("uMirror"), 1f)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, resolved.tex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskA)
        GLES30.glUniform1i(program.loc("uMaskA"), 1)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskB)
        GLES30.glUniform1i(program.loc("uMaskB"), 2)
        GLES30.glUniform2f(program.loc("uTexel"), 1f / frame.width, 1f / frame.height)
        val count = geo.stamps.size.coerceAtMost(12)
        GLES30.glUniform1i(program.loc("uStampCount"), count)
        val stampArr = FloatArray(48)
        val kinds = FloatArray(12)
        for (i in 0 until 12) {
            val s = geo.stamps.getOrNull(i)
            if (s != null) {
                stampArr[i * 4] = s.x
                stampArr[i * 4 + 1] = s.y
                stampArr[i * 4 + 2] = s.rx
                stampArr[i * 4 + 3] = s.ry
                kinds[i] = s.kind.toFloat()
            }
        }
        GLES30.glUniform4fv(program.loc("uStamps"), 12, stampArr, 0)
        GLES30.glUniform1fv(program.loc("uStampKind"), 12, kinds, 0)
        fun col(c: MakeupColor) = floatArrayOf(c.r, c.g, c.b)
        GLES30.glUniform1f(program.loc("uFoundation"), snap.foundation.intensity)
        GLES30.glUniform1f(program.loc("uCoverage"), snap.foundation.coverage)
        GLES30.glUniform3fv(program.loc("uFoundCol"), 1, col(snap.foundation.color), 0)
        GLES30.glUniform1f(program.loc("uConcealer"), snap.concealer.intensity)
        GLES30.glUniform3fv(program.loc("uConcCol"), 1, col(snap.concealer.color), 0)
        GLES30.glUniform1f(program.loc("uBlush"), snap.blush.intensity)
        GLES30.glUniform3fv(program.loc("uBlushCol"), 1, col(snap.blush.color), 0)
        GLES30.glUniform1f(program.loc("uContour"), snap.contour.intensity)
        GLES30.glUniform3fv(program.loc("uContourCol"), 1, col(snap.contour.color), 0)
        GLES30.glUniform1f(program.loc("uHighlight"), snap.highlight.intensity)
        GLES30.glUniform3fv(program.loc("uHighCol"), 1, col(snap.highlight.color), 0)
        GLES30.glUniform1f(program.loc("uBrow"), snap.eyebrow.intensity)
        GLES30.glUniform3fv(program.loc("uBrowCol"), 1, col(snap.eyebrow.color), 0)
        GLES30.glUniform1f(program.loc("uShadow"), snap.eyeshadow.intensity)
        GLES30.glUniform3fv(program.loc("uLidCol"), 1, col(snap.eyeshadow.lidColor), 0)
        GLES30.glUniform3fv(program.loc("uCreaseCol"), 1, col(snap.eyeshadow.creaseColor), 0)
        GLES30.glUniform1f(program.loc("uLiner"), snap.eyeliner.intensity)
        GLES30.glUniform3fv(program.loc("uLinerCol"), 1, col(snap.eyeliner.color), 0)
        GLES30.glUniform1f(program.loc("uLash"), snap.eyelashes.intensity)
        GLES30.glUniform3fv(program.loc("uLashCol"), 1, col(snap.eyelashes.color), 0)
        GLES30.glUniform1f(program.loc("uLip"), snap.lipstick.intensity)
        GLES30.glUniform1f(program.loc("uLipOpacity"), snap.lipstick.opacity)
        GLES30.glUniform3fv(program.loc("uLipCol"), 1, col(snap.lipstick.color), 0)
        GLES30.glUniform1f(program.loc("uLipLiner"), snap.lipLiner.intensity)
        GLES30.glUniform3fv(program.loc("uLipLinerCol"), 1, col(snap.lipLiner.color), 0)
        GLES30.glUniform1f(program.loc("uGloss"), snap.lipGloss.intensity)
        GLES30.glUniform1f(program.loc("uLens"), snap.lens.intensity)
        GLES30.glUniform3fv(program.loc("uLensCol"), 1, lensColor(snap.lens.style), 0)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        checkGl("makeup compose")
    }

    private fun lensColor(style: LensStyle): FloatArray = when (style) {
        LensStyle.PureTone -> floatArrayOf(0.45f, 0.62f, 0.78f)
        LensStyle.GoldenGlint -> floatArrayOf(0.72f, 0.55f, 0.22f)
        LensStyle.SapphireInk -> floatArrayOf(0.15f, 0.28f, 0.72f)
        LensStyle.WarmGlint -> floatArrayOf(0.62f, 0.42f, 0.28f)
        LensStyle.KiwiPop -> floatArrayOf(0.35f, 0.72f, 0.32f)
        LensStyle.SilverMist -> floatArrayOf(0.62f, 0.66f, 0.70f)
    }

    companion object {
        const val ID = "aurafx.makeup.professional"
    }
}
