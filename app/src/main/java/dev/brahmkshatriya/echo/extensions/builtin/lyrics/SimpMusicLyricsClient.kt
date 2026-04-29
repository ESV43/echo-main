package dev.brahmkshatriya.echo.extensions.builtin.lyrics

import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import kotlinx.serialization.Serializable
import okhttp3.Request

class SimpMusicLyricsClient : BaseLyricsClient() {
    
    @Serializable
    data class PipedLyrics(
        val lyrics: String? = null
    )

    override suspend fun searchTrackLyrics(clientId: String, track: Track): dev.brahmkshatriya.echo.common.models.Feed<Lyrics> {
        val videoId = track.id
        if (videoId.length != 11) return super.searchTrackLyrics(clientId, track)

        val url = "https://pipedapi.kavin.rocks/lyrics?v=$videoId"
        val request = Request.Builder().url(url).build()
        val response = CallWait(request)
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return super.searchTrackLyrics(clientId, track)
            val piped = json.decodeFromString<PipedLyrics>(body)
            if (piped.lyrics != null) {
                val lyrics = Lyrics(
                    id = "simp_$videoId",
                    title = track.title,
                    subtitle = "SimpMusic (Piped)",
                    lyrics = Lyrics.Simple(piped.lyrics)
                )
                return listOf(lyrics).toFeed()
            }
        }
        return super.searchTrackLyrics(clientId, track)
    }
}
