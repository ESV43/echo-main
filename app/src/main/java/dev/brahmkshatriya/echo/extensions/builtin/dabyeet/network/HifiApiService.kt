package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network

import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Streamable.Media.Companion.toServerMedia
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiBtsManifest
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiSearchResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiTrackManifestResponse
import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models.HifiTrackResponse
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

        val response = runCatching {
            get<HifiTrackManifestResponse>(
                url = "$baseUrl/trackManifests/",
                params = mapOf(
                    "id" to trackId,
                    "formats" to "AACLC,FLAC,FLAC_HIRES,HEAACV1",
                    "adaptive" to "true",
                    "manifestType" to "MPEG_DASH",
                    "uriScheme" to "HTTPS",
                    "usage" to if (isDownload) "DOWNLOAD" else "PLAYBACK"
                )
            )
        }.getOrNull()

        val manifestUrl = response?.data?.data?.attributes?.uri
        if (!manifestUrl.isNullOrBlank()) {
            return manifestUrl.toServerMedia(type = Streamable.SourceType.DASH)
        }

        return getLegacyTrackStream(baseUrl, trackId)
    }

    private suspend fun resolveTrackId(baseUrl: String, streamable: Streamable): String {
        streamable.id.toLongOrNull()?.let { return it.toString() }

        val searchQuery = streamable.extras["searchQuery"]
            ?: throw IllegalStateException("Missing hifi-api search query for ${streamable.id}")
        val title = streamable.extras["title"].orEmpty()
        val artist = streamable.extras["artist"]
        val response = get<HifiSearchResponse>(
            url = "$baseUrl/search/",
            params = mapOf("s" to searchQuery)
        )

        return response.data.items
            .firstOrNull { title.isBlank() || it.matches(title, artist) }
            ?.id
            ?: response.data.items.firstOrNull()?.id
            ?: throw IllegalStateException("No hifi-api match found for $searchQuery")
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun getLegacyTrackStream(baseUrl: String, trackId: String): Streamable.Media {
        val response = get<HifiTrackResponse>(
            url = "$baseUrl/track/",
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
