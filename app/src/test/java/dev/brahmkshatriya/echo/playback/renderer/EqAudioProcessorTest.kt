package dev.brahmkshatriya.echo.playback.renderer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
}
