package dev.brahmkshatriya.echo.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi

class VisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private var shader: RuntimeShader? = null
    private var time = 0f
    private var amplitude = 0.5f

    private val AGSL_SHADER = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float iAmplitude;

        float4 main(float2 fragCoord) {
            float2 uv = fragCoord / iResolution.xy;
            float3 col = 0.5 + 0.5 * cos(iTime + uv.xyx + float3(0, 2, 4));
            float mask = sin(uv.x * 10.0 + iTime) * 0.1 + 0.5;
            if (uv.y > mask + iAmplitude * 0.2) {
                return float4(col * 0.2, 1.0);
            }
            return float4(col, 1.0);
        }
    """.trimIndent()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shader = RuntimeShader(AGSL_SHADER)
        }
    }

    fun updateAmplitude(amp: Float) {
        amplitude = amp
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        time += 0.05f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
            shader?.apply {
                setFloatUniform("iResolution", width.toFloat(), height.toFloat())
                setFloatUniform("iTime", time)
                setFloatUniform("iAmplitude", amplitude)
            }
            paint.shader = shader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } else {
            // Fallback for older versions
            canvas.drawColor(Color.parseColor("#121212"))
            paint.color = Color.parseColor("#2196F3")
            val h = height * 0.5f + amplitude * 100f
            canvas.drawRect(0f, height - h, width.toFloat(), height.toFloat(), paint)
        }
        invalidate()
    }
}
