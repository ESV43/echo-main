package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val tracks: List<Track>? = null,
    val albums: List<Album>? = null,
    val pagination: Pagination,
)