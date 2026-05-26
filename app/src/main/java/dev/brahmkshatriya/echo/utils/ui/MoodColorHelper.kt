package dev.brahmkshatriya.echo.utils.ui

import android.graphics.Color
import dev.brahmkshatriya.echo.ui.player.PlayerColors
import kotlin.math.abs
import kotlin.math.sin

object MoodColorHelper {

    data class Mood(val label: String, val accent: Int, val background: Int, val onBackground: Int) {
        fun toPlayerColors() = PlayerColors(accent, background, onBackground)
    }

    private val moods = listOf(
        Mood("Energetic", 0xFFFF6B35.toInt(), 0xFF1A0A00.toInt(), 0xFFFFFFFF.toInt()),
        Mood("Happy", 0xFFFFD93D.toInt(), 0xFF1A1A00.toInt(), 0xFFFFFFFF.toInt()),
        Mood("Calm", 0xFF4ECDC4.toInt(), 0xFF001A1A.toInt(), 0xFFFFFFFF.toInt()),
        Mood("Sad", 0xFF6C63FF.toInt(), 0xFF0A001A.toInt(), 0xFFFFFFFF.toInt()),
        Mood("Dark", 0xFFFF4081.toInt(), 0xFF1A0010.toInt(), 0xFFFFFFFF.toInt()),
        Mood("Focused", 0xFF00E676.toInt(), 0xFF001A0A.toInt(), 0xFFFFFFFF.toInt()),
        Mood("Neutral", 0xFFB388FF.toInt(), 0xFF100A1A.toInt(), 0xFFFFFFFF.toInt())
    )

    fun getMoodForBpm(bpm: Float): Mood {
        return when {
            bpm >= 140 -> moods[0]
            bpm >= 110 -> moods[1]
            bpm >= 80 -> moods[2]
            bpm >= 60 -> moods[3]
            bpm > 0 -> moods[5]
            else -> moods[6]
        }
    }

    fun getPlayerColorsForBpm(bpm: Float, baseColors: PlayerColors): PlayerColors {
        if (bpm <= 0) return baseColors
        val mood = getMoodForBpm(bpm)
        val blend = 0.6f
        return PlayerColors(
            accent = blendColors(mood.accent, baseColors.accent, blend),
            background = blendColors(mood.background, baseColors.background, blend),
            onBackground = baseColors.onBackground
        )
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val r = (Color.red(c1) * (1 - ratio) + Color.red(c2) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * (1 - ratio) + Color.green(c2) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * (1 - ratio) + Color.blue(c2) * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
