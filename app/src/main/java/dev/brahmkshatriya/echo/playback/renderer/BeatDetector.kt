package dev.brahmkshatriya.echo.playback.renderer

import java.util.LinkedList
import java.util.Queue

class BeatDetector {
    private val energyHistory: Queue<Float> = LinkedList()
    private val maxHistorySize = 43

    private val beatIntervals: Queue<Long> = LinkedList()
    private val maxIntervalsSize = 20
    private var lastBeatTimestamp: Long = 0L

    var onBpmEstimated: ((Float) -> Unit)? = null
    private var elapsedMs: Long = 0L

    fun processBlock(blockEnergy: Float, sampleRate: Int) {
        val blockSize = 1024
        val blockDurationMs = (blockSize.toFloat() / sampleRate * 1000f).toLong()
        elapsedMs += blockDurationMs

        if (energyHistory.size >= 10) { // Require a small history before starting detection
            val sum = energyHistory.sum()
            val avg = sum / energyHistory.size
            
            if (blockEnergy > 1.3f * avg && (elapsedMs - lastBeatTimestamp) >= 250L) {
                if (lastBeatTimestamp > 0L) {
                    val interval = elapsedMs - lastBeatTimestamp
                    if (interval in 250..2000) {
                        beatIntervals.add(interval)
                        if (beatIntervals.size > maxIntervalsSize) {
                            beatIntervals.poll()
                        }
                        
                        val medianInterval = calculateMedian(beatIntervals.toList())
                        if (medianInterval > 0f) {
                            val bpm = 60000f / medianInterval
                            onBpmEstimated?.invoke(bpm)
                        }
                    }
                }
                lastBeatTimestamp = elapsedMs
            }
        }

        energyHistory.add(blockEnergy)
        if (energyHistory.size > maxHistorySize) {
            energyHistory.poll()
        }
    }

    private fun calculateMedian(list: List<Long>): Float {
        if (list.isEmpty()) return 0f
        val sorted = list.sorted()
        val size = sorted.size
        return if (size % 2 == 1) {
            sorted[size / 2].toFloat()
        } else {
            (sorted[size / 2 - 1] + sorted[size / 2]).toFloat() / 2f
        }
    }

    fun reset() {
        energyHistory.clear()
        beatIntervals.clear()
        lastBeatTimestamp = 0L
        elapsedMs = 0L
    }
}
