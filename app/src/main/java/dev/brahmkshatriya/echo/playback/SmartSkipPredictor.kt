package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.content.SharedPreferences

class SmartSkipPredictor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_skip_prefs", Context.MODE_PRIVATE)

    fun recordPlay(trackId: String) {
        val playKey = "${trackId}_plays"
        val plays = prefs.getInt(playKey, 0)
        prefs.edit().putInt(playKey, plays + 1).apply()
    }

    fun recordSkip(trackId: String) {
        val skipKey = "${trackId}_skips"
        val skips = prefs.getInt(skipKey, 0)
        prefs.edit().putInt(skipKey, skips + 1).apply()
    }

    fun shouldAutoSkip(trackId: String): Boolean {
        val playKey = "${trackId}_plays"
        val skipKey = "${trackId}_skips"
        val plays = prefs.getInt(playKey, 0)
        val skips = prefs.getInt(skipKey, 0)
        val total = plays + skips
        if (total < 5) return false
        val skipRate = skips.toFloat() / total
        return skipRate >= 0.80f
    }
}
