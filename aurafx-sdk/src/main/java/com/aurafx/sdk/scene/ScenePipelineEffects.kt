package com.aurafx.sdk.scene

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.LightingMode
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
import java.nio.FloatBuffer

internal class QuadGpu {
    var vao = 0
    var vbo = 0
    fun create() {
        val qv = floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
        val qBuf = ByteBuffer.allocateDirect(qv.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(qv)
        qBuf.position(0)
        val va = IntArray(1)
        val vb = IntArray(1)
        GLES30.glGenVertexArrays(1, va, 0)
        GLES30.glGenBuffers(1, vb, 0)
        vao = va[0]
        vbo = vb[0]
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, qv.size * 4, qBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glBindVertexArray(0)
    }
    fun release() {
        if (vao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
        if (vbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
        vao = 0
        vbo = 0
    }
}

internal fun resolveOrCopy(
    frame: FrameContext,
    resolve: ShaderProgram?,
    copy2d: ShaderProgram?,
    target: GlFramebuffer,
    quadVao: Int,
) {
    val incoming = frame.processedTextureId
    if (incoming != 0 && !frame.processedIsOes) {
        val program = copy2d ?: return
        target.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, incoming)
        GLES30.glUniform1i(program.loc("uTexture"), 0)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    } else {
        val program = resolve ?: return
        target.bind()
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

class SegmentationUploadEffect : Effect {
    override val id: String = AuraFxEffectOrder.SEGMENTATION
    private var tex = 0
    private var buf: ByteBuffer? = null
    private var attached = false
    override fun onAttach(context: EffectContext) {
        val t = IntArray(1)
        GLES30.glGenTextures(1, t, 0)
        tex = t[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        attached = true
        AuraFxLog.i("SegmentationUploadEffect attached")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached) return
        val mask = tracking.segmentation ?: return
        if (!mask.inBounds()) return
        val needed = mask.packedRgba.size
        val existing = buf
        val b = if (existing != null && existing.capacity() >= needed) existing else {
            ByteBuffer.allocateDirect(needed).also { buf = it }
        }
        b.clear()
        b.put(mask.packedRgba)
        b.rewind()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            mask.width, mask.height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, b,
        )
        frame.segmentationTextureId = tex
    }

    override fun onDetach() {
        attached = false
        if (tex != 0) GLES30.glDeleteTextures(1, intArrayOf(tex), 0)
        tex = 0
    }
}

class BackgroundPipelineEffect(private val rig: BackgroundRig) : Effect {
    override val id: String = AuraFxEffectOrder.BACKGROUND
    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private var bg: ShaderProgram? = null
    private val resolved = GlFramebuffer()
    private val out = GlFramebuffer()
    private val quad = QuadGpu()
    private var lastW = 0
    private var lastH = 0
    private var attached = false

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(SceneShaders.VERT_RESOLVE, SceneShaders.FRAG_OES)
        copy2d = ShaderProgram(SceneShaders.VERT_BLIT, SceneShaders.FRAG_COPY)
        bg = ShaderProgram(SceneShaders.VERT_BLIT, SceneShaders.FRAG_BACKGROUND)
        quad.create()
        attached = true
        AuraFxLog.i("BackgroundPipelineEffect attached")
        checkGl("bg attach")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached) return
        val snap = rig.snapshot()
        if (snap.isIdentity()) return
        val def = snap.id?.let { BackgroundCatalog.require(it) } ?: return
        val mask = tracking.segmentation
        if (mask == null || !mask.inBounds() || mask.personCoverage < 0.008f) return
        if (frame.segmentationTextureId == 0) return
        ensure(frame.width, frame.height)
        resolveOrCopy(frame, resolve, copy2d, resolved, quad.vao)
        val program = bg ?: return
        out.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, resolved.tex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frame.segmentationTextureId)
        GLES30.glUniform1i(program.loc("uMask"), 1)
        GLES30.glUniform2f(program.loc("uTexel"), 1f / frame.width, 1f / frame.height)
        GLES30.glUniform1f(program.loc("uIntensity"), snap.intensity)
        GLES30.glUniform1i(program.loc("uMode"), def.shaderMode)
        GLES30.glUniform3f(program.loc("uColorA"), def.colorA[0], def.colorA[1], def.colorA[2])
        GLES30.glUniform3f(program.loc("uColorB"), def.colorB[0], def.colorB[1], def.colorB[2])
        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        frame.processedTextureId = out.tex
        frame.processedIsOes = false
        checkGl("bg")
    }

    override fun onDetach() {
        attached = false
        resolve?.release(); copy2d?.release(); bg?.release()
        resolve = null; copy2d = null; bg = null
        resolved.release(); out.release(); quad.release()
    }

    private fun ensure(w: Int, h: Int) {
        if (w == lastW && h == lastH && resolved.tex != 0) return
        lastW = w; lastH = h
        resolved.ensure(w, h); out.ensure(w, h)
    }
}

class HairPipelineEffect(private val rig: HairRig) : Effect {
    override val id: String = AuraFxEffectOrder.HAIR
    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private var hair: ShaderProgram? = null
    private val resolved = GlFramebuffer()
    private val out = GlFramebuffer()
    private val quad = QuadGpu()
    private var lastW = 0
    private var lastH = 0
    private var attached = false

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(SceneShaders.VERT_RESOLVE, SceneShaders.FRAG_OES)
        copy2d = ShaderProgram(SceneShaders.VERT_BLIT, SceneShaders.FRAG_COPY)
        hair = ShaderProgram(SceneShaders.VERT_BLIT, SceneShaders.FRAG_HAIR)
        quad.create()
        attached = true
        AuraFxLog.i("HairPipelineEffect attached")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached) return
        val snap = rig.snapshot()
        if (snap.isIdentity()) return
        val mask = tracking.segmentation
        if (mask == null || mask.hairCoverage < 0.002f || frame.segmentationTextureId == 0) return
        ensure(frame.width, frame.height)
        resolveOrCopy(frame, resolve, copy2d, resolved, quad.vao)
        val rgb = HairCatalog.colorRgb(snap.color, floatArrayOf(snap.customR, snap.customG, snap.customB))
        val program = hair ?: return
        out.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, resolved.tex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frame.segmentationTextureId)
        GLES30.glUniform1i(program.loc("uMask"), 1)
        GLES30.glUniform3f(program.loc("uHairCol"), rgb[0], rgb[1], rgb[2])
        GLES30.glUniform1f(program.loc("uIntensity"), snap.intensity)
        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        frame.processedTextureId = out.tex
        frame.processedIsOes = false
        checkGl("hair")
    }

    override fun onDetach() {
        attached = false
        resolve?.release(); copy2d?.release(); hair?.release()
        resolve = null; copy2d = null; hair = null
        resolved.release(); out.release(); quad.release()
    }

    private fun ensure(w: Int, h: Int) {
        if (w == lastW && h == lastH && resolved.tex != 0) return
        lastW = w; lastH = h
        resolved.ensure(w, h); out.ensure(w, h)
    }
}

class BodyPipelineEffect(private val rig: BodyRig) : Effect {
    override val id: String = AuraFxEffectOrder.BODY
    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private var warp: ShaderProgram? = null
    private val resolved = GlFramebuffer()
    private val out = GlFramebuffer()
    private val quad = QuadGpu()
    private var warpVao = 0
    private var warpVbo = 0
    private var warpIbo = 0
    private val indices = BodyWarpField.buildIndices()
    private var meshScratch = FloatArray(BodyWarpField.vertexCount() * 6)
    private var lastW = 0
    private var lastH = 0
    private var attached = false

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(SceneShaders.VERT_RESOLVE, SceneShaders.FRAG_OES)
        copy2d = ShaderProgram(SceneShaders.VERT_BLIT, SceneShaders.FRAG_COPY)
        warp = ShaderProgram(SceneShaders.VERT_WARP, SceneShaders.FRAG_BODY)
        quad.create()
        val va = IntArray(1)
        val vb = IntArray(1)
        GLES30.glGenVertexArrays(1, va, 0)
        GLES30.glGenBuffers(1, vb, 0)
        warpVao = va[0]
        warpVbo = vb[0]
        val ib = IntArray(1)
        GLES30.glGenBuffers(1, ib, 0)
        warpIbo = ib[0]
        val idx = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().put(indices)
        idx.position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, warpIbo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 4, idx, GLES30.GL_STATIC_DRAW)
        attached = true
        AuraFxLog.i("BodyPipelineEffect attached")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached) return
        val snap = rig.snapshot()
        if (snap.isIdentity()) return
        val pose = tracking.pose.firstOrNull()?.takeIf { it.isValid() } ?: return
        if (frame.segmentationTextureId == 0) return
        val mask = tracking.segmentation
        if (mask == null || mask.bodyCoverage < 0.002f) return
        ensure(frame.width, frame.height)
        resolveOrCopy(frame, resolve, copy2d, resolved, quad.vao)
        meshScratch = BodyWarpField.buildMesh(pose, snap, meshScratch)
        val program = warp ?: return
        out.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, resolved.tex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frame.segmentationTextureId)
        GLES30.glUniform1i(program.loc("uMask"), 1)
        val fb: FloatBuffer = ByteBuffer.allocateDirect(meshScratch.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(meshScratch)
        fb.position(0)
        GLES30.glBindVertexArray(warpVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, warpVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, meshScratch.size * 4, fb, GLES30.GL_DYNAMIC_DRAW)
        val stride = 6 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 8)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 16)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, warpIbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indices.size, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        frame.processedTextureId = out.tex
        frame.processedIsOes = false
        checkGl("body")
    }

    override fun onDetach() {
        attached = false
        resolve?.release(); copy2d?.release(); warp?.release()
        resolve = null; copy2d = null; warp = null
        resolved.release(); out.release(); quad.release()
        if (warpVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(warpVao), 0)
        if (warpVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(warpVbo), 0)
        if (warpIbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(warpIbo), 0)
        warpVao = 0; warpVbo = 0; warpIbo = 0
    }

    private fun ensure(w: Int, h: Int) {
        if (w == lastW && h == lastH && resolved.tex != 0) return
        lastW = w; lastH = h
        resolved.ensure(w, h); out.ensure(w, h)
    }
}

class LightingPipelineEffect(private val rig: LightingRig) : Effect {
    override val id: String = AuraFxEffectOrder.LIGHTING
    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private var light: ShaderProgram? = null
    private val resolved = GlFramebuffer()
    private val out = GlFramebuffer()
    private val quad = QuadGpu()
    private var lastW = 0
    private var lastH = 0
    private var attached = false

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(SceneShaders.VERT_RESOLVE, SceneShaders.FRAG_OES)
        copy2d = ShaderProgram(SceneShaders.VERT_BLIT, SceneShaders.FRAG_COPY)
        light = ShaderProgram(SceneShaders.VERT_BLIT, SceneShaders.FRAG_LIGHT)
        quad.create()
        attached = true
        AuraFxLog.i("LightingPipelineEffect attached")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached) return
        val snap = rig.snapshot()
        if (snap.isIdentity()) return
        if (frame.segmentationTextureId == 0) return
        val mask = tracking.segmentation
        if (mask == null || mask.personCoverage < 0.008f) return
        ensure(frame.width, frame.height)
        resolveOrCopy(frame, resolve, copy2d, resolved, quad.vao)
        val program = light ?: return
        out.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, resolved.tex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frame.segmentationTextureId)
        GLES30.glUniform1i(program.loc("uMask"), 1)
        GLES30.glUniform1f(program.loc("uIntensity"), snap.intensity)
        GLES30.glUniform1i(
            program.loc("uMode"),
            when (snap.mode) {
                LightingMode.Soft -> 0
                LightingMode.Directional -> 1
                LightingMode.Warm -> 2
                LightingMode.Cool -> 3
                LightingMode.Natural -> 4
            },
        )
        GLES30.glUniform1f(program.loc("uBrightness"), snap.brightness)
        GLES30.glUniform1f(program.loc("uWarmth"), snap.warmth)
        GLES30.glUniform1f(program.loc("uShadowLift"), snap.shadowLift)
        GLES30.glUniform1f(program.loc("uHighlight"), snap.highlightControl)
        GLES30.glUniform1f(program.loc("uAzimuth"), snap.azimuth)
        GLES30.glBindVertexArray(quad.vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        frame.processedTextureId = out.tex
        frame.processedIsOes = false
        checkGl("lighting")
    }

    override fun onDetach() {
        attached = false
        resolve?.release(); copy2d?.release(); light?.release()
        resolve = null; copy2d = null; light = null
        resolved.release(); out.release(); quad.release()
    }

    private fun ensure(w: Int, h: Int) {
        if (w == lastW && h == lastH && resolved.tex != 0) return
        lastW = w; lastH = h
        resolved.ensure(w, h); out.ensure(w, h)
    }
}
