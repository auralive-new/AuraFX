package com.aurafx.sdk.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.net.Uri
import com.aurafx.sdk.api.AuraFxError
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.performance.PerformanceManager
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

data class EditorFeature(
    val id: String,
    val available: Boolean,
    val note: String,
)

object EditorCapabilities {
    val features: List<EditorFeature> = listOf(
        EditorFeature("import.photo", true, "JPEG/PNG via content URI"),
        EditorFeature("import.video", true, "MP4 via content URI"),
        EditorFeature("preview", true, "Decoded still or first video frame"),
        EditorFeature("timeline", true, "trimStartUs/trimEndUs/speed"),
        EditorFeature("trim", true, "Sample-accurate copy inside the selected window"),
        EditorFeature("photo.crop", true, "Normalized crop on stills"),
        EditorFeature("photo.rotate", true, "90-degree steps on stills"),
        EditorFeature("video.rotate", true, "Muxer orientation hint only; pixels are not resampled"),
        EditorFeature("speed", true, "Video timestamps scaled on export"),
        EditorFeature("audio.music", true, "Replacement AAC/M4A track mux; not a ducked mix"),
        EditorFeature("video.crop", false, "Unavailable: needs decode-scale-encode"),
        EditorFeature("video.effectReprocess", false, "Unavailable: would be a second live frame producer"),
        EditorFeature("export", true, "JPEG still or MP4 copy/transform"),
    )

    fun available(id: String): Boolean = features.firstOrNull { it.id == id }?.available == true
}

data class EditorTimeline(
    var trimStartUs: Long = 0L,
    var trimEndUs: Long = 0L,
    var speed: Float = 1f,
    var rotateDegrees: Int = 0,
    var cropLeft: Float = 0f,
    var cropTop: Float = 0f,
    var cropRight: Float = 1f,
    var cropBottom: Float = 1f,
    var musicUri: Uri? = null,
) {
    fun clamp(durationUs: Long) {
        if (durationUs <= 0L) return
        trimStartUs = trimStartUs.coerceIn(0L, durationUs)
        if (trimEndUs <= trimStartUs) trimEndUs = durationUs
        trimEndUs = trimEndUs.coerceIn(trimStartUs + 1_000L, durationUs)
        speed = speed.coerceIn(0.25f, 4f)
        rotateDegrees = ((rotateDegrees / 90) * 90).let { d -> ((d % 360) + 360) % 360 }
        cropLeft = cropLeft.coerceIn(0f, 0.95f)
        cropTop = cropTop.coerceIn(0f, 0.95f)
        cropRight = cropRight.coerceIn(cropLeft + 0.05f, 1f)
        cropBottom = cropBottom.coerceIn(cropTop + 0.05f, 1f)
    }
}

data class EditorClip(
    val uri: Uri,
    val isVideo: Boolean,
    val durationUs: Long,
    val width: Int,
    val height: Int,
    val mime: String,
    val timeline: EditorTimeline = EditorTimeline(),
)

class AuraFxEditor(
    context: Context,
    private val performance: PerformanceManager = PerformanceManager(),
) {
    private val app = context.applicationContext
    var clip: EditorClip? = null
        private set

    fun import(uri: Uri): AuraFxResult<EditorClip> {
        return try {
            val cr = app.contentResolver
            val type = cr.getType(uri) ?: ""
            val isVideo = type.startsWith("video/") || uri.toString().contains(".mp4", ignoreCase = true)
            if (isVideo) {
                val extractor = MediaExtractor()
                extractor.setDataSource(app, uri, null)
                var duration = 0L
                var w = 0
                var h = 0
                var mime = "video/mp4"
                for (i in 0 until extractor.trackCount) {
                    val fmt = extractor.getTrackFormat(i)
                    val m = fmt.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                    if (m.startsWith("video/")) {
                        mime = m
                        duration = if (fmt.containsKey(android.media.MediaFormat.KEY_DURATION)) {
                            fmt.getLong(android.media.MediaFormat.KEY_DURATION)
                        } else 0L
                        w = fmt.getInteger(android.media.MediaFormat.KEY_WIDTH)
                        h = fmt.getInteger(android.media.MediaFormat.KEY_HEIGHT)
                    }
                }
                extractor.release()
                val imported = EditorClip(uri, true, duration, w, h, mime).also {
                    it.timeline.trimEndUs = duration
                    it.timeline.clamp(duration)
                }
                clip = imported
                AuraFxResult.Ok(imported)
            } else {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                val imported = EditorClip(
                    uri, false, 0L, opts.outWidth, opts.outHeight, type.ifBlank { "image/jpeg" },
                )
                clip = imported
                AuraFxResult.Ok(imported)
            }
        } catch (t: Throwable) {
            AuraFxResult.Err(AuraFxError.InvalidState("Import failed: ${t.message}"))
        }
    }

    fun applyTimeline(block: EditorTimeline.() -> Unit): AuraFxResult<EditorClip> {
        val current = clip ?: return AuraFxResult.Err(AuraFxError.InvalidState("No clip imported"))
        block(current.timeline)
        current.timeline.clamp(if (current.isVideo) current.durationUs else 1_000_000L)
        return AuraFxResult.Ok(current)
    }

    fun previewBitmap(): AuraFxResult<Bitmap> {
        val current = clip ?: return AuraFxResult.Err(AuraFxError.InvalidState("No clip imported"))
        return try {
            if (current.isVideo) {
                AuraFxResult.Err(
                    AuraFxError.InvalidState("Video frame preview is host-owned (VideoView). Editor does not decode GOP frames here."),
                )
            } else {
                val bmp = app.contentResolver.openInputStream(current.uri)?.use { BitmapFactory.decodeStream(it) }
                    ?: return AuraFxResult.Err(AuraFxError.InvalidState("Could not decode photo"))
                AuraFxResult.Ok(transformStill(bmp, current.timeline))
            }
        } catch (t: Throwable) {
            AuraFxResult.Err(AuraFxError.GpuFailure("Preview decode failed", t))
        }
    }

    fun export(dest: File): AuraFxResult<File> {
        val current = clip ?: return AuraFxResult.Err(AuraFxError.InvalidState("No clip imported"))
        val started = System.nanoTime()
        val result = if (current.isVideo) exportVideo(current, dest) else exportPhoto(current, dest)
        if (result is AuraFxResult.Ok) {
            performance.markExportNs(System.nanoTime() - started)
        }
        return result
    }

    private fun exportPhoto(clip: EditorClip, dest: File): AuraFxResult<File> {
        return try {
            val src = app.contentResolver.openInputStream(clip.uri)?.use { BitmapFactory.decodeStream(it) }
                ?: return AuraFxResult.Err(AuraFxError.InvalidState("Could not decode photo"))
            val out = transformStill(src, clip.timeline)
            dest.parentFile?.mkdirs()
            FileOutputStream(dest).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            if (out !== src) src.recycle()
            AuraFxResult.Ok(dest)
        } catch (t: Throwable) {
            AuraFxResult.Err(AuraFxError.InvalidState("Photo export failed: ${t.message}"))
        }
    }

    private fun transformStill(src: Bitmap, timeline: EditorTimeline): Bitmap {
        val w = src.width
        val h = src.height
        val l = (timeline.cropLeft * w).toInt().coerceIn(0, w - 2)
        val t = (timeline.cropTop * h).toInt().coerceIn(0, h - 2)
        val r = (timeline.cropRight * w).toInt().coerceIn(l + 1, w)
        val b = (timeline.cropBottom * h).toInt().coerceIn(t + 1, h)
        var bmp = Bitmap.createBitmap(src, l, t, r - l, b - t)
        if (timeline.rotateDegrees != 0) {
            val m = Matrix()
            m.postRotate(timeline.rotateDegrees.toFloat())
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated !== bmp) bmp.recycle()
            bmp = rotated
        }
        return bmp
    }

    private fun exportVideo(clip: EditorClip, dest: File): AuraFxResult<File> {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        val musicExtractor = if (clip.timeline.musicUri != null) MediaExtractor() else null
        return try {
            extractor.setDataSource(app, clip.uri, null)
            dest.parentFile?.mkdirs()
            val m = MediaMuxer(dest.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = m
            if (clip.timeline.rotateDegrees != 0) {
                m.setOrientationHint(clip.timeline.rotateDegrees)
            }
            val map = IntArray(extractor.trackCount) { -1 }
            val replaceAudio = clip.timeline.musicUri != null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (replaceAudio && mime.startsWith("audio/")) continue
                extractor.selectTrack(i)
                map[i] = m.addTrack(fmt)
            }
            var musicTrack = -1
            val musicUri = clip.timeline.musicUri
            if (musicExtractor != null && musicUri != null) {
                musicExtractor.setDataSource(app, musicUri, null)
                for (i in 0 until musicExtractor.trackCount) {
                    val mime = musicExtractor.getTrackFormat(i).getString(android.media.MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        musicExtractor.selectTrack(i)
                        musicTrack = m.addTrack(musicExtractor.getTrackFormat(i))
                        break
                    }
                }
            }
            m.start()
            copySamples(extractor, m, map, clip.timeline)
            if (musicExtractor != null && musicTrack >= 0) {
                copyMusic(musicExtractor, m, musicTrack, clip.timeline)
            }
            m.stop()
            m.release()
            muxer = null
            extractor.release()
            musicExtractor?.release()
            AuraFxResult.Ok(dest)
        } catch (t: Throwable) {
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
            try {
                musicExtractor?.release()
            } catch (_: Throwable) {
            }
            try {
                muxer?.release()
            } catch (_: Throwable) {
            }
            AuraFxResult.Err(AuraFxError.InvalidState("Video export failed: ${t.message}"))
        }
    }

    private fun copySamples(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        map: IntArray,
        timeline: EditorTimeline,
    ) {
        val info = MediaCodec.BufferInfo()
        val buffer = java.nio.ByteBuffer.allocate(1 shl 20)
        val startUs = timeline.trimStartUs
        val endUs = if (timeline.trimEndUs > 0L) timeline.trimEndUs else Long.MAX_VALUE
        val speed = max(0.25f, timeline.speed)
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        while (true) {
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            val time = extractor.sampleTime
            if (time > endUs) break
            if (time >= startUs) {
                info.offset = 0
                info.size = size
                info.flags = extractor.sampleFlags
                info.presentationTimeUs = ((time - startUs) / speed).toLong()
                val outTrack = map.getOrNull(extractor.sampleTrackIndex) ?: -1
                if (outTrack >= 0) muxer.writeSampleData(outTrack, buffer, info)
            }
            extractor.advance()
        }
    }

    private fun copyMusic(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        track: Int,
        timeline: EditorTimeline,
    ) {
        val info = MediaCodec.BufferInfo()
        val buffer = java.nio.ByteBuffer.allocate(1 shl 18)
        val window = (timeline.trimEndUs - timeline.trimStartUs).coerceAtLeast(1_000L)
        val speed = max(0.25f, timeline.speed)
        val maxUs = (window / speed).toLong()
        while (true) {
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            val time = extractor.sampleTime
            if (time > maxUs) break
            info.offset = 0
            info.size = size
            info.flags = extractor.sampleFlags
            info.presentationTimeUs = time
            muxer.writeSampleData(track, buffer, info)
            extractor.advance()
        }
    }
}
