package com.aurafx.sdk.capture

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.os.Process
import android.view.Surface
import com.aurafx.sdk.api.RecordedVideo
import com.aurafx.sdk.internal.AuraFxLog
import com.aurafx.sdk.internal.render.BlitProgram
import com.aurafx.sdk.internal.render.EglCore
import com.aurafx.sdk.performance.PerformanceManager
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Encodes the already-processed GPU texture via a MediaCodec input Surface.
 * No per-frame CPU readback. Audio is optional and skipped if the mic cannot start.
 */
internal class ProcessedVideoRecorder(
    private val performance: PerformanceManager,
) {
    private val running = AtomicBoolean(false)
    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var encoderSurface: Surface? = null
    private var encoderEgl: EglCore? = null
    private var encoderEglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var videoTrack = -1
    private var audioTrack = -1
    private var muxerStarted = false
    private var width = 0
    private var height = 0
    private var dest: File? = null
    private var firstVideoNs = 0L
    private var lastVideoUs = -1L
    private var frameCount = 0L
    private var encodeDropped = 0L
    private var audioRecord: AudioRecord? = null
    private var audioEncoder: MediaCodec? = null
    private var audioThread: Thread? = null
    private val audioStop = AtomicBoolean(false)
    private var hasAudio = false
    private val muxLock = Any()
    private val bufferInfo = MediaCodec.BufferInfo()
    private var audioStartedNs = 0L

    fun isRunning(): Boolean = running.get()

    fun start(egl: EglCore, file: File, w: Int, h: Int, recordAudio: Boolean) {
        if (!running.compareAndSet(false, true)) {
            throw IllegalStateException("Recorder already running")
        }
        dest = file
        width = w and 1.inv()
        height = h and 1.inv()
        if (width < 16 || height < 16) {
            running.set(false)
            throw IllegalStateException("Encoder size ${w}x${h} is too small")
        }
        file.parentFile?.mkdirs()
        firstVideoNs = 0L
        lastVideoUs = -1L
        frameCount = 0L
        encodeDropped = 0L
        muxerStarted = false
        videoTrack = -1
        audioTrack = -1
        hasAudio = false
        audioStop.set(false)

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, (width * height * 4).coerceIn(2_000_000, 10_000_000))
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = codec.createInputSurface()
        codec.start()
        encoder = codec
        encoderSurface = surface
        encoderEgl = egl
        encoderEglSurface = egl.createWindowSurface(surface)
        muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        if (recordAudio) {
            tryStartAudio()
        }
        AuraFxLog.i("Processed video recorder start ${width}x${height} audio=$hasAudio file=${file.name}")
    }

    fun drawProcessed(
        egl: EglCore,
        blit: BlitProgram,
        textureId: Int,
        isOes: Boolean,
        texMatrix: FloatArray,
        timestampNs: Long,
    ) {
        if (!running.get()) return
        val surface = encoderEglSurface
        if (surface == EGL14.EGL_NO_SURFACE) return
        val started = System.nanoTime()
        try {
            egl.makeCurrent(surface)
            android.opengl.GLES30.glViewport(0, 0, width, height)
            android.opengl.GLES30.glClear(android.opengl.GLES30.GL_COLOR_BUFFER_BIT)
            if (isOes) blit.drawOes(textureId, texMatrix, mirrorX = false) else blit.draw2d(textureId, mirrorX = false)
            if (firstVideoNs == 0L) firstVideoNs = timestampNs
            var ptsUs = AvTimestamps.relativeUs(timestampNs, firstVideoNs)
            ptsUs = AvTimestamps.bumpIfNeeded(lastVideoUs, ptsUs)
            lastVideoUs = ptsUs
            EGLExt.eglPresentationTimeANDROID(egl.display, surface, timestampNs)
            egl.swapBuffers(surface)
            drainVideo(endOfStream = false)
            frameCount += 1
            performance.onEncoderFrame(System.nanoTime() - started)
        } catch (t: Throwable) {
            encodeDropped += 1
            performance.onEncoderDropped()
            AuraFxLog.e("Encoder draw failed", t)
        }
    }

    fun stop(): RecordedVideo {
        val file = dest ?: throw IllegalStateException("Recorder was not started")
        audioStop.set(true)
        try {
            audioThread?.join(1_500)
        } catch (_: Throwable) {
        }
        audioThread = null
        try {
            encoder?.signalEndOfInputStream()
        } catch (_: Throwable) {
        }
        try {
            drainVideo(endOfStream = true)
        } catch (_: Throwable) {
        }
        try {
            drainAudio(endOfStream = true)
        } catch (_: Throwable) {
        }
        releaseCodecs()
        running.set(false)
        return RecordedVideo(
            file = file,
            width = width,
            height = height,
            durationUs = lastVideoUs.coerceAtLeast(0L),
            hasAudio = hasAudio,
            frameCount = frameCount,
        )
    }

    fun releaseQuiet() {
        if (!running.get() && encoder == null) return
        audioStop.set(true)
        try {
            audioThread?.join(500)
        } catch (_: Throwable) {
        }
        releaseCodecs()
        running.set(false)
    }

    private fun releaseCodecs() {
        try {
            audioRecord?.stop()
        } catch (_: Throwable) {
        }
        try {
            audioRecord?.release()
        } catch (_: Throwable) {
        }
        audioRecord = null
        try {
            audioEncoder?.stop()
        } catch (_: Throwable) {
        }
        try {
            audioEncoder?.release()
        } catch (_: Throwable) {
        }
        audioEncoder = null
        try {
            encoder?.stop()
        } catch (_: Throwable) {
        }
        try {
            encoder?.release()
        } catch (_: Throwable) {
        }
        encoder = null
        try {
            val core = encoderEgl
            if (core != null && encoderEglSurface != EGL14.EGL_NO_SURFACE) {
                core.destroySurface(encoderEglSurface)
            }
        } catch (_: Throwable) {
        }
        encoderEglSurface = EGL14.EGL_NO_SURFACE
        encoderEgl = null
        try {
            encoderSurface?.release()
        } catch (_: Throwable) {
        }
        encoderSurface = null
        synchronized(muxLock) {
            try {
                if (muxerStarted) muxer?.stop()
            } catch (_: Throwable) {
            }
            try {
                muxer?.release()
            } catch (_: Throwable) {
            }
            muxer = null
            muxerStarted = false
        }
    }

    private fun drainVideo(endOfStream: Boolean) {
        val codec = encoder ?: return
        var spins = 0
        while (true) {
            val idx = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000 else 0)
            when {
                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                    spins += 1
                    if (spins > 48) return
                }
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    synchronized(muxLock) {
                        videoTrack = muxer?.addTrack(codec.outputFormat) ?: -1
                        maybeStartMuxer()
                    }
                }
                idx >= 0 -> {
                    val encoded = codec.getOutputBuffer(idx)
                    if (encoded != null && bufferInfo.size > 0 && muxerStarted &&
                        bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                    ) {
                        encoded.position(bufferInfo.offset)
                        encoded.limit(bufferInfo.offset + bufferInfo.size)
                        synchronized(muxLock) { muxer?.writeSampleData(videoTrack, encoded, bufferInfo) }
                    }
                    codec.releaseOutputBuffer(idx, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    if (!endOfStream) return
                }
                else -> return
            }
        }
    }

    private fun drainAudio(endOfStream: Boolean) {
        val codec = audioEncoder ?: return
        val info = MediaCodec.BufferInfo()
        var spins = 0
        while (spins++ < 32) {
            val idx = codec.dequeueOutputBuffer(info, if (endOfStream) 5_000 else 0)
            if (idx == MediaCodec.INFO_TRY_AGAIN_LATER) return
            if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                synchronized(muxLock) {
                    audioTrack = muxer?.addTrack(codec.outputFormat) ?: -1
                    maybeStartMuxer()
                }
                continue
            }
            if (idx < 0) return
            val buf = codec.getOutputBuffer(idx)
            if (buf != null && info.size > 0 && muxerStarted &&
                info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
            ) {
                buf.position(info.offset)
                buf.limit(info.offset + info.size)
                synchronized(muxLock) { muxer?.writeSampleData(audioTrack, buf, info) }
            }
            codec.releaseOutputBuffer(idx, false)
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
        }
    }

    private fun maybeStartMuxer() {
        val mux = muxer ?: return
        if (muxerStarted) return
        val needAudio = hasAudio
        if (videoTrack < 0) return
        if (needAudio && audioTrack < 0) return
        mux.start()
        muxerStarted = true
    }

    private fun tryStartAudio() {
        val sampleRate = 44100
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) return
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.CAMCORDER,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2,
            )
        } catch (t: Throwable) {
            AuraFxLog.w("AudioRecord unavailable", t)
            return
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }
        val aac = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 96_000)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBuf)
        aac.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        aac.start()
        audioEncoder = aac
        audioRecord = record
        hasAudio = true
        audioStartedNs = System.nanoTime()
        lastAudioUs = -1L
        audioThread = Thread({ audioLoop(record, aac, sampleRate) }, "aurafx-audio").also {
            it.priority = Process.THREAD_PRIORITY_AUDIO
            it.start()
        }
    }

    private var lastAudioUs = -1L

    private fun audioLoop(record: AudioRecord, codec: MediaCodec, sampleRate: Int) {
        try {
            record.startRecording()
        } catch (t: Throwable) {
            AuraFxLog.w("Audio startRecording failed; continuing video-only", t)
            hasAudio = false
            return
        }
        val pcm = ByteArray(2048)
        var inputEos = false
        while (!audioStop.get() || !inputEos) {
            if (!inputEos) {
                val n = try {
                    record.read(pcm, 0, pcm.size)
                } catch (_: Throwable) {
                    -1
                }
                val inIdx = codec.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val inBuf = codec.getInputBuffer(inIdx)
                    if (n > 0 && inBuf != null) {
                        inBuf.clear()
                        inBuf.put(pcm, 0, n)
                        var pts = AvTimestamps.relativeUs(System.nanoTime(), audioStartedNs)
                        pts = AvTimestamps.bumpIfNeeded(lastAudioUs, pts)
                        lastAudioUs = pts
                        codec.queueInputBuffer(inIdx, 0, n, pts, 0)
                    } else if (audioStop.get()) {
                        codec.queueInputBuffer(inIdx, 0, 0, lastAudioUs.coerceAtLeast(0L), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputEos = true
                    } else if (inBuf != null) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, 0)
                    }
                }
            }
            drainAudio(endOfStream = false)
            if (audioStop.get() && inputEos) break
        }
        drainAudio(endOfStream = true)
    }
}
