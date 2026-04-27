package dev.brahmkshatriya.echo.extensions.builtin.dabyeet.models

import kotlinx.serialization.Serializable

@Serializable
data class AudioQuality(
    val maximumBitDepth: Int,
    val maximumSamplingRate: Double,
    val isHiRes: Boolean
)