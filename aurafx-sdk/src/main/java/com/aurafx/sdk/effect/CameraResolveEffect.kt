package com.aurafx.sdk.effect

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.filter.FilterShaders
import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.internal.render.GlFramebuffer
import com.aurafx.sdk.internal.render.ShaderProgram
import com.aurafx.sdk.internal.render.checkGl
import com.aurafx.sdk.internal.render.checkGlSoft
import com.aurafx.sdk.vision.TrackingData
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Always convert the camera OES texture to a 2D FBO before later effects.
 * Makeup / filter / AR then share one upright, mirrored 2D frame instead of
 * each pass silently skipping and leaving the presenter on raw OES.
 */
class CameraResolveEffect : Effect {
    override val id: String = ID

    private var resolve: ShaderProgram? = null
    private var copy2d: ShaderProgram? = null
    private val target = GlFramebuffer()
    private var quadVao = 0
    private var quadVbo = 0
    private var attached = false
    private var lastW = 0
    private var lastH = 0

    override fun onAttach(context: EffectContext) {
        resolve = ShaderProgram(FilterShaders.VERT_RESOLVE, FilterShaders.FRAG_RESOLVE_OES)
        copy2d = ShaderProgram(FilterShaders.VERT_BLIT, FilterShaders.FRAG_COPY_2D)
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
        attached = true
        AuraFxLog.i("CameraResolveEffect attached")
        checkGl("camera resolve attach")
    }

    override fun process(frame: FrameContext, tracking: TrackingData) {
        if (!attached || frame.width <= 0 || frame.height <= 0) return
        if (frame.width != lastW || frame.height != lastH || target.tex == 0) {
            lastW = frame.width
            lastH = frame.height
            target.ensure(frame.width, frame.height)
        }
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
        } else {
            val program = resolve ?: return
            target.bind()
            program.use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, frame.inputTextureId)
            GLES30.glUniform1i(program.loc("uTexture"), 0)
            GLES30.glUniformMatrix4fv(program.loc("uTexMatrix"), 1, false, frame.texMatrix, 0)
            val mirror = if (frame.lensFacing == LensFacing.FRONT) -1f else 1f
            GLES30.glUniform1f(program.loc("uMirror"), mirror)
            GLES30.glBindVertexArray(quadVao)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }
        GLES30.glBindVertexArray(0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        frame.processedTextureId = target.tex
        frame.processedIsOes = false
        checkGlSoft("camera resolve")
    }

    override fun onDetach() {
        attached = false
        resolve?.release()
        copy2d?.release()
        resolve = null
        copy2d = null
        target.release()
        if (quadVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(quadVao), 0)
        if (quadVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadVbo), 0)
        quadVao = 0
        quadVbo = 0
    }

    companion object {
        const val ID = "aurafx.camera.resolve"
    }
}
