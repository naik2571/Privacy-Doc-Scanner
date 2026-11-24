package com.docscan.privacy.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class GraphicOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#14B8A6") // Neon Teal
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, 20, 184, 166)
        style = Paint.Style.FILL
    }

    private val boundingBoxes = mutableListOf<RectF>()

    fun setBoundingBoxes(boxes: List<Rect>) {
        boundingBoxes.clear()
        for (b in boxes) {
            boundingBoxes.add(RectF(b))
        }
        postInvalidate()
    }

    fun clear() {
        boundingBoxes.clear()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in boundingBoxes) {
            canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
            canvas.drawRoundRect(rect, 6f, 6f, boxPaint)
        }
    }
}
