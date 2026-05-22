package dev.brahmkshatriya.echo.ui.player.more.lyrics

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class WordHighlightSpan(
    private val startTime: Long,
    private val endTime: Long,
    private val activeColor: Int,
    private val inactiveColor: Int
) : ReplacementSpan() {

    var currentProgress: Long = 0L

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        if (fm != null) {
            paint.getFontMetricsInt(fm)
        }
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val width = paint.measureText(text, start, end)
        val ratio = when {
            currentProgress < startTime -> 0f
            currentProgress > endTime -> 1f
            endTime == startTime -> 1f
            else -> (currentProgress - startTime).toFloat() / (endTime - startTime).toFloat()
        }.coerceIn(0f, 1f)

        val oldColor = paint.color
        if (ratio <= 0f) {
            paint.color = inactiveColor
            canvas.drawText(text, start, end, x, y.toFloat(), paint)
        } else if (ratio >= 1f) {
            paint.color = activeColor
            canvas.drawText(text, start, end, x, y.toFloat(), paint)
        } else {
            val splitX = x + width * ratio

            // Draw active portion on the left
            canvas.save()
            canvas.clipRect(x, top.toFloat(), splitX, bottom.toFloat())
            paint.color = activeColor
            canvas.drawText(text, start, end, x, y.toFloat(), paint)
            canvas.restore()

            // Draw inactive portion on the right
            canvas.save()
            canvas.clipRect(splitX, top.toFloat(), x + width, bottom.toFloat())
            paint.color = inactiveColor
            canvas.drawText(text, start, end, x, y.toFloat(), paint)
            canvas.restore()
        }
        paint.color = oldColor
    }
}
