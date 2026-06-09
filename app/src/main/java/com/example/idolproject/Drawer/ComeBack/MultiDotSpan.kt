package com.example.idolproject.Drawer.ComeBack

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

/**
 * 한 날짜 아래에 최대 3개의 점을 수평으로 그려주는 Span
 * 점이 여러 개일 때, 각 점의 색을 다르게 표시
 */
class MultiDotSpan(
    private val radius: Float,
    private val baseColor: Int,
    private val count: Int
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
        val maxDots = count.coerceAtMost(3)
        if (maxDots <= 0) return

        val oldColor = paint.color

        val centerX = (left + right) / 2f
        val centerY = bottom + radius * 1.5f

        val gap = radius * 2.8f
        val totalWidth = gap * (maxDots - 1)

        val palette = arrayOf(
            baseColor,                    // 1번째 점
            Color.parseColor("#2196F3"),  // 2번째 점: 파랑 느낌
            Color.parseColor("#9C27B0")   // 3번째 점: 보라 느낌
        )

        for (i in 0 until maxDots) {
            val x = centerX - totalWidth / 2f + i * gap
            paint.color = palette.getOrElse(i) { baseColor }
            canvas.drawCircle(x, centerY, radius, paint)
        }

        paint.color = oldColor
    }
}