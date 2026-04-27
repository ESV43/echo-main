package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models

import kotlinx.serialization.Serializable

@Serializable
data class LibraryResponse(
    val library: LibraryItem
)