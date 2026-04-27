package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FavouriteResponse {
    @SerialName("favorites")
    val track: List<Track> = emptyList()
}