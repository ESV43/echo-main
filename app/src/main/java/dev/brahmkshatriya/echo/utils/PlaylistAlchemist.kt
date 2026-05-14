package dev.brahmkshatriya.echo.utils

import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.fusionKey

object PlaylistAlchemist {

    fun List<Track>.deduplicate(): List<Track> {
        val seen = mutableSetOf<String>()
        return filter { track ->
            val key = "${track.title.lowercase().trim()}|${track.artists.joinToString(",") { it.name.lowercase().trim() }}"
            seen.add(key)
        }
    }

    fun List<Track>.sortByVibe(): List<Track> {
        if (isEmpty()) return this
        val first = first()
        val genres = first.genres.map { it.lowercase() }.toSet()
        return sortedByDescending { track ->
            track.genres.count { it.lowercase() in genres }
        }
    }

    fun List<Track>.sortByEnergy(): List<Track> {
        return sortedBy { it.duration ?: 0L }
    }

    fun List<Track>.sortByDate(): List<Track> {
        // Mock sorting by date as we don't have release date in all tracks reliably
        return sortedByDescending { it.id } 
    }
}
