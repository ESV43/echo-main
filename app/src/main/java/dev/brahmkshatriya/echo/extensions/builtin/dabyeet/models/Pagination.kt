package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models

import kotlinx.serialization.Serializable

@Serializable
data class Pagination(
    val offset: Int? = null,
    val limit: Int,
    val total: Int,
    val hasMore: Boolean? = false,
    val returned: Int? = 0
)