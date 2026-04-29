package dev.brahmkshatriya.echo.extensions.builtin.lyrics

import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import kotlinx.serialization.Serializable
import okhttp3.Request

class SpotifyLyricsClient : BaseLyricsClient() {
    
    @Serializable
    data class SpotifyLyricsResponse(
        val lyrics: LyricsData? = null
    )

    @Serializable
    data class LyricsData(
        val lines: List<Line>? = null
    )

    @Serializable
    data class Line(
        val startTimeMs: String,
        val words: String,
        val syllables: List<String>? = null
    )

    override suspend fun searchTrackLyrics(clientId: String, track: Track): dev.brahmkshatriya.echo.common.models.Feed<Lyrics> {
        // This is a common public proxy for Spotify lyrics. 
        // It requires a track ID, but sometimes works with a search.
        // For this implementation, we'll assume we can't easily get the Spotify ID without a search.
        // So we might skip it or use a search if the API supports it.
        // Let's just provide it as a placeholder that could be expanded.
        return super.searchTrackLyrics(clientId, track)
    }

    override suspend fun loadLyrics(lyrics: Lyrics): Lyrics {
        return lyrics
    }
}
