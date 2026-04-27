package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models

import kotlinx.serialization.Serializable

@Serializable
data class GenericResponse(
    val message: String? = null
)