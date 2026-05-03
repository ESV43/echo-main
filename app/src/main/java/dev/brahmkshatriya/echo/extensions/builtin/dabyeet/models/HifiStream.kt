package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models

import dev.brahmkshatriya.echo.extensions.builtin.dabyeet.utils.IntToString
import kotlinx.serialization.Serializable

@Serializable
data class HifiTrackManifestResponse(
    val data: HifiTrackManifestEnvelope
)

@Serializable
data class HifiTrackManifestEnvelope(
    val data: HifiTrackManifestData
)

@Serializable
data class HifiTrackManifestData(
    val attributes: HifiTrackManifestAttributes
)

@Serializable
data class HifiTrackManifestAttributes(
    val uri: String,
    val formats: List<String> = emptyList()
)

@Serializable
data class HifiTrackResponse(
    val data: HifiTrackData
)

@Serializable
data class HifiTrackData(
    val manifestMimeType: String,
    val manifest: String
)

@Serializable
data class HifiBtsManifest(
    val urls: List<String> = emptyList()
)

@Serializable
data class HifiSearchResponse(
    val data: HifiSearchData
)

@Serializable
data class HifiSearchData(
    val items: List<HifiSearchTrack> = emptyList()
)

@Serializable
data class HifiSearchTrack(
    @Serializable(with = IntToString::class)
    val id: String,
    val title: String,
    val artist: HifiSearchArtist? = null,
    val artists: List<HifiSearchArtist> = emptyList()
) {
    fun matches(titleQuery: String, artistQuery: String?): Boolean {
        val normalizedTitle = title.normalizeForMatch()
        val normalizedTitleQuery = titleQuery.normalizeForMatch()
        val artistNames = (artists + listOfNotNull(artist)).map { it.name.normalizeForMatch() }
        val normalizedArtistQuery = artistQuery?.normalizeForMatch().orEmpty()

        return normalizedTitle.contains(normalizedTitleQuery) ||
                normalizedTitleQuery.contains(normalizedTitle) ||
                normalizedArtistQuery.isBlank() ||
                artistNames.any {
                    it.contains(normalizedArtistQuery) || normalizedArtistQuery.contains(it)
                }
    }

    private fun String.normalizeForMatch() = lowercase()
        .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

@Serializable
data class HifiSearchArtist(
    @Serializable(with = IntToString::class)
    val id: String = "",
    val name: String
)

@Serializable
data class HifiLyricsResponse(
    val data: HifiLyricsData
)

@Serializable
data class HifiLyricsData(
    val lines: List<HifiLyricLine> = emptyList()
)

@Serializable
data class HifiLyricLine(
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val words: List<HifiLyricWord> = emptyList()
)

@Serializable
data class HifiLyricWord(
    val startTime: Long,
    val endTime: Long,
    val text: String
)
