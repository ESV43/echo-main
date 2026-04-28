package dev.brahmkshatriya.echo.extensions.builtin.dabyeet

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
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
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toServerMedia
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingTextInput
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.BuildConfig
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network.ApiService
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network.HifiApiService
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network.YoutubeMusicApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.Locale.getDefault
import java.util.concurrent.TimeUnit

@Suppress("unused")
class DabYeetExtension : ExtensionClient, SearchFeedClient, TrackClient, AlbumClient, ArtistClient,
    ShareClient, LoginClient.CustomInput, LibraryFeedClient, HomeFeedClient {

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

    private var hifiApiBaseUrl: String = DEFAULT_HIFI_API_BASE_URL
    private var dabApiBaseUrl: String = DEFAULT_DAB_API_BASE_URL

    private val hifiApi by lazy { HifiApiService(client, json) }
    private val youtubeMusicApi by lazy { YoutubeMusicApiService(client, json) }
    private val dabApi by lazy { ApiService(client, json) }


    // ===== Settings ===== //

    override suspend fun onExtensionSelected() {}

    override suspend fun onInitialize() {}

    override suspend fun getSettingItems(): List<Setting> = listOf(
        SettingTextInput(
            title = "HiFi API base URL",
            key = HIFI_API_BASE_URL,
            summary = "hifi-api URL used for hi-res audio. Defaults to $DEFAULT_HIFI_API_BASE_URL"
        ),
        SettingTextInput(
            title = "DAB API base URL",
            key = DAB_API_BASE_URL,
            summary = "DAB API URL used as fallback for lossless audio. Defaults to $DEFAULT_DAB_API_BASE_URL"
        )
    )

    override fun setSettings(settings: Settings) {
        hifiApiBaseUrl = settings.getString(HIFI_API_BASE_URL)
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_HIFI_API_BASE_URL

        dabApiBaseUrl = settings.getString(DAB_API_BASE_URL)
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_DAB_API_BASE_URL
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
        val hifiResult = runCatching {
            hifiApi.getStream(hifiApiBaseUrl, streamable, isDownload)
        }
        
        if (hifiResult.isSuccess) return hifiResult.getOrThrow()
        
        hifiResult.exceptionOrNull()?.printStackTrace()
        
        // DAB API Fallback
        return runCatching {
            val searchQuery = streamable.extras["searchQuery"]
                ?: throw IllegalStateException("Missing search query for fallback")
            
            val searchResponse = dabApi.search(
                baseUrl = dabApiBaseUrl,
                query = searchQuery,
                type = "track"
            )
            
            val trackId = searchResponse.tracks?.firstOrNull()?.id
                ?: throw IllegalStateException("No DAB fallback match found")
                
            val stream = dabApi.getStream(dabApiBaseUrl, trackId)
            stream.url.toServerMedia()
        }.getOrElse { e ->
            e.printStackTrace()
            throw Exception("Failed to load media from Hifi and DAB fallback: ${e.message}", e)
        }
    }

    override suspend fun getLoginForm(): List<LoginClient.Form> = listOf(
        LoginClient.Form(
            key = "login",
            title = "Login",
            type = LoginClient.InputField.Type.Email,
            inputFields = listOf(
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Email,
                    key = "email",
                    label = "Email",
                    isRequired = true
                ),
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Password,
                    key = "password",
                    label = "Password",
                    isRequired = true
                )
            )
        )
    )

    override suspend fun onLogin(
        key: String,
        data: Map<String, String?>
    ): List<User> {
        when (key) {
            "register" -> {
                val response = dabApi.register(
                    baseUrl = dabApiBaseUrl,
                    username = data["username"]!!,
                    email = data["email"]!!,
                    password = data["password"]!!,
                    inviteCode = data["inviteCode"]
                )
                val session = getSession(response)
                    ?: throw Exception("Failed to extract session from response")
                return listOf(
                    User(
                        id = data["email"]!!,
                        name = data["username"]!!,
                        extras = mapOf(
                            "session" to session,
                            "email" to data["email"]!!,
                            "password" to data["password"]!!
                        )
                    )
                )
            }
            "login" -> {
                val response = dabApi.login(
                    baseUrl = dabApiBaseUrl,
                    username = data["email"]!!,
                    password = data["password"]!!
                )
                val session = getSession(response)
                    ?: throw Exception("Failed to extract session from response")
                return listOf(
                    User(
                        id = data["email"]!!,
                        name = data["email"]!!,
                        extras = mapOf(
                            "session" to session,
                            "email" to data["email"]!!,
                            "password" to data["password"]!!
                        )
                    )
                )
            }
            else -> {
                throw IllegalArgumentException("Invalid login form key: $key")
            }
        }
    }

    private fun getSession(response: Response): String? {
        val cookies = Cookie.parseAll(
            response.request.url,
            response.headers
        )
        val session = cookies.find { it.name == "session" }?.value

        return if (session != null) {
            "session=$session"
        } else {
            null
        }
    }

    private var _session: String? = null
    override fun setLoginUser(user: User?) {
        _session = user?.extras?.get("session")
    }

    override suspend fun getCurrentUser(): User? = null

    // ======= LibraryFeedClient ===== //

    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        val session = _session ?: throw ClientException.LoginRequired()

        val favorites = dabApi.getFavourites(dabApiBaseUrl, session)
        val likedTracks = favorites.track.map { it.toTrack(json) }

        val favShelf = Shelf.Lists.Items(
            id = "fav",
            title = "Favourites",
            list = likedTracks,
            type = Shelf.Lists.Type.Linear,
        )

        val playlists = dabApi.getPlaylists(dabApiBaseUrl, session).libraries.map { it.toPlaylist() }

        val playlistShelf = Shelf.Lists.Items(
            id = "playlist",
            title = "Playlists",
            list = playlists,
            type = Shelf.Lists.Type.Linear,
        )

        return listOf(favShelf, playlistShelf).toFeed()
    }

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

        private const val DAB_API_BASE_URL = "dab_api_base_url"
        private const val DEFAULT_DAB_API_BASE_URL = "https://dabmusic.xyz/api"

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
