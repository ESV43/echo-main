package dev.brahmkshatriya.echo.extensions.builtin.lyrics

import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImportType
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

class BuiltinLyricsExtension : LyricsClient {
    
    companion object {
        val metadata = Metadata(
            className = BuiltinLyricsExtension::class.java.name,
            path = "builtin",
            id = "builtin_lyrics",
            name = "Built-in Lyrics",
            description = "Provides lyrics from LRCLIB, YouTube and more",
            type = ExtensionType.LYRICS,
            importType = ImportType.BuiltIn,
            version = "1.0.0",
            author = "Echo"
        )
    }

    override suspend fun getSettingItems(): List<Setting> = emptyList()
    override fun setSettings(settings: Settings) {}

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
        return Feed(emptyList()) { emptyList<Lyrics>().toFeedData() }
    }

    override suspend fun loadLyrics(lyrics: Lyrics): Lyrics {
        return lyrics
    }
}
