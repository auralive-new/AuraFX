package com.aurafx.sdk.internal.render

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.view.Surface
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxInputFrame
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.api.AuraFxSessionListener
import com.aurafx.sdk.api.CapturedPhoto
import com.aurafx.sdk.api.FrameIngress
import com.aurafx.sdk.api.LensFacing
import com.aurafx.sdk.api.RecordedVideo
import com.aurafx.sdk.capture.ProcessedJpegWriter
import com.aurafx.sdk.capture.ProcessedVideoRecorder
import com.aurafx.sdk.effect.EffectContext
import com.aurafx.sdk.effect.EffectManager
import com.aurafx.sdk.effect.FrameContext
import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.performance.PerformanceManager
import com.aurafx.sdk.pipeline.FramePipeline
import com.aurafx.sdk.vision.VisionFrame
import com.aurafx.sdk.vision.VisionProcessor
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class AuraFxRenderThread(
    private val performance: PerformanceManager,
    private val pipeline: FramePipeline,
    private val effects: EffectManager,
    private val vision: VisionProcessor,
    private val mirrorFrontCamera: Boolean,
    private val listener: AuraFxSessionListener?,
    private val mainPoster: (Runnable) -> Unit,
) {
    private val thread = HandlerThread("aurafx-gl", Process.THREAD_PRIORITY_DISPLAY)
    private lateinit var handler: Handler
    private val ready = CountDownLatch(1)
    private val initError = AtomicReference<Throwable?>()

    private var egl: EglCore? = null
    private var pbuffer: EGLSurface = EGL14.EGL_NO_SURFACE
    private var windowSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var outputSurface: Surface? = null
    private var viewportW = 0
    private var viewportH = 0

    private val blit = BlitProgram()
    private val gpuTimer = GpuTimer()
    private var oesTextureId = 0
    private var cpuTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraSurface: Surface? = null
    private val texMatrix = FloatArray(16)
    private var facing: LensFacing = LensFacing.FRONT
    private val firstFrameNotified = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private val liveCameraActive = AtomicBoolean(false)
    private val recorder = ProcessedVideoRecorder(performance)
    private val photoIo = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "aurafx-photo").apply { isDaemon = true }
    }
    private val captureFbo = GlFramebuffer()
    private var photoPixels: ByteBuffer? = null
    private data class PhotoJob(val file: File, val done: (AuraFxResult<CapturedPhoto>) -> Unit)
    private val photoJob = AtomicReference<PhotoJob?>(null)

    fun start() {
        AuraFxLog.i("GL thread start")
        thread.start()
        handler = Handler(thread.looper)
        handler.post {
            try {
                initializeGl()
            } catch (t: Throwable) {
                AuraFxLog.e("GL init failed", t)
                initError.set(t)
            } finally {
                ready.countDown()
            }
        }
    }

    fun awaitReady(timeoutMs: Long = 4_000): AuraFxError? {
        if (!ready.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            return AuraFxError.GpuFailure("GL thread init timed out")
        }
        val err = initError.get() ?: return null
        return AuraFxError.GpuFailure(err.message ?: "GL init failed", err)
    }

    fun cameraPreviewSurface(): Surface =
        cameraSurface ?: throw IllegalStateException("GL camera surface not created")

    fun setFacing(value: LensFacing) {
        facing = value
    }

    fun setLiveCameraActive(active: Boolean) {
        liveCameraActive.set(active)
        if (active) {
            firstFrameNotified.set(false)
        }
        AuraFxLog.i("liveCameraActive=$active")
    }

    fun isRecording(): Boolean = recorder.isRunning()

    fun captureProcessedPhoto(file: File, done: (AuraFxResult<CapturedPhoto>) -> Unit) {
        photoJob.set(PhotoJob(file, done))
    }

    fun startRecording(file: File, recordAudio: Boolean): AuraFxResult<Unit> {
        if (recorder.isRunning()) {
            return AuraFxResult.Err(AuraFxError.InvalidState("Recording already in progress"))
        }
        return try {
            handler.runSync(timeoutMs = 2_500) {
                val eglCore = egl ?: throw IllegalStateException("EGL not ready")
                val w = if (viewportW > 0) viewportW else 1280
                val h = if (viewportH > 0) viewportH else 720
                recorder.start(eglCore, file, w, h, recordAudio)
                if (windowSurface != EGL14.EGL_NO_SURFACE) {
                    eglCore.makeCurrent(windowSurface)
                }
            }
            AuraFxResult.Ok(Unit)
        } catch (t: Throwable) {
            AuraFxLog.e("startRecording failed", t)
            AuraFxResult.Err(AuraFxError.GpuFailure("Failed to start processed video recording", t))
        }
    }

    fun stopRecording(done: (AuraFxResult<RecordedVideo>) -> Unit) {
        handler.post {
            try {
                if (!recorder.isRunning()) {
                    mainPoster { done(AuraFxResult.Err(AuraFxError.InvalidState("Not recording"))) }
                    return@post
                }
                val result = recorder.stop()
                val eglCore = egl
                if (eglCore != null && windowSurface != EGL14.EGL_NO_SURFACE) {
                    eglCore.makeCurrent(windowSurface)
                }
                performance.markExportNs(result.durationUs * 1_000L)
                mainPoster { done(AuraFxResult.Ok(result)) }
            } catch (t: Throwable) {
                AuraFxLog.e("stopRecording failed", t)
                mainPoster { done(AuraFxResult.Err(AuraFxError.GpuFailure("Failed to stop recording", t))) }
            }
        }
    }

    fun setCameraBufferSize(width: Int, height: Int) {
        handler.runSync {
            AuraFxLog.i("SurfaceTexture setDefaultBufferSize ${width}x${height}")
            surfaceTexture?.setDefaultBufferSize(width, height)
        }
    }

    fun attachOutput(surface: Surface, width: Int, height: Int) {
        handler.post {
            try {
                detachWindowLocked()
                outputSurface = surface
                viewportW = width
                viewportH = height
                val eglCore = egl ?: return@post
                windowSurface = eglCore.createWindowSurface(surface)
                eglCore.makeCurrent(windowSurface)
                GLES30.glViewport(0, 0, width, height)
                GLES30.glClearColor(0f, 0f, 0f, 1f)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                eglCore.swapBuffers(windowSurface)
                AuraFxLog.i("EGL window attached ${width}x${height}")
            } catch (t: Throwable) {
                AuraFxLog.e("Failed to attach preview surface", t)
                postError(AuraFxError.GpuFailure("Failed to attach preview surface", t))
            }
        }
    }

    fun detachOutput() {
        handler.post {
            try {
                detachWindowLocked()
                egl?.makeCurrent(pbuffer)
            } catch (_: Throwable) {
            }
        }
    }

    fun resize(width: Int, height: Int) {
        handler.post {
            viewportW = width
            viewportH = height
            if (windowSurface != EGL14.EGL_NO_SURFACE) {
                GLES30.glViewport(0, 0, width, height)
            }
        }
    }

    fun submitExternalFrame(frame: AuraFxInputFrame) {
        if (liveCameraActive.get()) {
            AuraFxLog.w("processFrame dropped; live camera pipeline owns the output")
            performance.onDropped()
            return
        }
        handler.post { presentExternal(frame) }
    }

    fun release() {
        if (!released.compareAndSet(false, true)) return
        val latch = CountDownLatch(1)
        handler.post {
            try {
                teardownGl()
            } finally {
                latch.countDown()
            }
        }
        latch.await(2, TimeUnit.SECONDS)
        thread.quitSafely()
        thread.join(1_000)
    }

    private fun initializeGl() {
        val eglCore = EglCore()
        egl = eglCore
        pbuffer = eglCore.createPbufferSurface(1, 1)
        eglCore.makeCurrent(pbuffer)
        blit.init()
        gpuTimer.init()

        val textures = IntArray(2)
        GLES30.glGenTextures(2, textures, 0)
        oesTextureId = textures[0]
        cpuTextureId = textures[1]
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cpuTextureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        checkGl("texture setup")

        val st = SurfaceTexture(oesTextureId)
        st.setDefaultBufferSize(1280, 720)
        st.setOnFrameAvailableListener({ onCameraFrameAvailable() }, handler)
        surfaceTexture = st
        cameraSurface = Surface(st)

        effects.attach(EffectContext(eglCore.context, performance))
        AuraFxLog.i(
            "EGL/GLES ready oesTex=$oesTextureId gpuTimer=${gpuTimer.supported} " +
                "renderer=BlitProgram OES",
        )
    }

    private fun onCameraFrameAvailable() {
        if (released.get()) return
        if (windowSurface == EGL14.EGL_NO_SURFACE) {
            performance.onDropped()
            drainTexture()
            return
        }
        if (!pipeline.tryBegin()) {
            performance.onDropped()
            drainTexture()
            return
        }
        val started = System.nanoTime()
        try {
            val st = surfaceTexture ?: return
            st.updateTexImage()
            val timestampNs = st.timestamp
            st.getTransformMatrix(texMatrix)
            val eglCore = egl ?: return
            eglCore.makeCurrent(windowSurface)
            if (viewportW > 0 && viewportH > 0) {
                GLES30.glViewport(0, 0, viewportW, viewportH)
            }
            val visionFrame = VisionFrame(
                timestampNs = timestampNs,
                width = viewportW,
                height = viewportH,
                oesTextureId = oesTextureId,
                texMatrix = texMatrix,
            )
            val tracking = vision.process(visionFrame)
            val frameContext = FrameContext(
                timestampNs = timestampNs,
                width = viewportW,
                height = viewportH,
                inputTextureId = oesTextureId,
                inputIsOes = true,
                texMatrix = texMatrix,
                lensFacing = facing,
            )
            effects.process(frameContext, tracking)

            val previousGpuNs = gpuTimer.pollNs()
            gpuTimer.begin()
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            val mirror = mirrorFrontCamera && facing == LensFacing.FRONT
            if (frameContext.processedTextureId != 0 && !frameContext.outputIsOes()) {
                blit.draw2d(frameContext.outputTextureId(), mirrorX = false)
            } else {
                blit.drawOes(oesTextureId, texMatrix, mirror)
            }
            gpuTimer.end()
            presentCaptureSinks(eglCore, frameContext, timestampNs)
            eglCore.makeCurrent(windowSurface)
            eglCore.swapBuffers(windowSurface)
            val processNs = System.nanoTime() - started
            performance.onFramePresented(
                timestampNs,
                processNs,
                previousGpuNs,
                ingress = FrameIngress.CAMERA_OES,
                admitted = pipeline.admittedCount(),
            )
            performance.markCameraReady()
            if (firstFrameNotified.compareAndSet(false, true)) {
                AuraFxLog.i(
                    "first OES frame ts=$timestampNs facing=$facing " +
                        "viewport=${viewportW}x${viewportH} mirror=$mirror",
                )
                mainPoster { listener?.onFirstFrame(timestampNs) }
            }
        } catch (t: Throwable) {
            AuraFxLog.e("Frame present failed", t)
            postError(AuraFxError.GpuFailure("Frame present failed", t))
        } finally {
            pipeline.end()
        }
    }

    private fun presentCaptureSinks(eglCore: EglCore, frame: FrameContext, timestampNs: Long) {
        val job = photoJob.getAndSet(null)
        if (job != null) {
            capturePhotoLocked(frame, timestampNs, job)
        }
        if (recorder.isRunning()) {
            val tex = frame.outputTextureId()
            val oes = frame.outputIsOes()
            recorder.drawProcessed(eglCore, blit, tex, oes, frame.texMatrix, timestampNs)
        }
    }

    private fun capturePhotoLocked(frame: FrameContext, timestampNs: Long, job: PhotoJob) {
        val started = System.nanoTime()
        try {
            val w = viewportW.coerceAtLeast(1)
            val h = viewportH.coerceAtLeast(1)
            captureFbo.ensure(w, h)
            captureFbo.bind()
            GLES30.glViewport(0, 0, w, h)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            if (frame.processedTextureId != 0 && !frame.outputIsOes()) {
                blit.draw2d(frame.outputTextureId(), mirrorX = false)
            } else if (frame.inputIsOes) {
                val mirror = mirrorFrontCamera && facing == LensFacing.FRONT
                blit.drawOes(frame.inputTextureId, frame.texMatrix, mirror)
            } else {
                blit.draw2d(frame.outputTextureId(), mirrorX = false)
            }
            val needed = w * h * 4
            val buf = photoPixels?.takeIf { it.capacity() >= needed } ?: ByteBuffer.allocateDirect(needed).also {
                photoPixels = it
            }
            buf.clear()
            GLES30.glReadPixels(0, 0, w, h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            val copy = ByteBuffer.allocateDirect(needed)
            buf.rewind()
            copy.put(buf)
            copy.rewind()
            val processed = frame.processedTextureId != 0
            photoIo.execute {
                try {
                    ProcessedJpegWriter.writeFlippedRgba(w, h, copy, job.file)
                    performance.markPhotoNs(System.nanoTime() - started)
                    mainPoster {
                        job.done(
                            AuraFxResult.Ok(
                                CapturedPhoto(job.file, w, h, timestampNs, processed),
                            ),
                        )
                    }
                } catch (t: Throwable) {
                    mainPoster { job.done(AuraFxResult.Err(AuraFxError.GpuFailure("Photo encode failed", t))) }
                }
            }
        } catch (t: Throwable) {
            AuraFxLog.e("Processed photo capture failed", t)
            mainPoster { job.done(AuraFxResult.Err(AuraFxError.GpuFailure("Processed photo capture failed", t))) }
        }
    }

    private fun drainTexture() {
        try {
            egl?.makeCurrent(pbuffer)
            surfaceTexture?.updateTexImage()
        } catch (_: Throwable) {
            // SurfaceTexture may already be released during teardown.
        }
    }

    private fun presentExternal(frame: AuraFxInputFrame) {
        if (windowSurface == EGL14.EGL_NO_SURFACE) {
            performance.onDropped()
            return
        }
        if (!pipeline.tryBegin()) {
            performance.onDropped()
            return
        }
        val started = System.nanoTime()
        try {
            val eglCore = egl ?: return
            eglCore.makeCurrent(windowSurface)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cpuTextureId)
            val buffer: ByteBuffer = frame.rgba8888.duplicate()
            buffer.rewind()
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RGBA,
                frame.width,
                frame.height,
                0,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                buffer,
            )
            GLES30.glViewport(0, 0, viewportW, viewportH)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            blit.draw2d(cpuTextureId, mirrorX = false)
            val extCtx = FrameContext(
                timestampNs = frame.timestampNs,
                width = viewportW,
                height = viewportH,
                inputTextureId = cpuTextureId,
                inputIsOes = false,
                texMatrix = FloatArray(16),
                lensFacing = facing,
            )
            extCtx.processedTextureId = cpuTextureId
            presentCaptureSinks(eglCore, extCtx, frame.timestampNs)
            eglCore.makeCurrent(windowSurface)
            eglCore.swapBuffers(windowSurface)
            performance.onFramePresented(
                frame.timestampNs,
                System.nanoTime() - started,
                gpuNs = null,
                ingress = FrameIngress.PROCESS_FRAME,
                admitted = pipeline.admittedCount(),
            )
        } catch (t: Throwable) {
            postError(AuraFxError.GpuFailure("processFrame upload failed", t))
        } finally {
            pipeline.end()
        }
    }

    private fun detachWindowLocked() {
        val eglCore = egl ?: return
        if (windowSurface != EGL14.EGL_NO_SURFACE) {
            eglCore.makeNothingCurrent()
            eglCore.destroySurface(windowSurface)
            windowSurface = EGL14.EGL_NO_SURFACE
        }
        outputSurface = null
    }

    private fun teardownGl() {
        AuraFxLog.i("GL teardown")
        try {
            recorder.releaseQuiet()
        } catch (_: Throwable) {
        }
        try {
            captureFbo.release()
        } catch (_: Throwable) {
        }
        try {
            effects.detachAll()
        } catch (_: Throwable) {
        }
        try {
            cameraSurface?.release()
            surfaceTexture?.setOnFrameAvailableListener(null)
            surfaceTexture?.release()
        } catch (_: Throwable) {
        }
        cameraSurface = null
        surfaceTexture = null
        try {
            gpuTimer.release()
            blit.release()
            if (oesTextureId != 0 || cpuTextureId != 0) {
                GLES30.glDeleteTextures(2, intArrayOf(oesTextureId, cpuTextureId), 0)
            }
        } catch (_: Throwable) {
        }
        oesTextureId = 0
        cpuTextureId = 0
        try {
            detachWindowLocked()
            val eglCore = egl
            if (eglCore != null) {
                if (pbuffer != EGL14.EGL_NO_SURFACE) {
                    eglCore.destroySurface(pbuffer)
                    pbuffer = EGL14.EGL_NO_SURFACE
                }
                eglCore.release()
            }
        } catch (_: Throwable) {
        }
        egl = null
        photoIo.shutdown()
    }

    private fun postError(error: AuraFxError) {
        mainPoster { listener?.onError(error) }
    }
}
