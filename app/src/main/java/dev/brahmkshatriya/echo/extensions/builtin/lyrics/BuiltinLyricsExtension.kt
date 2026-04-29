package dev.brahmkshatriya.echo.extensions.builtin.lyrics

import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImportType
import dev.brahmkshatriya.echo.common.models.Feed

class BuiltinLyricsExtension : LyricsClient {
    
    companion object {
        val metadata = Metadata(
            id = "builtin_lyrics",
            name = "Built-in Lyrics",
            description = "Provides lyrics from LRCLIB, YouTube and more",
            type = ExtensionType.LYRICS,
            importType = ImportType.BuiltIn,
            version = "1.0.0",
            author = "Echo"
        )
    }

    private val clients = listOf(
        LRCLIBLyricsClient(),
        YouTubeLyricsClient(),
        SimpMusicLyricsClient(),
        SpotifyLyricsClient()
    )

    override suspend fun searchTrackLyrics(clientId: String, track: Track): Feed<Lyrics> {
        clients.forEach {
            val feed = it.searchTrackLyrics(clientId, track)
            if (feed.tabs.isNotEmpty() || feed.getPagedData(null).pagedData.loadAll().isNotEmpty()) {
                return feed
            }
        }
        return Feed(emptyList()) { null }
    }

    override suspend fun loadLyrics(lyrics: Lyrics): Lyrics {
        return lyrics
    }
}
