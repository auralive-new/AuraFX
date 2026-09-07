package com.aurafx.sdk.filter

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.beauty.RegionMaskBuilder
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

/**
 * GPU FilterEngine in the existing effect graph.
 * Order: makeup → beauty → this. Color grade never runs before cosmetics.
 */
class FilterPipelineEffect(
    private val rig: FilterRig,
) : Effect {
    override val id: String = ID

    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private var grade: ShaderProgram? = null
    private val resolved = GlFramebuffer()
    private val filtered = GlFramebuffer()
    private var maskTex = 0
    private var lutTex = 0
    private var quadVao = 0
    private var quadVbo = 0
    private var maskBuffer: ByteBuffer? = null
    private var lutBuffer: ByteBuffer? = null
    private var attached = false
    private var lastW = 0
    private var lastH = 0
    private var uploadedLutId: String? = null

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(FilterShaders.VERT_RESOLVE, FilterShaders.FRAG_RESOLVE_OES)
        copy2d = ShaderProgram(FilterShaders.VERT_BLIT, FilterShaders.FRAG_COPY_2D)
        grade = ShaderProgram(FilterShaders.VERT_BLIT, FilterShaders.FRAG_GRADE)
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
        val mt = IntArray(1)
        GLES30.glGenTextures(1, mt, 0)
        maskTex = mt[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        val lt = IntArray(1)
        GLES30.glGenTextures(1, lt, 0)
        lutTex = lt[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTex)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)
        uploadLut(Lut3d.identity())
        uploadedLutId = null
        GLES30.glBindVertexArray(0)
        maskBuffer = ByteBuffer.allocateDirect(RegionMaskBuilder.SIZE * RegionMaskBuilder.SIZE * 4)
        attached = true
        AuraFxLog.i("FilterPipelineEffect attached")
        checkGl("filter attach")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached || frame.width <= 0 || frame.height <= 0) return
        val snap = rig.snapshot()
        if (snap.isIdentity()) return
        val def = snap.definition ?: return
        ensureTargets(frame.width, frame.height)
        val incoming = frame.processedTextureId
        if (incoming != 0 && !frame.processedIsOes) {
            copyPrevious(incoming)
        } else {
            resolveOes(frame)
        }
        if (def.usesLut && uploadedLutId != def.id) {
            uploadLut(def.bakedLut())
            uploadedLutId = def.id
        } else if (!def.usesLut && uploadedLutId != "identity") {
            uploadLut(Lut3d.identity())
            uploadedLutId = "identity"
        }
        uploadMask(tracking)
        runGrade(frame, snap, def)
        frame.processedTextureId = filtered.tex
        frame.processedIsOes = false
    }

    override fun onDetach() {
        attached = false
        resolve?.release()
        copy2d?.release()
        grade?.release()
        resolve = null
        copy2d = null
        grade = null
        resolved.release()
        filtered.release()
        if (maskTex != 0) GLES30.glDeleteTextures(1, intArrayOf(maskTex), 0)
        if (lutTex != 0) GLES30.glDeleteTextures(1, intArrayOf(lutTex), 0)
        if (quadVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(quadVao), 0)
        if (quadVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadVbo), 0)
        maskTex = 0
        lutTex = 0
        quadVao = 0
        quadVbo = 0
        uploadedLutId = null
    }

    private fun ensureTargets(w: Int, h: Int) {
        if (w == lastW && h == lastH && resolved.tex != 0) return
        lastW = w
        lastH = h
        resolved.ensure(w, h)
        filtered.ensure(w, h)
    }

    private fun resolveOes(frame: FrameContext) {
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
        checkGl("filter resolve")
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
        checkGl("filter copy2d")
    }

    private fun uploadMask(tracking: TrackingData) {
        val pixels = RegionMaskBuilder.build(tracking.landmarks)
        val buf = maskBuffer ?: return
        buf.rewind()
        buf.put(pixels)
        buf.rewind()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            RegionMaskBuilder.SIZE, RegionMaskBuilder.SIZE, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf,
        )
    }

    private fun uploadLut(lut: Lut3d) {
        val rgb = lut.toRgb8()
        val needed = rgb.size
        val existing = lutBuffer
        val buf = if (existing != null && existing.capacity() >= needed) {
            existing
        } else {
            ByteBuffer.allocateDirect(needed).also { lutBuffer = it }
        }
        buf.clear()
        buf.put(rgb)
        buf.rewind()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTex)
        GLES30.glTexImage3D(
            GLES30.GL_TEXTURE_3D, 0, GLES30.GL_RGB8,
            lut.size, lut.size, lut.size, 0,
            GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, buf,
        )
        checkGl("filter lut upload")
    }

    private fun runGrade(frame: FrameContext, snap: FilterSnapshot, def: FilterDefinition) {
        val program = grade ?: return
        val g = def.grade.clamp()
        val parametric = if (def.usesLut) ColorGrade() else g
        filtered.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, resolved.tex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex)
        GLES30.glUniform1i(program.loc("uMask"), 1)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTex)
        GLES30.glUniform1i(program.loc("uLut"), 2)
        GLES30.glUniform2f(program.loc("uTexel"), 1f / frame.width, 1f / frame.height)
        GLES30.glUniform1f(program.loc("uIntensity"), snap.intensity)
        GLES30.glUniform1f(program.loc("uUseLut"), if (def.usesLut) 1f else 0f)
        GLES30.glUniform1f(program.loc("uLutSize"), def.lutSize.toFloat())
        GLES30.glUniform1f(program.loc("uExposure"), parametric.exposure)
        GLES30.glUniform1f(program.loc("uBrightness"), parametric.brightness)
        GLES30.glUniform1f(program.loc("uContrast"), parametric.contrast)
        GLES30.glUniform1f(program.loc("uSaturation"), parametric.saturation)
        GLES30.glUniform1f(program.loc("uVibrance"), parametric.vibrance)
        GLES30.glUniform1f(program.loc("uTemperature"), parametric.temperature)
        GLES30.glUniform1f(program.loc("uTint"), parametric.tint)
        GLES30.glUniform1f(program.loc("uHighlights"), parametric.highlights)
        GLES30.glUniform1f(program.loc("uShadows"), parametric.shadows)
        GLES30.glUniform1f(program.loc("uBlacks"), parametric.blacks)
        GLES30.glUniform1f(program.loc("uWhites"), parametric.whites)
        GLES30.glUniform1f(program.loc("uGamma"), parametric.gamma)
        GLES30.glUniform1f(program.loc("uLift"), parametric.lift)
        GLES30.glUniform1f(program.loc("uGain"), parametric.gain)
        GLES30.glUniform1f(program.loc("uBalShadowR"), parametric.balanceShadowR)
        GLES30.glUniform1f(program.loc("uBalShadowB"), parametric.balanceShadowB)
        GLES30.glUniform1f(program.loc("uBalHighR"), parametric.balanceHighR)
        GLES30.glUniform1f(program.loc("uBalHighB"), parametric.balanceHighB)
        GLES30.glUniform1f(program.loc("uSelectiveSat"), parametric.selectiveSat)
        GLES30.glUniform1f(program.loc("uVignette"), snap.vignette())
        GLES30.glUniform1f(program.loc("uGrain"), snap.grain())
        GLES30.glUniform1f(program.loc("uBloom"), g.bloom)
        GLES30.glUniform1f(program.loc("uSkinProtect"), if (def.skinAware) g.skinProtect else 0f)
        GLES30.glUniform1f(program.loc("uFeatureProtect"), if (def.skinAware) g.featureProtect else 0f)
        GLES30.glUniform1i(program.loc("uSceneMode"), def.sceneMode)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        checkGl("filter grade")
    }

    companion object {
        const val ID = "aurafx.filter.engine"
    }
}
