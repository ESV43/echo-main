package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialListPreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialMultipleChoicePreference
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSliderPreference
import androidx.lifecycle.lifecycleScope
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.utils.ui.prefs.TransitionPreference
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class V4LabFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.v4_lab)
    override val icon get() = R.drawable.ic_autorenew.toResourceImageHolder()
    override val creator = { V4Preference() }

    class V4Preference : PreferenceFragmentCompat() {

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
                title = getString(R.string.v4_status)
                key = "v4_status_category"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                TransitionPreference(context).apply {
                    key = Keys.STATUS
                    title = getString(R.string.v4_release_dashboard)
                    summary = getString(R.string.v4_release_dashboard_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
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
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue("balanced")
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.CONTEXTUAL_HOME
                    title = getString(R.string.v4_contextual_home)
                    summary = getString(R.string.v4_contextual_home_summary)
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
                title = getString(R.string.v4_sources_and_library)
                key = "v4_sources_and_library"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = Keys.SOURCE_FUSION
                    title = getString(R.string.v4_source_fusion)
                    summary = getString(R.string.v4_source_fusion_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.AUDIO_FINGERPRINT
                    title = getString(R.string.v4_audio_fingerprint)
                    summary = getString(R.string.v4_audio_fingerprint_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(false)
                    addPreference(this)
                }

                MaterialMultipleChoicePreference(context).apply {
                    key = Keys.PLAYLIST_ALCHEMIST
                    title = getString(R.string.v4_playlist_alchemist)
                    summary = getString(R.string.v4_playlist_alchemist_summary)
                    entries = context.resources.getStringArray(R.array.v4_playlist_tools)
                    entryValues = context.resources.getStringArray(R.array.v4_playlist_tool_values)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue(setOf("dedupe", "repair", "sort"))
                    addPreference(this)
                }

                SwitchPreferenceCompat(context).apply {
                    key = Keys.LIBRARY_HEALTH
                    title = getString(R.string.v4_library_health)
                    summary = getString(R.string.v4_library_health_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }
            }

            PreferenceCategory(context).apply {
                title = getString(R.string.v4_offline_and_downloads)
                key = "v4_offline_and_downloads"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                SwitchPreferenceCompat(context).apply {
                    key = Keys.OFFLINE_DISCOVERY
                    title = getString(R.string.v4_offline_discovery)
                    summary = getString(R.string.v4_offline_discovery_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                MaterialMultipleChoicePreference(context).apply {
                    key = Keys.SMART_DOWNLOADS
                    title = getString(R.string.v4_smart_downloads)
                    summary = getString(R.string.v4_smart_downloads_summary)
                    entries = context.resources.getStringArray(R.array.v4_smart_download_rules)
                    entryValues = context.resources.getStringArray(R.array.v4_smart_download_values)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    setDefaultValue(setOf("liked", "wifi", "storage"))
                    addPreference(this)
                }
            }

            PreferenceCategory(context).apply {
                title = getString(R.string.v4_player_and_lyrics)
                key = "v4_player_and_lyrics"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

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
            return when (preference.key) {
                Keys.STATUS -> {
                    showStatusDialog()
                    true
                }

                else -> false
            }
        }

        private val extensionLoader: ExtensionLoader by inject()

        private fun showStatusDialog() {
            val prefs = preferenceManager.sharedPreferences ?: return
            val enabled = Keys.switchDefaults.count { (key, default) ->
                prefs.getBoolean(key, default)
            }

            val unified = extensionLoader.music.value.find {
                it.id == UnifiedExtension.UNIFIED_ID
            }?.instance?.value?.getOrNull() as? UnifiedExtension

            lifecycleScope.launch {
                val health = unified?.db?.getLibraryHealth() ?: mapOf()
                val history = unified?.db?.getRecentlyPlayed(1000) ?: listOf()
                val plays = history.sumOf { (unified?.db?.dao?.getHistory(it.id, it.extras.extensionId)?.playCount ?: 0) }
                val skips = history.sumOf { (unified?.db?.dao?.getHistory(it.id, it.extras.extensionId)?.skipCount ?: 0) }

                val message = getString(
                    R.string.v4_release_dashboard_message,
                    enabled,
                    health["broken"] ?: 0,
                    health["duplicates"] ?: 0,
                    plays,
                    skips,
                    prefs.getString(Keys.SMART_QUEUE_MODE, "vibe"),
                    prefs.getString(Keys.VISUAL_PLAYER, "immersive")
                )
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.v4_release_dashboard)
                    .setMessage(message)
                    .setPositiveButton(R.string.okay) { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            }
        }
    }

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
            EXTENSION_INSPECTOR to true
        )
    }
}
