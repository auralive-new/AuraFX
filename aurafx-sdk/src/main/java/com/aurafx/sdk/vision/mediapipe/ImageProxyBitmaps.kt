package com.aurafx.sdk.vision.mediapipe

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

internal object ImageProxyBitmaps {
    fun copy(image: ImageProxy, reusable: Bitmap?): Bitmap {
        val yPlane = image.planes[0]
        val w = image.width
        val h = image.height
        val bmp = reusable?.takeIf { it.width == w && it.height == h && !it.isRecycled }
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        if (image.format == android.graphics.ImageFormat.FLEX_RGBA_8888 || image.planes.size == 1) {
            val buffer = yPlane.buffer.duplicate()
            buffer.rewind()
            if (yPlane.pixelStride == 4 && yPlane.rowStride == w * 4) {
                bmp.copyPixelsFromBuffer(buffer)
            } else {
                copyStridedRgba(buffer, yPlane.rowStride, yPlane.pixelStride, w, h, bmp)
            }
        } else {
            yuv420ToBitmap(image, bmp)
        }
        val rotation = image.imageInfo.rotationDegrees
        if (rotation == 0) return bmp
        val m = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    private fun copyStridedRgba(buffer: ByteBuffer, rowStride: Int, pixelStride: Int, w: Int, h: Int, bmp: Bitmap) {
        val row = ByteArray(rowStride)
        val argb = IntArray(w * h)
        for (rowI in 0 until h) {
            buffer.position(rowI * rowStride)
            buffer.get(row, 0, minOf(row.size, rowStride))
            for (col in 0 until w) {
                val o = col * pixelStride
                val r = row[o].toInt() and 0xff
                val g = row[o + 1].toInt() and 0xff
                val b = row[o + 2].toInt() and 0xff
                val a = if (pixelStride > 3) row[o + 3].toInt() and 0xff else 255
                argb[rowI * w + col] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bmp.setPixels(argb, 0, w, 0, 0, w, h)
    }

    private fun yuv420ToBitmap(image: ImageProxy, out: Bitmap) {
        val y = image.planes[0]
        val u = image.planes[1]
        val v = image.planes[2]
        val w = image.width
        val h = image.height
        val pixels = IntArray(w * h)
        val yBuf = y.buffer
        val uBuf = u.buffer
        val vBuf = v.buffer
        for (row in 0 until h) {
            for (col in 0 until w) {
                val yi = yBuf.get(row * y.rowStride + col * y.pixelStride).toInt() and 0xff
                val ui = uBuf.get((row / 2) * u.rowStride + (col / 2) * u.pixelStride).toInt() and 0xff
                val vi = vBuf.get((row / 2) * v.rowStride + (col / 2) * v.pixelStride).toInt() and 0xff
                val yp = yi - 16
                val up = ui - 128
                val vp = vi - 128
                val r = (1.164f * yp + 1.596f * vp).toInt().coerceIn(0, 255)
                val g = (1.164f * yp - 0.392f * up - 0.813f * vp).toInt().coerceIn(0, 255)
                val b = (1.164f * yp + 2.017f * up).toInt().coerceIn(0, 255)
                pixels[row * w + col] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
    }
}
