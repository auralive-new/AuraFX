package com.aurafx.sdk.beauty

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
import com.aurafx.sdk.vision.TrackingData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * GPU skin + beauty color + localized face-shape warp.
 * Identity snapshot leaves the camera frame untouched (no processed texture).
 */
class BeautyPipelineEffect(
    private val rig: BeautyRig,
) : Effect {
    override val id: String = ID

    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private var skin: ShaderProgram? = null
    private var warp: ShaderProgram? = null
    private val resolved = GlFramebuffer()
    private val beautified = GlFramebuffer()
    private val warped = GlFramebuffer()
    private var maskTex = 0
    private var quadVao = 0
    private var quadVbo = 0
    private var warpVao = 0
    private var warpVbo = 0
    private var warpIbo = 0
    private var meshScratch = FloatArray(WarpField.vertexCount() * 6)
    private val indices = WarpField.buildIndices()
    private var maskBuffer: ByteBuffer? = null
    private var attached = false
    private var lastW = 0
    private var lastH = 0

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(BeautyShaders.VERT_RESOLVE, BeautyShaders.FRAG_RESOLVE_OES)
        copy2d = ShaderProgram(BeautyShaders.VERT_BLIT, BeautyShaders.FRAG_COPY_2D)
        skin = ShaderProgram(BeautyShaders.VERT_BLIT, BeautyShaders.FRAG_SKIN)
        warp = ShaderProgram(BeautyShaders.VERT_WARP, BeautyShaders.FRAG_WARP)
        val qv = floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
        val qBuf = ByteBuffer.allocateDirect(qv.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(qv)
        qBuf.position(0)
        val va = IntArray(2)
        val vb = IntArray(2)
        GLES30.glGenVertexArrays(2, va, 0)
        GLES30.glGenBuffers(2, vb, 0)
        quadVao = va[0]
        warpVao = va[1]
        quadVbo = vb[0]
        warpVbo = vb[1]
        GLES30.glBindVertexArray(quadVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, qv.size * 4, qBuf, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        val ib = IntArray(1)
        GLES30.glGenBuffers(1, ib, 0)
        warpIbo = ib[0]
        val idx = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().put(indices)
        idx.position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, warpIbo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 4, idx, GLES30.GL_STATIC_DRAW)
        val mt = IntArray(1)
        GLES30.glGenTextures(1, mt, 0)
        maskTex = mt[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glBindVertexArray(0)
        maskBuffer = ByteBuffer.allocateDirect(RegionMaskBuilder.SIZE * RegionMaskBuilder.SIZE * 4)
        attached = true
        AuraFxLog.i("BeautyPipelineEffect shaders attached")
        checkGl("beauty attach")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached || frame.width <= 0 || frame.height <= 0) return
        val snap = rig.snapshot()
        val landmarks = tracking.landmarks
        if (snap.isIdentity()) return
        if (landmarks == null) {
            if (snap.skinIdentity()) {
                AuraFxLog.debugThrottled(
                    "beauty skip: landmarks=null status=${tracking.status} faces=${tracking.faces.size} " +
                        "provider=${tracking.visionProvider}",
                )
                return
            }
            AuraFxLog.debugThrottled(
                "beauty global skin (no face yet) provider=${tracking.visionProvider} status=${tracking.status}",
            )
            ensureTargets(frame.width, frame.height)
            val incoming = frame.processedTextureId
            if (incoming != 0 && !frame.processedIsOes) {
                copyPrevious(incoming)
            } else {
                resolveOes(frame)
            }
            uploadFullFrameSkinMask()
            runSkin(frame, snap)
            frame.processedTextureId = beautified.tex
            frame.processedIsOes = false
            return
        }
        ensureTargets(frame.width, frame.height)
        val incoming = frame.processedTextureId
        if (incoming != 0 && !frame.processedIsOes) {
            copyPrevious(incoming)
        } else {
            resolveOes(frame)
        }
        uploadMask(landmarks)
        runSkin(frame, snap)
        val outTex = if (snap.shapeIdentity()) {
            beautified.tex
        } else {
            runWarp(snap, landmarks)
            warped.tex
        }
        frame.processedTextureId = outTex
        frame.processedIsOes = false
    }

    override fun onDetach() {
        attached = false
        resolve?.release()
        copy2d?.release()
        skin?.release()
        warp?.release()
        resolve = null
        copy2d = null
        skin = null
        warp = null
        resolved.release()
        beautified.release()
        warped.release()
        if (maskTex != 0) GLES30.glDeleteTextures(1, intArrayOf(maskTex), 0)
        if (quadVao != 0) GLES30.glDeleteVertexArrays(2, intArrayOf(quadVao, warpVao), 0)
        if (quadVbo != 0) GLES30.glDeleteBuffers(2, intArrayOf(quadVbo, warpVbo), 0)
        if (warpIbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(warpIbo), 0)
        maskTex = 0
        quadVao = 0
        warpVao = 0
        quadVbo = 0
        warpVbo = 0
        warpIbo = 0
    }

    private fun ensureTargets(w: Int, h: Int) {
        if (w == lastW && h == lastH && resolved.tex != 0) return
        lastW = w
        lastH = h
        resolved.ensure(w, h)
        beautified.ensure(w, h)
        warped.ensure(w, h)
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
        checkGl("beauty resolve")
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
        checkGl("beauty copy2d")
    }

    private fun uploadFullFrameSkinMask() {
        val pixels = RegionMaskBuilder.buildFullFrameSkin()
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

    private fun uploadMask(landmarks: FaceLandmarks) {
        val pixels = RegionMaskBuilder.build(landmarks)
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

    private fun runSkin(frame: FrameContext, snap: BeautySnapshot) {
        val program = skin ?: return
        beautified.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, resolved.tex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, maskTex)
        GLES30.glUniform1i(program.loc("uMask"), 1)
        GLES30.glUniform2f(program.loc("uTexel"), 1f / frame.width, 1f / frame.height)
        GLES30.glUniform1f(program.loc("uFine"), snap.fineSmooth)
        GLES30.glUniform1f(program.loc("uSmooth"), snap.smoothness)
        GLES30.glUniform1f(program.loc("uTextureKeep"), snap.texturePreserve)
        GLES30.glUniform1f(program.loc("uBlemish"), snap.blemishReduction)
        GLES30.glUniform1f(program.loc("uEvenness"), snap.evenness)
        GLES30.glUniform1f(program.loc("uBrightness"), snap.brightness)
        GLES30.glUniform1f(program.loc("uWhiten"), snap.whiten)
        GLES30.glUniform1f(program.loc("uRuddy"), snap.ruddy)
        GLES30.glUniform1f(program.loc("uTone"), snap.tone)
        GLES30.glUniform1f(program.loc("uNatural"), snap.naturalSkin)
        GLES30.glUniform1f(program.loc("uTooth"), snap.toothWhiten)
        GLES30.glUniform1f(program.loc("uCircles"), snap.circles)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        checkGl("beauty skin")
    }

    private fun runWarp(snap: BeautySnapshot, landmarks: FaceLandmarks): Int {
        val program = warp ?: return beautified.tex
        meshScratch = WarpField.buildMesh(landmarks, snap, meshScratch)
        warped.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, beautified.tex)
        GLES30.glUniform1i(program.loc("uImage"), 0)
        val fb: FloatBuffer = ByteBuffer.allocateDirect(meshScratch.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(meshScratch)
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
        checkGl("beauty warp")
        return warped.tex
    }

    companion object {
        const val ID = "aurafx.beauty.skin-shape"
    }
}
