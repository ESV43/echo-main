package dev.brahmkshatriya.echo.utils.ui

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

class ShimmerDrawable(
    private val widthFraction: Float = 0.4f,
    shimmerAlpha: Int = 40,
    baseAlpha: Int = 10,
) : Drawable() {

    private val shimmerColor = Color.argb(shimmerAlpha, 255, 255, 255)
    private val baseColor = Color.argb(baseAlpha, 255, 255, 255)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()

    private val animator = ValueAnimator.ofFloat(-widthFraction, 1f + widthFraction).apply {
        duration = 1200
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { invalidateSelf() }
    }

    private var shaderWidth = 0f

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        val w = bounds.width().toFloat()
        if (w <= 0) return
        shaderWidth = w * (1f + widthFraction * 2f)
        paint.shader = LinearGradient(
            0f, 0f, shaderWidth, 0f,
            intArrayOf(baseColor, shimmerColor, baseColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        if (w <= 0 || paint.shader == null) return
        matrix.reset()
        val fraction = animator.animatedFraction
        val offset = -widthFraction * w + (w + widthFraction * w * 2f) * fraction
        matrix.postTranslate(offset, 0f)
        paint.shader?.setLocalMatrix(matrix)
        canvas.drawRect(bounds, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
    }
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT

    fun start() { animator.start() }
    fun stop() { animator.cancel() }
    fun isRunning() = animator.isRunning
}
