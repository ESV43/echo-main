package dev.brahmkshatriya.echo.utils.ui

import android.graphics.*
import android.graphics.drawable.Drawable
import kotlin.math.abs

class WaveformDrawable(
    private val accentColor: Int = Color.WHITE,
    private val backgroundColor: Int = Color.argb(80, 255, 255, 255)
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var samples: FloatArray = FloatArray(64) { 0.1f + abs(kotlin.math.sin(it * 0.5)).toFloat() * 0.5f }
    private var progress: Float = 0f
    private var amplitude: Float = 0.5f

    fun setSamples(newSamples: FloatArray) {
        samples = newSamples
        invalidateSelf()
    }

    fun setProgress(p: Float) {
        progress = p.coerceIn(0f, 1f)
        invalidateSelf()
    }

    fun setAmplitude(amp: Float) {
        amplitude = amp.coerceIn(0f, 1f)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0 || h <= 0) return

        val barCount = samples.size
        val barWidth = w / barCount
        val gap = barWidth * 0.2f
        val barActualWidth = barWidth - gap

        val midY = h / 2f

        for (i in 0 until barCount) {
            val normalizedHeight = (samples[i % samples.size] * amplitude * h * 0.8f).coerceIn(2f, h * 0.8f)
            val x = i * barWidth + gap / 2f
            val fraction = i.toFloat() / barCount

            paint.color = if (fraction <= progress) {
                ColorUtils.blendColors(backgroundColor, accentColor, (progress - fraction) / progress.coerceAtLeast(0.01f))
            } else {
                backgroundColor
            }

            paint.alpha = if (fraction <= progress) 255 else 120
            canvas.drawRoundRect(x, midY - normalizedHeight / 2f, x + barActualWidth, midY + normalizedHeight / 2f, 2f, 2f, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

private object ColorUtils {
    fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val r = (Color.red(c1) * (1 - ratio) + Color.red(c2) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * (1 - ratio) + Color.green(c2) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * (1 - ratio) + Color.blue(c2) * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
