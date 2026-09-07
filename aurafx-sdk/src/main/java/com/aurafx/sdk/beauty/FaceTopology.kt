package com.aurafx.sdk.beauty

/**
 * MediaPipe Face Mesh canonical indices (468 + optional iris 468..477).
 * Contours are wound for filled-polygon masks.
 */
object FaceTopology {
    const val LANDMARK_COUNT = 468
    const val IRIS_COUNT = 10

    val FACE_OVAL = intArrayOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365,
        379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93,
        234, 127, 162, 21, 54, 103, 67, 109,
    )

    val LIPS_OUTER = intArrayOf(
        61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409, 270, 269, 267,
        0, 37, 39, 40, 185,
    )
    val LIPS_INNER = intArrayOf(
        78, 95, 88, 178, 87, 14, 317, 402, 318, 324, 308, 415, 310, 311, 312,
        13, 82, 81, 80, 191,
    )

    val LEFT_EYE = intArrayOf(
        33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246,
    )
    val RIGHT_EYE = intArrayOf(
        362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398,
    )

    val LEFT_BROW = intArrayOf(70, 63, 105, 66, 107, 55, 65, 52, 53, 46)
    val RIGHT_BROW = intArrayOf(300, 293, 334, 296, 336, 285, 295, 282, 283, 276)

    val LEFT_IRIS = intArrayOf(468, 469, 470, 471, 472)
    val RIGHT_IRIS = intArrayOf(473, 474, 475, 476, 477)

    val NOSE_BRIDGE = intArrayOf(6, 197, 195, 5, 4, 1, 19, 94, 2)
    val NOSE_TIP = 1
    val NOSE_BOTTOM = 2
    val LEFT_ALA = 98
    val RIGHT_ALA = 327
    val NOSTRIL_LEFT = 48
    val NOSTRIL_RIGHT = 278

    val CHIN = 152
    val FOREHEAD = 10
    val LEFT_JAW = 234
    val RIGHT_JAW = 454

    val LEFT_CHEEK = intArrayOf(50, 101, 36, 205, 206, 207, 187)
    val RIGHT_CHEEK = intArrayOf(280, 330, 266, 425, 426, 427, 411)

    val JAW_LEFT = intArrayOf(132, 58, 172, 136, 150, 149, 176, 148)
    val JAW_RIGHT = intArrayOf(361, 288, 397, 365, 379, 378, 400, 377)

    val MOUTH_CORNERS = intArrayOf(61, 291)
    val MOUTH_CENTER = 13

    val LEFT_EYE_CENTER = 468
    val RIGHT_EYE_CENTER = 473
    val LEFT_EYE_INNER = 133
    val LEFT_EYE_OUTER = 33
    val RIGHT_EYE_INNER = 362
    val RIGHT_EYE_OUTER = 263

    /** Lower-lid to upper-cheek pocket used for under-eye circles. */
    val LEFT_UNDER_EYE = intArrayOf(111, 117, 118, 119, 120, 121, 128, 245, 193, 221, 222, 223, 224, 225, 113, 247, 30, 29, 27, 28, 56, 190, 243, 112, 26, 22, 23, 24, 110, 25)
    val RIGHT_UNDER_EYE = intArrayOf(340, 346, 347, 348, 349, 350, 357, 465, 417, 441, 442, 443, 444, 445, 342, 467, 260, 259, 257, 258, 286, 414, 463, 341, 256, 252, 253, 254, 339, 255)

    fun validIndex(index: Int, count: Int): Boolean = index >= 0 && index < count
}
