package com.aurafx.sdk.capture

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

internal object ProcessedJpegWriter {
    fun writeFlippedRgba(width: Int, height: Int, rgba: ByteBuffer, dest: File, quality: Int = 92) {
        rgba.rewind()
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(rgba)
        val matrix = Matrix()
        matrix.preScale(1f, -1f)
        val flipped = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true)
        if (flipped !== bmp) bmp.recycle()
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { out ->
            flipped.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        flipped.recycle()
    }
}
