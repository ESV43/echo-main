package dev.brahmkshatriya.echo.extensions.builtin.lyrics

import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class LRCLIBLyricsClient : BaseLyricsClient() {
    
    @Serializable
    data class LRCLIBResponse(
        val id: Int,
        val trackName: String? = null,
        val artistName: String? = null,
        val albumName: String? = null,
        val duration: Double? = null,
        val instrumental: Boolean? = null,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null
    )

    override suspend fun searchTrackLyrics(clientId: String, track: Track): dev.brahmkshatriya.echo.common.models.Feed<Lyrics> {
        val url = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
            .addQueryParameter("artist_name", track.artists.firstOrNull()?.name ?: "")
            .addQueryParameter("track_name", track.title)
            .addQueryParameter("duration", (track.duration / 1000).toString())
            .build()

        val request = Request.Builder().url(url).build()
        val response = CallWait(request)
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return super.searchTrackLyrics(clientId, track)
            val lrcResponse = json.decodeFromString<LRCLIBResponse>(body)
            val lyrics = Lyrics(
                id = lrcResponse.id.toString(),
                title = lrcResponse.trackName ?: track.title,
                subtitle = lrcResponse.artistName,
                lyrics = parseLrc(lrcResponse.syncedLyrics ?: lrcResponse.plainLyrics ?: "")
            )
            return listOf(lyrics).toFeed()
        }
        return super.searchTrackLyrics(clientId, track)
    }

    override suspend fun loadLyrics(lyrics: Lyrics): Lyrics {
        return lyrics
    }

    private fun parseLrc(lrc: String): Lyrics.Lyric {
        if (!lrc.contains("[")) return Lyrics.Simple(lrc)
        
        val lines = lrc.lines()
        val items = mutableListOf<Lyrics.Item>()
        val timeRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]")
        
        lines.forEach { line ->
            val match = timeRegex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val ms = match.groupValues[3].toLong().let { 
                    if (match.groupValues[3].length == 2) it * 10 else it 
                }
                val startTime = min * 60000 + sec * 1000 + ms
                val text = line.substring(match.range.last + 1).trim()
                items.add(Lyrics.Item(text, startTime, 0))
            }
        }
        
        for (i in 0 until items.size - 1) {
            items[i] = items[i].copy(endTime = items[i+1].startTime)
        }
        if (items.isNotEmpty()) {
            items[items.size - 1] = items[items.size - 1].copy(endTime = items[items.size - 1].startTime + 5000)
        }
        
        return Lyrics.Timed(items)
    }
}
