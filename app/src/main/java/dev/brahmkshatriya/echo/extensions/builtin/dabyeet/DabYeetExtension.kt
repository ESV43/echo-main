package dev.brahmkshatriya.echo.extensions.builtin.dabyeet

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingTextInput
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.BuildConfig
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network.HifiApiService
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network.YoutubeMusicApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.Locale.getDefault
import java.util.concurrent.TimeUnit

@Suppress("unused")
class DabYeetExtension : ExtensionClient, SearchFeedClient, TrackClient, AlbumClient, ArtistClient,
    ShareClient, HomeFeedClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private val hifiApi by lazy { HifiApiService(client, json) }
    private val youtubeMusicApi by lazy { YoutubeMusicApiService(client, json) }

    private var hifiApiBaseUrl: String = DEFAULT_HIFI_API_BASE_URL


    // ===== Settings ===== //

    override suspend fun onExtensionSelected() {}

    override suspend fun onInitialize() {}

    override suspend fun getSettingItems(): List<Setting> = listOf(
        SettingTextInput(
            title = "HiFi API base URL",
            key = HIFI_API_BASE_URL,
            summary = "hifi-api URL used for hi-res audio. Defaults to $DEFAULT_HIFI_API_BASE_URL"
        )
    )

    override fun setSettings(settings: Settings) {
        hifiApiBaseUrl = settings.getString(HIFI_API_BASE_URL)
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_HIFI_API_BASE_URL
    }


    //===== HomeFeedClient =====//

    private val homeQueries = listOf(
        "new music",
        "trending songs",
        "top songs"
    )

    override suspend fun loadHomeFeed(): Feed<Shelf> = coroutineScope {
        homeQueries.mapIndexed { index, query ->
            async {
                runCatching {
                    Shelf.Lists.Items(
                        id = index.toString(),
                        title = query.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() },
                        list = youtubeMusicApi.searchTracks(query).map { it.toTrack() },
                        type = Shelf.Lists.Type.Grid
                    )
                }.getOrNull()
            }
        }.awaitAll().filterNotNull().toFeed()
    }

    //==== SearchFeedClient ====//

    override suspend fun loadSearchFeed(query: String): Feed<Shelf> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList<Shelf>().toFeed()

        listOf(
            Shelf.Lists.Items(
                id = "tracks",
                title = "Tracks",
                type = Shelf.Lists.Type.Grid,
                list = youtubeMusicApi.searchTracks(query).map { it.toTrack() }
            )
        ).toFeed()
    }

    private fun YoutubeMusicApiService.Track.toTrack(): Track {
        val primaryArtist = artists.firstOrNull().orEmpty()
        val artistItems = artists.ifEmpty { listOf("YouTube Music") }.map { name ->
            Artist(id = name, name = name, isShareable = false)
        }

        return Track(
            id = videoId,
            title = title,
            artists = artistItems,
            album = album?.let { Album(id = it, title = it, artists = artistItems, isShareable = false) },
            cover = thumbnail?.toImageHolder(),
            duration = durationSeconds?.times(1000L),
            subtitle = artists.joinToString(", ").ifBlank { null },
            extras = mapOf(
                "searchQuery" to listOf(title, primaryArtist).filter { it.isNotBlank() }.joinToString(" "),
                "youtubeVideoId" to videoId,
            ),
            streamables = listOf(
                Streamable.server(
                    id = videoId,
                    quality = 1,
                    title = "Hi-Res",
                    extras = mapOf(
                        "searchQuery" to listOf(title, primaryArtist).filter { it.isNotBlank() }.joinToString(" "),
                        "title" to title,
                        "artist" to primaryArtist,
                        "youtubeVideoId" to videoId,
                    )
                )
            )
        )
    }

    // ====== TrackClient ======= //

    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track = track

    override suspend fun loadStreamableMedia(
        streamable: Streamable,
        isDownload: Boolean
    ): Streamable.Media {
        return hifiApi.getStream(hifiApiBaseUrl, streamable, isDownload)
    }

    override suspend fun loadFeed(track: Track): Feed<Shelf>? = null

    // ====== AlbumClient ====== //

    override suspend fun loadAlbum(album: Album): Album {
        return album
    }

    override suspend fun loadTracks(album: Album): Feed<Track>? {
        return null
    }

    override suspend fun loadFeed(album: Album): Feed<Shelf>? = null

    // ====== ArtistClient ===== //

    override suspend fun loadArtist(artist: Artist): Artist {
        return artist
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return emptyList<Shelf>().toFeed()
    }

    // ====== ShareClient ===== //

    override suspend fun onShare(item: EchoMediaItem): String {
        return when (item) {
            is Track -> "https://music.youtube.com/watch?v=${item.extras["youtubeVideoId"] ?: item.id}"
            is Album -> "https://music.youtube.com/search?q=${item.title}"
            is Artist -> "https://music.youtube.com/search?q=${item.name}"

            is Radio -> throw ClientException.NotSupported("Will not be implemented")
            else -> throw ClientException.NotSupported("Sharing is not supported for this item")
        }
    }

    companion object {
        private const val HIFI_API_BASE_URL = "hifi_api_base_url"
        private const val DEFAULT_HIFI_API_BASE_URL = "https://tidal.squid.wtf"

        val metadata = Metadata(
            className = "dev.brahmkshatriya.echo.extensions.builtin.dabyeet.DabYeetExtension",
            path = "",
            importType = ImportType.BuiltIn,
            type = ExtensionType.MUSIC,
            id = "dab_yeet",
            name = "Hi-Res Music",
            description = "YouTube Music metadata with hifi-api hi-res audio",
            version = "v${BuildConfig.VERSION_CODE}",
            author = "Sad",
            icon = "https://raw.githubusercontent.com/BitFable/echo-dab-yeet-extension/refs/heads/image-branch/music.png".toImageHolder()
        )
    }
}
