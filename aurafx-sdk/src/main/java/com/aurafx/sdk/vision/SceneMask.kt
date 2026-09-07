package com.aurafx.sdk.vision

object SelfieClass {
    const val BACKGROUND = 0
    const val HAIR = 1
    const val BODY_SKIN = 2
    const val FACE_SKIN = 3
    const val CLOTHES = 4
    const val OTHER = 5
}

object SceneMaskPacker {
    fun packCategory(
        width: Int,
        height: Int,
        category: ByteArray,
        mirrorX: Boolean,
        previous: ByteArray? = null,
        smooth: Float = 0.35f,
    ): SegmentationMask {
        require(category.size >= width * height)
        val out = ByteArray(width * height * 4)
        var personSum = 0
        var hairSum = 0
        var bodySum = 0
        var faceSum = 0
        val a = smooth.coerceIn(0f, 0.9f)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val sx = if (mirrorX) width - 1 - x else x
                val cat = category[y * width + sx].toInt() and 0xff
                val person = if (cat != SelfieClass.BACKGROUND) 255 else 0
                val hair = if (cat == SelfieClass.HAIR) 255 else 0
                val face = if (cat == SelfieClass.FACE_SKIN) 255 else 0
                val body = if (cat == SelfieClass.BODY_SKIN || cat == SelfieClass.CLOTHES) 255 else 0
                val o = (y * width + x) * 4
                var p = person
                var h = hair
                var b = body
                var f = face
                if (previous != null && previous.size == out.size) {
                    p = mixU8(previous[o], p, a)
                    h = mixU8(previous[o + 1], h, a)
                    b = mixU8(previous[o + 2], b, a)
                    f = mixU8(previous[o + 3], f, a)
                }
                // Keep hair off face-skin so recolor does not paint forehead skin.
                if (f > 80) h = (h * (255 - f) / 255)
                out[o] = p.toByte()
                out[o + 1] = h.toByte()
                out[o + 2] = b.toByte()
                out[o + 3] = f.toByte()
                personSum += p
                hairSum += h
                bodySum += b
                faceSum += f
            }
        }
        val n = (width * height * 255f).coerceAtLeast(1f)
        return SegmentationMask(
            width = width,
            height = height,
            packedRgba = out,
            source = "mediapipe-selfie-multiclass",
            personCoverage = personSum / n,
            hairCoverage = hairSum / n,
            bodyCoverage = bodySum / n,
            faceCoverage = faceSum / n,
        )
    }

    fun packConfidence(
        width: Int,
        height: Int,
        background: FloatArray,
        hair: FloatArray,
        bodySkin: FloatArray,
        faceSkin: FloatArray,
        clothes: FloatArray,
        mirrorX: Boolean,
        previous: ByteArray? = null,
        smooth: Float = 0.35f,
    ): SegmentationMask {
        val category = ByteArray(width * height)
        for (i in category.indices) {
            val scores = floatArrayOf(
                background.getOrElse(i) { 0f },
                hair.getOrElse(i) { 0f },
                bodySkin.getOrElse(i) { 0f },
                faceSkin.getOrElse(i) { 0f },
                clothes.getOrElse(i) { 0f },
                0f,
            )
            var best = 0
            var bestV = scores[0]
            for (c in 1 until 5) {
                if (scores[c] > bestV) {
                    bestV = scores[c]
                    best = c
                }
            }
            category[i] = best.toByte()
        }
        return packCategory(width, height, category, mirrorX, previous, smooth)
    }

    fun channelMean(mask: SegmentationMask, channel: Int): Float {
        var s = 0
        var i = channel
        while (i < mask.packedRgba.size) {
            s += mask.packedRgba[i].toInt() and 0xff
            i += 4
        }
        return s / (mask.width * mask.height * 255f)
    }

    private fun mixU8(prev: Byte, cur: Int, a: Float): Int {
        val p = prev.toInt() and 0xff
        return (p * a + cur * (1f - a)).toInt().coerceIn(0, 255)
    }
}

object PoseIndex {
    const val COUNT = 33
    const val NOSE = 0
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
}
