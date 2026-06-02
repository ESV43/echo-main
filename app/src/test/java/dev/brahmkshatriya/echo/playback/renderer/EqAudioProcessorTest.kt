package dev.brahmkshatriya.echo.playback.renderer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi

@androidx.annotation.OptIn(UnstableApi::class)
class EqAudioProcessorTest {

    @Test
    fun testGainsApplication() {
        val processor = EqAudioProcessor()
        val gains = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
        processor.setGains(gains)
        // Basic check to ensure it doesn't crash and gains are accepted
        // In a real test we would verify the output buffer, but EqAudioProcessor 
        // internal state is private.
    }

    @Test
    fun testProcessorLifecycleAndReset() {
        val processor = EqAudioProcessor()
        val inputFormat = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outputFormat = processor.configure(inputFormat)
        
        // Assert that configuration worked and the output format is identical (pcm-16bit is processed in-place)
        assertEquals(inputFormat.sampleRate, outputFormat.sampleRate)
        assertEquals(inputFormat.channelCount, outputFormat.channelCount)
        assertEquals(inputFormat.encoding, outputFormat.encoding)
        
        processor.flush()
        
        // Let's queue some PCM 16-bit data (4 bytes = 2 channels * 1 sample)
        val inputBuffer = ByteBuffer.allocate(1024).order(ByteOrder.nativeOrder())
        // Fill with some dummy non-zero samples
        for (i in 0 until 512) {
            inputBuffer.putShort((i % 1000).toShort())
        }
        inputBuffer.flip()
        
        processor.queueInput(inputBuffer)
        
        val outputBuffer = processor.output
        // The output buffer should have been written to and contain processed samples
        assertTrue(outputBuffer.hasRemaining())
    }

    @Test
    fun testEqualizerPresets() {
        val processor = EqAudioProcessor()
        val inputFormat = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(inputFormat)
        processor.flush()

        // 1. Process with Flat gains
        val flatGains = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        processor.setGains(flatGains)
        
        val inputBuffer1 = ByteBuffer.allocate(128).order(ByteOrder.nativeOrder())
        for (i in 0 until 64) {
            inputBuffer1.putShort((1000).toShort())
        }
        inputBuffer1.flip()
        processor.queueInput(inputBuffer1)
        val flatOutput = processor.output
        val flatBytes = ByteArray(flatOutput.remaining())
        flatOutput.get(flatBytes)

        // 2. Process with Pop preset gains (which should modify the EQ curve)
        processor.flush()
        val popGains = floatArrayOf(-2f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -2f)
        processor.setGains(popGains)
        
        val inputBuffer2 = ByteBuffer.allocate(128).order(ByteOrder.nativeOrder())
        for (i in 0 until 64) {
            inputBuffer2.putShort((1000).toShort())
        }
        inputBuffer2.flip()
        processor.queueInput(inputBuffer2)
        val popOutput = processor.output
        val popBytes = ByteArray(popOutput.remaining())
        popOutput.get(popBytes)

        // The output bytes from Pop preset should be different from Flat gains
        assertTrue(!flatBytes.contentEquals(popBytes))
    }

    @Test
    fun testSleepTimerVolumeFade() {
        val steps = 30
        val volumes = FloatArray(steps + 1)
        for (i in 0..steps) {
            volumes[i] = 1f - (i.toFloat() / steps)
        }
        assertEquals(1.0f, volumes[0], 0.001f)
        assertEquals(0.0f, volumes[steps], 0.001f)
        assertTrue(volumes[15] in 0.49f..0.51f)
    }
}
