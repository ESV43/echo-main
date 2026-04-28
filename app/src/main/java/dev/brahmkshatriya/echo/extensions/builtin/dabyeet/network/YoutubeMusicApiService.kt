package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class YoutubeMusicApiService(client: OkHttpClient, json: Json) : BaseHttpClient(client, json) {

    data class Track(
        val videoId: String,
        val title: String,
        val artists: List<String>,
        val album: String?,
        val thumbnail: String?,
        val durationSeconds: Long?
    )

    suspend fun searchTracks(query: String): List<Track> {
        val body = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240408.01.00")
                })
            })
            put("query", query)
            put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBQ%3D%3D")
        }.toString()

        val request = Request.Builder()
            .url("$BASE_URL/search?key=$INNERTUBE_KEY")
            .header("Content-Type", "application/json")
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val response = execute(request).body.string()
        return findObjects(json.parseToJsonElement(response), "musicResponsiveListItemRenderer")
            .mapNotNull { parseTrack(it) }
            .distinctBy { it.videoId }
            .take(25)
    }

    private fun parseTrack(renderer: JsonObject): Track? {
        val videoId = findFirstString(renderer, "videoId") ?: return null
        val columns = renderer["flexColumns"]?.jsonArray.orEmpty()
        val title = columns.firstOrNull()
            ?.let { findFirstString(it, "text") }
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val details = columns.drop(1)
            .flatMap { findRuns(it) }
            .mapNotNull { it["text"]?.jsonPrimitive?.contentOrNull?.trim() }
            .filter { it.isNotBlank() && it != "Song" && it != "Video" && it != "Album" }

        val artists = details.takeWhile { !looksLikeYear(it) && !looksLikeDuration(it) }
            .filterNot { it == title }
            .take(3)
        val album = details.drop(artists.size)
            .firstOrNull { !looksLikeYear(it) && !looksLikeDuration(it) }

        return Track(
            videoId = videoId,
            title = title,
            artists = artists,
            album = album,
            thumbnail = findThumbnails(renderer).maxByOrNull { it.width ?: 0 }?.url,
            durationSeconds = findFirstString(renderer, "text")
                ?.takeIf { looksLikeDuration(it) }
                ?.toDurationSeconds()
        )
    }

    private fun findObjects(element: JsonElement, key: String): List<JsonObject> {
        val results = mutableListOf<JsonObject>()
        fun visit(current: JsonElement) {
            when (current) {
                is JsonObject -> current.forEach { (name, value) ->
                    if (name == key) (value as? JsonObject)?.let(results::add)
                    visit(value)
                }
                is JsonArray -> current.forEach(::visit)
                else -> Unit
            }
        }
        visit(element)
        return results
    }

    private fun findRuns(element: JsonElement): List<JsonObject> {
        val runs = mutableListOf<JsonObject>()
        fun visit(current: JsonElement) {
            when (current) {
                is JsonObject -> current.forEach { (name, value) ->
                    if (name == "runs") runs += value.jsonArray.map { it.jsonObject }
                    visit(value)
                }
                is JsonArray -> current.forEach(::visit)
                else -> Unit
            }
        }
        visit(element)
        return runs
    }

    private fun findFirstString(element: JsonElement, key: String): String? {
        when (element) {
            is JsonObject -> {
                element[key]?.let { value ->
                    (value as? JsonPrimitive)?.contentOrNull?.let { return it }
                }
                element.values.forEach { findFirstString(it, key)?.let { found -> return found } }
            }
            is JsonArray -> element.forEach { findFirstString(it, key)?.let { found -> return found } }
            else -> Unit
        }
        return null
    }

    private data class Thumbnail(val url: String, val width: Int?)

    private fun findThumbnails(element: JsonElement): List<Thumbnail> {
        val thumbnails = mutableListOf<Thumbnail>()
        fun visit(current: JsonElement) {
            when (current) {
                is JsonObject -> {
                    current["thumbnails"]?.let { list ->
                        thumbnails += list.jsonArray.mapNotNull { item ->
                            val obj = item.jsonObject
                            val url = obj["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                            Thumbnail(url, obj["width"]?.jsonPrimitive?.contentOrNull?.toIntOrNull())
                        }
                    }
                    current.values.forEach(::visit)
                }
                is JsonArray -> current.forEach(::visit)
                else -> Unit
            }
        }
        visit(element)
        return thumbnails
    }

    private fun looksLikeDuration(value: String) = Regex("^\\d{1,2}:\\d{2}(?::\\d{2})?$").matches(value)

    private fun looksLikeYear(value: String) = Regex("^\\d{4}$").matches(value)

    private fun String.toDurationSeconds(): Long {
        return split(":").mapNotNull { it.toLongOrNull() }
            .fold(0L) { total, part -> total * 60 + part }
    }

    companion object {
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1"
        private const val INNERTUBE_KEY = "AIzaSyC9XL3ZjWc8w8MFF5B5Q9gF9pyI8uSxX4"
    }
}
