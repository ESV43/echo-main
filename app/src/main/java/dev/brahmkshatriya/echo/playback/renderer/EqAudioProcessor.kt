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
    @Volatile
    private var filters: Array<Array<BiquadFilter>>? = null

    var pcmCallback: ((ShortArray) -> Unit)? = null
    private var sampleCount = 0
    private val sampleInterval = 44100 * 2 // Sample every 2 seconds
    private val pcmBuffer = ShortArray(16000)

    @Volatile
    var autoGainEnabled: Boolean = false

    val beatDetector = BeatDetector()

    private var currentGain = 1.0f
    private var targetGain = 1.0f

    private var beatSampleCount = 0
    private var beatEnergySum = 0.0f

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
        beatDetector.reset()
        beatSampleCount = 0
        beatEnergySum = 0.0f
        currentGain = 1.0f
        targetGain = 1.0f
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

        if (autoGainEnabled) {
            val rms = calculateRms(inputBuffer)
            targetGain = if (rms > 0.005f) {
                (0.22f / rms).coerceIn(0.1f, 3.0f)
            } else {
                1.0f
            }
        } else {
            targetGain = 1.0f
        }

        while (inputBuffer.hasRemaining()) {
            for (c in 0 until channelCount) {
                if (!inputBuffer.hasRemaining()) break
                val rawSample = inputBuffer.short
                var sample = rawSample.toDouble()
                
                if (c == 0) {
                    if (sampleCount < pcmBuffer.size) {
                        pcmBuffer[sampleCount] = rawSample
                    }

                    // Feed beat detector
                    val normSample = rawSample.toFloat() / 32768f
                    beatEnergySum += normSample * normSample
                    beatSampleCount++
                    if (beatSampleCount >= 1024) {
                        val blockEnergy = beatEnergySum / 1024f
                        val sampleRate = inputAudioFormat.sampleRate
                        if (sampleRate > 0) {
                            beatDetector.processBlock(blockEnergy, sampleRate)
                        }
                        beatSampleCount = 0
                        beatEnergySum = 0.0f
                    }
                }

                // Apply active gain factor before filtering (or after, linear DSP)
                sample *= currentGain

                for (b in bands.indices) {
                    sample = filters[c][b].process(sample)
                }
                
                val outSample = sample.coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
                buffer.putShort(outSample)
            }

            // Smoothly adapt gain
            currentGain += (targetGain - currentGain) * 0.0001f

            sampleCount++
            if (sampleCount >= sampleInterval) {
                pcmCallback?.invoke(pcmBuffer.copyOf())
                sampleCount = 0
            }
        }
        buffer.flip()
    }

    private fun calculateRms(buffer: ByteBuffer): Float {
        val dup = buffer.duplicate()
        dup.order(buffer.order())
        var sumSq = 0.0
        var count = 0
        while (dup.hasRemaining()) {
            if (dup.remaining() < 2) break
            val sample = dup.short.toFloat() / 32768f
            sumSq += (sample * sample).toDouble()
            count++
        }
        if (count == 0) return 0f
        return kotlin.math.sqrt(sumSq / count).toFloat()
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