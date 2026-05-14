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
        // Try to get Spotify ID from extras if it exists
        val spotifyId = track.extras["spotify_id"] ?: return super.searchTrackLyrics(clientId, track)

        val url = "https://spotify-lyric-api.herokuapp.com/?trackid=$spotifyId"
        val request = Request.Builder().url(url).build()
        val response = CallWait(request)
        
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return super.searchTrackLyrics(clientId, track)
            val data = try {
                json.decodeFromString<SpotifyLyricsResponse>(body)
            } catch (e: Exception) {
                null
            } ?: return super.searchTrackLyrics(clientId, track)

            val lines = data.lyrics?.lines ?: return super.searchTrackLyrics(clientId, track)
            val items = lines.mapIndexed { index, line ->
                val start = line.startTimeMs.toLong()
                val nextStart = lines.getOrNull(index + 1)?.startTimeMs?.toLong() ?: (start + 5000)
                Lyrics.Item(line.words, start, nextStart)
            }

            val lyrics = Lyrics(
                id = "spotify_$spotifyId",
                title = track.title,
                subtitle = "Spotify Lyrics",
                lyrics = Lyrics.Timed(items)
            )
            return listOf(lyrics).toFeed()
        }

        return super.searchTrackLyrics(clientId, track)
    }
}
