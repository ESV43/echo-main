package dev.brahmkshatriya.echo.ui.settings


import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.CACHE_SIZE
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.CLOSE_PLAYER
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.CROSSFADE
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.CROSSFADE_DURATION
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.FADE_CONTROLS
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.FADE_DURATION
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.FLUID_LYRICS
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.KEY_AI_AUTO_EQ
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.MORE_BRAIN_CAPACITY
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.PREFERRED_LYRICS_SOURCE
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.SKIP_SILENCE
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.STREAM_QUALITY
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.UNMETERED_STREAM_QUALITY
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.streamQualities
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.AUTO_GAIN
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.REPLAY_GAIN
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.BPM_CROSSFADE
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.AUTO_SKIP
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.PREDICTIVE_CACHE
import dev.brahmkshatriya.echo.playback.listener.PlayerRadio.Companion.AUTO_START_RADIO
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel.Companion.KEEP_QUEUE
import dev.brahmkshatriya.echo.ui.settings.AudioEffectsFragment.Companion.AUDIO_FX
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.ui.settings.Keys
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialMultipleChoicePreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSliderPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.TransitionPreference

class SettingsPlayerFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.player)
    override val icon get() = R.drawable.ic_play_circle.toResourceImageHolder()
    override val creator = { AudioPreference() }

    class AudioPreference : PreferenceFragmentCompat() {

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            configure()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = SETTINGS_NAME
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            PreferenceCategory(context).apply {
                title = getString(R.string.playback)
                key = "playback"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                TransitionPreference(context).apply {
                    key = AUDIO_FX
                    title = getString(R.string.audio_fx)
                    summary = getString(R.string.audio_fx_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = STREAM_QUALITY
                    title = getString(R.string.stream_quality)
                    summary = getString(R.string.stream_quality_summary)
                    entries = context.resources.getStringArray(R.array.stream_qualities)
                    entryValues = streamQualities
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue(streamQualities[1])
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = UNMETERED_STREAM_QUALITY
                    title = getString(R.string.unmetered_stream_quality)
                    summary = getString(R.string.unmetered_stream_quality_summary)
                    entries =
                        context.resources.getStringArray(R.array.stream_qualities) + getString(R.string.off)
                    entryValues = streamQualities + "off"
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("off")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = KEY_AI_AUTO_EQ
                    title = getString(R.string.ai_auto_eq)
                    summary = getString(R.string.ai_auto_eq_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = AUTO_GAIN
                    title = getString(R.string.v4_auto_gain)
                    summary = getString(R.string.v4_auto_gain_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = REPLAY_GAIN
                    title = getString(R.string.v4_replay_gain)
                    summary = getString(R.string.v4_replay_gain_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = Keys.VISUAL_PLAYER
                    title = getString(R.string.v4_visual_player_modes)
                    summary = getString(R.string.v4_visual_player_modes_summary)
                    entries = context.resources.getStringArray(R.array.v4_visual_player_modes)
                    entryValues = context.resources.getStringArray(R.array.v4_visual_player_values)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("immersive")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.ADAPTIVE_AUDIO
                    title = getString(R.string.v4_adaptive_audio_profiles)
                    summary = getString(R.string.v4_adaptive_audio_profiles_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }

            PreferenceCategory(context).apply {
                title = getString(R.string.behavior)
                key = "behavior"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = KEEP_QUEUE
                    title = getString(R.string.keep_queue)
                    summary = getString(R.string.keep_queue_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = CLOSE_PLAYER
                    title = getString(R.string.stop_player)
                    summary = getString(R.string.stop_player_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = SKIP_SILENCE
                    title = getString(R.string.skip_silence)
                    summary = getString(R.string.skip_silence_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = FADE_CONTROLS
                    title = getString(R.string.fade_controls)
                    summary = getString(R.string.fade_controls_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 100, 1500, steps = 100, allowOverride = true)
                    .apply {
                        key = FADE_DURATION
                        title = getString(R.string.fade_duration)
                        summary = getString(R.string.fade_duration_summary)
                        dependency = FADE_CONTROLS
                        isIconSpaceReserved = false
                        setDefaultValue(300)
                        addPreference(this)
                    }

                SwitchPreferenceCompat(context).apply {
                    key = CROSSFADE
                    title = getString(R.string.crossfade)
                    summary = getString(R.string.crossfade_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                 MaterialSliderPreference(context, 1, 12, allowOverride = true).apply {
                    key = CROSSFADE_DURATION
                    title = getString(R.string.crossfade_duration)
                    summary = getString(R.string.crossfade_duration_summary)
                    dependency = CROSSFADE
                    isIconSpaceReserved = false
                    setDefaultValue(5)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = BPM_CROSSFADE
                    title = getString(R.string.v4_bpm_crossfade)
                    summary = getString(R.string.v4_bpm_crossfade_summary)
                    dependency = CROSSFADE
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = MORE_BRAIN_CAPACITY
                    title = getString(R.string.more_brain_capacity)
                    summary = getString(R.string.more_brain_capacity_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = AUTO_START_RADIO
                    title = getString(R.string.auto_start_radio)
                    summary = getString(R.string.auto_start_radio_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = AUTO_SKIP
                    title = getString(R.string.v4_auto_skip)
                    summary = getString(R.string.v4_auto_skip_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 200, 1000, allowOverride = true).apply {
                    key = CACHE_SIZE
                    title = getString(R.string.cache_size)
                    summary = getString(R.string.cache_size_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(250)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = PREDICTIVE_CACHE
                    title = getString(R.string.v4_predictive_cache)
                    summary = getString(R.string.v4_predictive_cache_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.MOOD_COLORS
                    title = getString(R.string.v4_mood_colors)
                    summary = getString(R.string.v4_mood_colors_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.WAVEFORM_SEEKBAR
                    title = getString(R.string.v4_waveform_seekbar)
                    summary = getString(R.string.v4_waveform_seekbar_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.ALBUM_ART_SWIPE
                    title = getString(R.string.v4_album_art_swipe)
                    summary = getString(R.string.v4_album_art_swipe_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.LYRICS_FLOATING_BUBBLE
                    title = getString(R.string.v4_floating_lyrics_bubble)
                    summary = getString(R.string.v4_floating_lyrics_bubble_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }
            }

            PreferenceCategory(context).apply {
                title = getString(R.string.v4_local_intelligence)
                key = "v4_local_intelligence"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = Keys.LOCAL_INTELLIGENCE
                    title = getString(R.string.v4_local_music_intelligence)
                    summary = getString(R.string.v4_local_music_intelligence_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = Keys.SOUND_PROFILE
                    title = getString(R.string.v4_sound_profile)
                    summary = getString(R.string.v4_sound_profile_summary)
                    entries = context.resources.getStringArray(R.array.v4_sound_profiles)
                    entryValues = context.resources.getStringArray(R.array.v4_sound_profile_values)
                    dependency = Keys.LOCAL_INTELLIGENCE
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("balanced")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.CONTEXTUAL_HOME
                    title = getString(R.string.v4_contextual_home)
                    summary = getString(R.string.v4_contextual_home_summary)
                    dependency = Keys.LOCAL_INTELLIGENCE
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }

            PreferenceCategory(context).apply {
                title = getString(R.string.v4_queue_and_search)
                key = "v4_queue_and_search"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialListPreference(context).apply {
                    key = Keys.SMART_QUEUE_MODE
                    title = getString(R.string.v4_smart_queue_lab)
                    summary = getString(R.string.v4_smart_queue_lab_summary)
                    entries = context.resources.getStringArray(R.array.v4_smart_queue_modes)
                    entryValues = context.resources.getStringArray(R.array.v4_smart_queue_values)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("vibe")
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 100, steps = 5).apply {
                    key = Keys.QUEUE_VARIETY
                    title = getString(R.string.v4_queue_variety)
                    summary = getString(R.string.v4_queue_variety_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(70)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.COMMAND_PALETTE
                    title = getString(R.string.v4_command_palette)
                    summary = getString(R.string.v4_command_palette_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.MULTI_SOURCE_SEARCH
                    title = getString(R.string.v4_multi_source_search)
                    summary = getString(R.string.v4_multi_source_search_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }

            PreferenceCategory(context).apply {
                title = getString(R.string.lyrics)
                key = "lyrics_category"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialListPreference(context).apply {
                    key = Keys.LYRICS_STYLE
                    title = getString(R.string.v4_lyrics_style)
                    summary = getString(R.string.v4_lyrics_style_summary)
                    entries = context.resources.getStringArray(R.array.v4_lyrics_styles)
                    entryValues = context.resources.getStringArray(R.array.v4_lyrics_style_values)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("classic")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = FLUID_LYRICS
                    title = getString(R.string.fluid_lyrics)
                    summary = getString(R.string.fluid_lyrics_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.LYRICS_FULLSCREEN
                    title = getString(R.string.v4_lyrics_fullscreen)
                    summary = getString(R.string.v4_lyrics_fullscreen_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.KARAOKE_FULLSCREEN
                    title = getString(R.string.v4_karaoke_fullscreen)
                    summary = getString(R.string.v4_karaoke_fullscreen_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.LYRICS_VISUALIZER
                    title = getString(R.string.v4_lyrics_visualizer)
                    summary = getString(R.string.v4_lyrics_visualizer_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.LYRICS_TRANSLATION
                    title = getString(R.string.v4_lyrics_translation)
                    summary = getString(R.string.v4_lyrics_translation_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 12, 36, steps = 1).apply {
                    key = Keys.LYRICS_FONT_SIZE
                    title = getString(R.string.v4_lyrics_font_size)
                    summary = getString(R.string.v4_lyrics_font_size_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(24)
                    addPreference(this)
                }

                MaterialSliderPreference(context, 0, 25, steps = 1).apply {
                    key = Keys.LYRICS_BG_BLUR
                    title = getString(R.string.v4_lyrics_bg_blur)
                    summary = getString(R.string.v4_lyrics_bg_blur_summary)
                    isIconSpaceReserved = false
                    setDefaultValue(0)
                    addPreference(this)
                }

                MaterialListPreference(context).apply {
                    key = PREFERRED_LYRICS_SOURCE
                    title = getString(R.string.preferred_lyrics_source)
                    summary = getString(R.string.preferred_lyrics_source_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    entries = arrayOf("Auto")
                    entryValues = arrayOf("auto")
                    setDefaultValue("auto")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.LYRICS_STUDIO
                    title = getString(R.string.v4_lyrics_studio)
                    summary = getString(R.string.v4_lyrics_studio_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.EXTENSION_INSPECTOR
                    title = getString(R.string.v4_extension_inspector)
                    summary = getString(R.string.v4_extension_inspector_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            val view = listView.findViewById<View>(preference.key.hashCode())
            return when (preference.key) {
                AUDIO_FX -> {
                    requireActivity().openFragment<AudioEffectsFragment>(view)
                    true
                }

                else -> false
            }
        }
    }
}
