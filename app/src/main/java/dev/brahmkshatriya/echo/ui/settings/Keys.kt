package dev.brahmkshatriya.echo.ui.settings

object Keys {
    const val STATUS = "v4_status"
    const val LOCAL_INTELLIGENCE = "v4_local_intelligence_enabled"
    const val SOUND_PROFILE = "v4_sound_profile"
    const val CONTEXTUAL_HOME = "v4_contextual_home"
    const val SMART_QUEUE_MODE = "v4_smart_queue_mode"
    const val QUEUE_VARIETY = "v4_queue_variety"
    const val COMMAND_PALETTE = "v4_command_palette"
    const val MULTI_SOURCE_SEARCH = "v4_multi_source_search"
    const val SOURCE_FUSION = "v4_source_fusion"
    const val AUDIO_FINGERPRINT = "v4_audio_fingerprint"
    const val PLAYLIST_ALCHEMIST = "v4_playlist_alchemist"
    const val LIBRARY_HEALTH = "v4_library_health"
    const val OFFLINE_DISCOVERY = "v4_offline_discovery"
    const val SMART_DOWNLOADS = "v4_smart_downloads"
    const val VISUAL_PLAYER = "v4_visual_player"
    const val ADAPTIVE_AUDIO = "v4_adaptive_audio"
    const val LYRICS_STUDIO = "v4_lyrics_studio"
    const val LYRICS_OFFSET = "v4_lyrics_offset"
    const val EXTENSION_INSPECTOR = "v4_extension_inspector"

    const val LYRICS_STYLE = "v4_lyrics_style"
    const val LYRICS_FULLSCREEN = "v4_lyrics_fullscreen"
    const val LYRICS_VISUALIZER = "v4_lyrics_visualizer"
    const val LYRICS_TRANSLATION = "v4_lyrics_translation"
    const val LYRICS_FONT_SIZE = "v4_lyrics_font_size"
    const val LYRICS_BG_BLUR = "v4_lyrics_bg_blur"
    const val LYRICS_TEXT_COLOR = "v4_lyrics_text_color"
    const val KARAOKE_FULLSCREEN = "v4_karaoke_fullscreen"

    const val PLAYER_CINEMATIC = "v4_player_cinematic"
    const val PLAYER_VINYL = "v4_player_vinyl"
    const val PLAYER_WAVE = "v4_player_wave"
    const val PLAYER_CARD = "v4_player_card"
    const val ALBUM_ART_SWIPE = "v4_album_art_swipe"
    const val MOOD_COLORS = "v4_mood_colors"
    const val WAVEFORM_SEEKBAR = "v4_waveform_seekbar"

    const val LYRICS_FLOATING_BUBBLE = "v4_floating_lyrics_bubble"

    val switchDefaults = mapOf(
        LOCAL_INTELLIGENCE to true,
        CONTEXTUAL_HOME to true,
        COMMAND_PALETTE to true,
        MULTI_SOURCE_SEARCH to true,
        SOURCE_FUSION to true,
        AUDIO_FINGERPRINT to false,
        LIBRARY_HEALTH to true,
        OFFLINE_DISCOVERY to true,
        ADAPTIVE_AUDIO to true,
        LYRICS_STUDIO to true,
        EXTENSION_INSPECTOR to true,
        LYRICS_FULLSCREEN to false,
        LYRICS_VISUALIZER to false,
        LYRICS_TRANSLATION to false,
        KARAOKE_FULLSCREEN to false,
        MOOD_COLORS to true,
        WAVEFORM_SEEKBAR to false,
        LYRICS_FLOATING_BUBBLE to false
    )
}
