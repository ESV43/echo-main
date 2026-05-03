package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network

import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toServerMedia
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiBtsManifest
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiLyricsResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiSearchResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiTrackManifestResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiTrackResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiUrlResponse
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class HifiApiService(client: OkHttpClient, json: Json) : BaseHttpClient(client, json) {

    suspend fun getStream(
        baseUrl: String,
        streamable: Streamable,
        isDownload: Boolean
    ): Streamable.Media {
        val trackId = streamable.extras["hifiTrackId"]
            ?: resolveTrackId(baseUrl, streamable)

        // 1. Try /track/url (Monochrome/Qobuz Direct)
        runCatching {
            get<HifiUrlResponse>(
                url = "$baseUrl/track/url",
                params = mapOf(
                    "id" to trackId,
                    "quality" to "27" // HI_RES
                )
            )
        }.getOrNull()?.let { response ->
            val url = response.url ?: response.data
            if (!url.isNullOrBlank()) return url.toServerMedia()
        }

        // 2. Try /track/stream (Fallback for some instances)
        runCatching {
            get<HifiUrlResponse>(
                url = "$baseUrl/track/stream",
                params = mapOf(
                    "id" to trackId,
                    "quality" to "27"
                )
            )
        }.getOrNull()?.let { response ->
            val url = response.url ?: response.data
            if (!url.isNullOrBlank()) return url.toServerMedia()
        }

        // 3. Try /trackManifests (Tidal Legacy)
        val manifestResponse = runCatching {
            get<HifiTrackManifestResponse>(
                url = "$baseUrl/trackManifests",
                params = mapOf(
                    "id" to trackId,
                    "formats" to "FLAC_HIRES,FLAC,AACLC,HEAACV1",
                    "adaptive" to "true",
                    "manifestType" to "MPEG_DASH",
                    "uriScheme" to "HTTPS",
                    "usage" to if (isDownload) "DOWNLOAD" else "PLAYBACK"
                )
            )
        }.getOrNull()

        manifestResponse?.data?.data?.attributes?.uri?.takeIf { it.isNotBlank() }?.let {
            return it.toServerMedia(type = Streamable.SourceType.DASH)
        }

        // 4. Legacy Fallback
        return getLegacyTrackStream(baseUrl, trackId)
    }

    suspend fun getLyrics(baseUrl: String, track: dev.brahmkshatriya.echo.common.models.Track): Lyrics.Lyric? {
        val trackId = track.id.toLongOrNull()?.toString() ?: runCatching {
            val searchQuery = listOf(track.title, track.artists.firstOrNull()?.name.orEmpty())
                .filter { it.isNotBlank() }.joinToString(" ")
            
            get<HifiSearchResponse>(
                url = "$baseUrl/search",
                params = mapOf("s" to searchQuery)
            ).data.items.firstOrNull()?.id
        }.getOrNull() ?: return null

        val response = runCatching {
            get<HifiLyricsResponse>(
                url = "$baseUrl/lyrics",
                params = mapOf(
                    "id" to trackId,
                    "source" to "apple_music"
                )
            )
        }.getOrNull() ?: return null

        val lines = response.data.lines
        if (lines.isEmpty()) return null

        val isWordByWord = lines.any { it.words.isNotEmpty() }
        return if (isWordByWord) {
            Lyrics.WordByWord(lines.map { line ->
                line.words.map { word ->
                    Lyrics.Item(word.text, word.startTime, word.endTime)
                }
            })
        } else {
            Lyrics.Timed(lines.map { line ->
                Lyrics.Item(line.text, line.startTime, line.endTime)
            })
        }
    }

    private suspend fun resolveTrackId(baseUrl: String, streamable: Streamable): String {
        streamable.id.toLongOrNull()?.let { return it.toString() }

        val searchQuery = streamable.extras["searchQuery"]
            ?: throw IllegalStateException("Missing hifi-api search query for ${streamable.id}")
        val title = streamable.extras["title"].orEmpty()
        val artist = streamable.extras["artist"]
        
        val response = runCatching {
            get<HifiSearchResponse>(
                url = "$baseUrl/search",
                params = mapOf("s" to searchQuery)
            )
        }.getOrElse { e ->
            throw Exception("Failed to search track on hifi-api: ${e.message}. Use search query: $searchQuery", e)
        }

        return response.data.items
            .firstOrNull { title.isBlank() || it.matches(title, artist) }
            ?.id
            ?: response.data.items.firstOrNull()?.id
            ?: throw IllegalStateException("No hifi-api match found for $searchQuery")
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun getLegacyTrackStream(baseUrl: String, trackId: String): Streamable.Media {
        val response = get<HifiTrackResponse>(
            url = "$baseUrl/track",
            params = mapOf(
                "id" to trackId,
                "quality" to "HI_RES_LOSSLESS"
            )
        )

        return when (response.data.manifestMimeType) {
            "application/vnd.tidal.bts" -> {
                val manifestJson = Base64.decode(response.data.manifest).decodeToString()
                val streamUrl = json.decodeFromString<HifiBtsManifest>(manifestJson)
                    .urls
                    .firstOrNull()
                    ?: throw IllegalStateException("hifi-api returned an empty BTS manifest")
                streamUrl.toServerMedia()
            }
            "application/dash+xml" -> {
                val manifestDataUrl = "data:application/dash+xml;base64,${response.data.manifest}"
                manifestDataUrl.toServerMedia(type = Streamable.SourceType.DASH)
            }
            else -> throw IllegalStateException("Unsupported hifi-api manifest type: ${response.data.manifestMimeType}")
        }
    }
}
