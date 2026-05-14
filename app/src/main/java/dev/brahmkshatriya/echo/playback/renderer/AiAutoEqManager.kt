package dev.brahmkshatriya.echo.playback.renderer

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AiAutoEqManager(private val context: Context, private val eqProcessor: EqAudioProcessor) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var interpreter: Interpreter? = null
    private val genres = listOf("Pop", "Rock", "Jazz", "Hip-Hop", "Classical")
    
    // EQ Presets: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
    private val presets = mapOf(
        "Pop" to floatArrayOf(0f, 1f, 2f, -1f, -2f, -1f, 0f, 1f, 2f, 2f),
        "Rock" to floatArrayOf(4f, 3f, 2f, 0f, -1f, 0f, 1f, 2f, 3f, 4f),
        "Jazz" to floatArrayOf(2f, 1f, 0f, 1f, 2f, 1f, 0f, 1f, 2f, 3f),
        "Hip-Hop" to floatArrayOf(5f, 4f, 2f, 0f, -1f, -1f, 0f, 1f, 2f, 3f),
        "Classical" to floatArrayOf(3f, 2f, 1f, 0.5f, 0f, 0.5f, 1f, 2f, 2f, 3f)
    )

    init {
        runCatching {
            val model = context.assets.open("genre_model.tflite").readBytes()
            val buffer = ByteBuffer.allocateDirect(model.size).apply {
                order(ByteOrder.nativeOrder())
                put(model)
            }
            interpreter = Interpreter(buffer)
        }.onFailure {
            android.util.Log.e("AiAutoEqManager", "Failed to load genre_model.tflite", it)
        }
    }

    fun classifyAndApplyEq(pcmData: ShortArray) {
        val interpreter = interpreter ?: return
        scope.launch {
            // Prepare input (example: 16000 samples)
            val input = ByteBuffer.allocateDirect(pcmData.size * 2).apply {
                order(ByteOrder.nativeOrder())
                pcmData.forEach { putShort(it) }
            }
            
            val output = Array(1) { FloatArray(genres.size) }
            interpreter.run(input, output)
            
            val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: return@launch
            val genre = genres[maxIndex]
            
            presets[genre]?.let { eqProcessor.setGains(it) }
        }
    }
}
