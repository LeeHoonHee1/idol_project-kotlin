package com.example.idolproject.Calendar

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

class MultiDotSpan(
    private val radius: Float,
    private val colors: List<Int>
) : LineBackgroundSpan {

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNum: Int
    ) {
        val dotColors = colors.take(3)
        if (dotColors.isEmpty()) return

        val oldColor = paint.color
        val oldAntiAlias = paint.isAntiAlias

        val spacing = radius * 3f
        val totalWidth = if (dotColors.size == 1) 0f else spacing * (dotColors.size - 1)
        var startX = (left + right) / 2f - totalWidth / 2f
        val centerY = bottom + radius * 1.8f

        dotColors.forEach { color ->
            paint.color = color
            paint.isAntiAlias = true
            canvas.drawCircle(startX, centerY, radius, paint)
            startX += spacing
        }

        paint.color = oldColor
        paint.isAntiAlias = oldAntiAlias
    }
}