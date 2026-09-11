package com.aurafx.deviceui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.roundToInt

/**
 * Lays itself out as the largest 9:16 rectangle that fits the parent measure specs.
 */
internal class PortraitPreviewFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    var aspectWidth = 9
    var aspectHeight = 16

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxW = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(1)
        val maxH = MeasureSpec.getSize(heightMeasureSpec).coerceAtLeast(1)
        val target = aspectWidth.toFloat() / aspectHeight.toFloat()
        val host = maxW.toFloat() / maxH.toFloat()
        val w: Int
        val h: Int
        if (host > target) {
            h = maxH
            w = (h * target).roundToInt().coerceAtLeast(1)
        } else {
            w = maxW
            h = (w / target).roundToInt().coerceAtLeast(1)
        }
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY),
        )
    }
}
