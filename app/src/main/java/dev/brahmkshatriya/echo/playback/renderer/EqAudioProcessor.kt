package dev.brahmkshatriya.echo.playback.renderer

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@OptIn(UnstableApi::class)
class EqAudioProcessor : BaseAudioProcessor() {

    private val bands = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    private val gains = FloatArray(bands.size)
    private var filters: Array<Array<BiquadFilter>>? = null

    fun setGains(newGains: FloatArray) {
        if (newGains.size != gains.size) return
        newGains.copyInto(gains)
        updateFilters()
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        updateFilters()
    }

    private fun updateFilters() {
        val format = inputAudioFormat
        if (format == AudioFormat.NOT_SET) return

        val sampleRate = format.sampleRate.toDouble()
        val channelCount = format.channelCount

        val newFilters = Array(channelCount) {
            Array(bands.size) { i ->
                BiquadFilter().apply {
                    setPeakingEq(sampleRate, bands[i].toDouble(), 1.41, gains[i].toDouble())
                }
            }
        }
        filters = newFilters
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val filters = filters ?: return
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val buffer = replaceOutputBuffer(remaining)
        val channelCount = inputAudioFormat.channelCount

        while (inputBuffer.hasRemaining()) {
            for (c in 0 until channelCount) {
                if (!inputBuffer.hasRemaining()) break
                var sample = inputBuffer.short.toDouble()
                
                for (b in bands.indices) {
                    sample = filters[c][b].process(sample)
                }
                
                val outSample = sample.coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
                buffer.putShort(outSample)
            }
        }
        buffer.flip()
    }

    private class BiquadFilter {
        private var b0 = 0.0
        private var b1 = 0.0
        private var b2 = 0.0
        private var a1 = 0.0
        private var a2 = 0.0

        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        fun setPeakingEq(sampleRate: Double, frequency: Double, q: Double, dbGain: Double) {
            val a = 10.0.pow(dbGain / 40.0)
            val omega = 2.0 * PI * frequency / sampleRate
            val sn = sin(omega)
            val cs = cos(omega)
            val alpha = sn / (2.0 * q)

            val b0_ = 1.0 + alpha * a
            val b1_ = -2.0 * cs
            val b2_ = 1.0 - alpha * a
            val a0_ = 1.0 + alpha / a
            val a1_ = -2.0 * cs
            val a2_ = 1.0 - alpha / a

            b0 = b0_ / a0_
            b1 = b1_ / a0_
            b2 = b2_ / a0_
            a1 = a1_ / a0_
            a2 = a2_ / a0_
        }

        fun process(x: Double): Double {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }
    }
}