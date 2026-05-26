package dev.brahmkshatriya.echo.ui.player.more.lyrics

enum class LyricsStyle(val value: String) {
    CLASSIC("classic"),
    KARAOKE("karaoke"),
    SCROLL("scroll"),
    COMPACT("compact"),
    NEON("neon");

    companion object {
        fun fromValue(value: String?): LyricsStyle = entries.find { it.value == value } ?: CLASSIC
    }
}
