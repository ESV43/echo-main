package dev.brahmkshatriya.echo.extensions.builtin.dabyeet

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toServerMedia
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.SettingList
import dev.brahmkshatriya.echo.common.settings.SettingTextInput
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.BuildConfig
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network.ApiService
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network.HifiApiService
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network.YoutubeMusicApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cookie
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.Locale.getDefault
import java.util.concurrent.TimeUnit

@Suppress("unused")
class DabYeetExtension : ExtensionClient, SearchFeedClient, TrackClient, AlbumClient, ArtistClient,
    ShareClient, LoginClient.CustomInput, LibraryFeedClient, HomeFeedClient, LyricsClient, PlaylistClient, RadioClient {

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

    private val hifiMirrors = listOf(
        "https://hifi-api.kennyy.com.br",
        "https://qobuz.kennyy.com.br",
        "https://triton.squid.wtf/api",
        "https://aether.squid.wtf/api",
        "https://zeus.squid.wtf/api",
        "https://virginia.monochrome.tf/api",
        "https://oregon.monochrome.tf/api",
        "https://wolf.qqdl.site/api",
        "https://maus.qqdl.site/api",
        "https://tidal.401658.xyz/api"
    )

    // ===== Settings ===== //

    override suspend fun onExtensionSelected() {}

    override suspend fun onInitialize() {}

    override suspend fun getSettingItems(): List<Setting> = listOf(
        SettingList(
            title = "Hi-Res Source",
            key = HIFI_SOURCE,
            summary = "Select the source for Hi-Res audio",
            entryTitles = listOf("Auto") + hifiMirrors.map { it.substringAfter("//").substringBefore(".") } + listOf("Custom"),
            entryValues = listOf("auto") + hifiMirrors + listOf("custom"),
            defaultEntryIndex = 0
        ),

        SettingTextInput(
            title = "HiFi API base URL",
            key = HIFI_API_BASE_URL,
            summary = "Custom hifi-api URL used for hi-res audio."
        ),

        SettingTextInput(
            title = "DAB API base URL",
            key = DAB_API_BASE_URL,
            summary = "DAB API URL used as fallback for lossless audio. Defaults to $DEFAULT_DAB_API_BASE_URL"
        )
    )

    private var hifiSource: String = "auto"
    override fun setSettings(settings: Settings) {
        hifiSource = settings.getString(HIFI_SOURCE) ?: "auto"
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
            isRadioSupported = true,
            extras = mapOf(
                "searchQuery" to listOf(title, primaryArtist).filter { it.isNotBlank() }.joinToString(" "),
                "youtubeVideoId" to videoId,
                "title" to title,
                "artist" to primaryArtist
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
        val urlsToTry = when (hifiSource) {
            "auto" -> (listOf(hifiApiBaseUrl) + hifiMirrors).distinct()
            "custom" -> listOf(hifiApiBaseUrl)
            else -> (listOf(hifiSource) + hifiMirrors + hifiApiBaseUrl).distinct()
        }
        
        for (baseUrl in urlsToTry) {
            val result = runCatching {
                hifiApi.getStream(baseUrl, streamable, isDownload)
            }
            if (result.isSuccess) return result.getOrThrow()
            result.exceptionOrNull()?.printStackTrace()
        }
        
        // DAB API Fallback
        val dabUrls = listOf(dabApiBaseUrl, "https://dab.yeet.su/api").distinct()
        var lastError: Exception? = null
        
        for (baseUrl in dabUrls) {
            val result = runCatching {
                val searchQuery = streamable.extras["searchQuery"]
                    ?: throw IllegalStateException("Missing search query for fallback")
                val title = streamable.extras["title"].orEmpty()
                val artist = streamable.extras["artist"].orEmpty()
                
                val searchResponse = dabApi.search(
                    baseUrl = baseUrl,
                    query = searchQuery,
                    type = "track"
                )
                
                val tracks = searchResponse.tracks ?: emptyList()
                val matchedTrack = tracks.firstOrNull { 
                    it.title.contains(title, ignoreCase = true) && 
                    (it.artist.contains(artist, ignoreCase = true) || artist.contains(it.artist, ignoreCase = true))
                } ?: tracks.firstOrNull() ?: throw IllegalStateException("No DAB fallback match found")
                    
                val stream = dabApi.getStream(baseUrl, matchedTrack.id)
                stream.url.toServerMedia()
            }
            if (result.isSuccess) return result.getOrThrow()
            lastError = result.exceptionOrNull() as? Exception
        }
        
        throw Exception("Failed to load media from all Hi-Res mirrors and DAB fallbacks", lastError)
    }

    override suspend fun loadFeed(track: Track): Feed<Shelf>? = null

    // ====== LyricsClient ====== //

    override suspend fun searchTrackLyrics(clientId: String, track: Track): Feed<Lyrics> {
        val urlsToTry = when (hifiSource) {
            "auto" -> (listOf(hifiApiBaseUrl) + hifiMirrors).distinct()
            "custom" -> listOf(hifiApiBaseUrl)
            else -> (listOf(hifiSource) + hifiMirrors + hifiApiBaseUrl).distinct()
        }

        for (baseUrl in urlsToTry) {
            val lyric = runCatching { hifiApi.getLyrics(baseUrl, track) }.getOrNull()
            if (lyric != null) {
                return listOf(
                    Lyrics(
                        id = track.id,
                        title = track.title,
                        subtitle = track.subtitle,
                        lyrics = lyric
                    )
                ).toFeed()
            }
        }
        return emptyList<Lyrics>().toFeed()
    }

    override suspend fun loadLyrics(lyrics: Lyrics): Lyrics = lyrics

    override val forms: List<LoginClient.Form> = listOf(
        LoginClient.Form(
            key = "login",
            label = "Login",
            icon = LoginClient.InputField.Type.Email,
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
        ),
        LoginClient.Form(
            key = "ytm",
            label = "YouTube Music",
            icon = LoginClient.InputField.Type.Password,
            inputFields = listOf(
                LoginClient.InputField(
                    type = LoginClient.InputField.Type.Password,
                    key = "cookie",
                    label = "Cookie",
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
            "ytm" -> {
                val cookie = data["cookie"]!!
                return listOf(
                    User(
                        id = "ytm",
                        name = "YouTube Music",
                        extras = mapOf(
                            "ytmCookie" to cookie
                        )
                    )
                )
            }
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
    private var _ytmCookie: String? = null
    override fun setLoginUser(user: User?) {
        _session = user?.extras?.get("session")
        _ytmCookie = user?.extras?.get("ytmCookie")
    }

    override suspend fun getCurrentUser(): User? = null

    // ======= LibraryFeedClient ===== //

    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        val shelves = mutableListOf<Shelf>()

        if (_session != null) {
            val session = _session!!
            val favorites = dabApi.getFavourites(dabApiBaseUrl, session)
            val likedTracks = favorites.track.map { it.toTrack(json) }

            val favShelf = Shelf.Lists.Items(
                id = "fav",
                title = "Favourites",
                list = likedTracks,
                type = Shelf.Lists.Type.Linear,
            )
            shelves.add(favShelf)

            val playlists = dabApi.getPlaylists(dabApiBaseUrl, session).libraries.map { it.toPlaylist() }

            val playlistShelf = Shelf.Lists.Items(
                id = "playlist",
                title = "Playlists",
                list = playlists,
                type = Shelf.Lists.Type.Linear,
            )
            shelves.add(playlistShelf)
        }

        if (_ytmCookie != null) {
            val ytmCookie = _ytmCookie!!
            val ytmPlaylists = youtubeMusicApi.getLibraryPlaylists(ytmCookie).mapNotNull { renderer ->
                val browseId = renderer["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val title = renderer["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                Playlist(
                    id = "ytm_$browseId",
                    title = title,
                    cover = null,
                    isRadioSupported = true
                )
            }
            if (ytmPlaylists.isNotEmpty()) {
                val ytmShelf = Shelf.Lists.Items(
                    id = "ytm_playlists",
                    title = "YouTube Music Playlists",
                    list = ytmPlaylists,
                    type = Shelf.Lists.Type.Linear,
                )
                shelves.add(ytmShelf)
            }
        }
        
        if (shelves.isEmpty()) {
            throw ClientException.LoginRequired()
        }

        return shelves.toFeed()
    }

    // ====== PlaylistClient ====== //
    override suspend fun loadPlaylist(playlist: Playlist): Playlist = playlist

    override suspend fun loadTracks(playlist: Playlist): Feed<Track> {
        if (playlist.id.startsWith("ytm_")) {
            val browseId = playlist.id.removePrefix("ytm_")
            val ytmCookie = _ytmCookie ?: throw ClientException.LoginRequired()
            val tracks = youtubeMusicApi.getPlaylist(browseId, ytmCookie).map { it.toTrack() }
            return tracks.toFeed()
        }
        throw ClientException.NotSupported("Only YouTube Music playlists are supported.")
    }

    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? = null

    // ====== RadioClient ====== //
    override suspend fun loadRadio(radio: Radio): Radio = radio

    override suspend fun loadTracks(radio: Radio): Feed<Track> {
        val tracks = youtubeMusicApi.getUpNext(radio.id).map { it.toTrack() }
        return tracks.toFeed()
    }

    override suspend fun radio(item: EchoMediaItem, context: EchoMediaItem?): Radio {
        return when (item) {
            is Track -> {
                val ytmVideoId = item.extras["youtubeVideoId"] ?: item.id
                Radio(
                    id = ytmVideoId,
                    title = "Radio",
                    subtitle = item.title,
                    cover = item.cover
                )
            }
            else -> throw ClientException.NotSupported("Radio not supported for this item.")
        }
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
        private const val HIFI_SOURCE = "hifi_source"
        private const val HIFI_API_BASE_URL = "hifi_api_base_url"
        private const val DEFAULT_HIFI_API_BASE_URL = "https://qobuz.kennyy.com.br"

        private const val DAB_API_BASE_URL = "dab_api_base_url"
        private const val DEFAULT_DAB_API_BASE_URL = "https://dab.yeet.su/api"

        val metadata = Metadata(
            className = "dev.brahmkshatriya.echo.extensions.builtin.dabyeet.DabYeetExtension",
            path = "",
            importType = ImportType.BuiltIn,
            type = ExtensionType.MUSIC,
            id = "dab_yeet",
            name = "Hi-Res Music & Lyrics",
            description = "High-fidelity streaming and word-by-word lyrics via hifi-api & DAB",
            version = "v${BuildConfig.VERSION_CODE}",
            author = "Echo Team",
            icon = "https://raw.githubusercontent.com/BitFable/echo-dab-yeet-extension/refs/heads/image-branch/music.png".toImageHolder()
        )
    }
}
