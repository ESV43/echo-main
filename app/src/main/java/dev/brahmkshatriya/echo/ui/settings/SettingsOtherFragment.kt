package dev.brahmkshatriya.echo.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialSwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.extensions.builtin.unified.extensionId
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import dev.brahmkshatriya.echo.utils.PermsUtils.registerActivityResultLauncher
import dev.brahmkshatriya.echo.utils.exportSettings
import dev.brahmkshatriya.echo.utils.importSettings
import dev.brahmkshatriya.echo.utils.ui.prefs.MaterialMultipleChoicePreference
import dev.brahmkshatriya.echo.utils.ui.prefs.TransitionPreference
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SettingsOtherFragment : BaseSettingsFragment() {
    override val title get() = getString(R.string.other_settings)
    override val icon get() = R.drawable.ic_more_horiz.toResourceImageHolder()
    override val creator = { OtherPreference() }

    class OtherPreference : PreferenceFragmentCompat() {

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            configure()
        }

        private val extensionLoader: ExtensionLoader by inject()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceManager.sharedPreferencesName = SETTINGS_NAME
            preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
            val screen = preferenceManager.createPreferenceScreen(context)
            preferenceScreen = screen

            PreferenceCategory(context).apply {
                title = getString(R.string.v4_sources_and_library)
                key = "v4_sources_and_library"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialSwitchPreference(context).apply {
                    key = Keys.SOURCE_FUSION
                    title = getString(R.string.v4_source_fusion)
                    summary = getString(R.string.v4_source_fusion_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                        screen.findPreference<Preference>(Keys.AUDIO_FINGERPRINT)?.isEnabled = newValue as Boolean
                        true
                    }
                    addPreference(this)
                }

                MaterialSwitchPreference(context).apply {
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
                    setDefaultValue(setOf("dedupe", "smart_queue", "source_fusion"))
                    addPreference(this)
                }

                MaterialSwitchPreference(context).apply {
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

                MaterialSwitchPreference(context).apply {
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
                    setDefaultValue(setOf("liked", "frequent", "wifi", "storage"))
                    addPreference(this)
                }
            }

            // ── Updates ────────────────────────────────────────────
            PreferenceCategory(context).apply {
                title = getString(R.string.check_for_updates)
                key = "updates_category"
                isIconSpaceReserved = false
                layoutResource = R.layout.preference_category
                screen.addPreference(this)

                MaterialSwitchPreference(context).apply {
                    title = getString(R.string.check_for_updates)
                    summary = getString(R.string.check_for_updates_summary)
                    key = "check_for_updates"
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                TransitionPreference(context).apply {
                    key = "check_now"
                    title = getString(R.string.check_now)
                    summary = getString(R.string.check_now_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                    setOnPreferenceClickListener {
                        val viewModel by activityViewModel<ExtensionsViewModel>()
                        viewModel.update(requireActivity(), true)
                        true
                    }
                }
            }

            // ── Advanced ──────────────────────────────────────────────
            PreferenceCategory(context).apply {
                title = getString(R.string.advanced)
                key = "advanced_category"
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
                    setOnPreferenceClickListener {
                        showStatusDialog()
                        true
                    }
                }

                MaterialSwitchPreference(context).apply {
                    key = Keys.EXTENSION_INSPECTOR
                    title = getString(R.string.v4_extension_inspector)
                    summary = getString(R.string.v4_extension_inspector_summary)
                    layoutResource = R.layout.preference_switch
                    isIconSpaceReserved = false
                    setDefaultValue(true)
                    addPreference(this)
                }

                TransitionPreference(context).apply {
                    key = "export"
                    title = getString(R.string.export_settings)
                    summary = getString(R.string.export_settings_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                    setOnPreferenceClickListener {
                        val contract = ActivityResultContracts.CreateDocument("application/json")
                        requireActivity().registerActivityResultLauncher(contract) { uri ->
                            uri?.let { context.exportSettings(it) }
                        }.launch("echo-settings.json")
                        true
                    }
                }

                TransitionPreference(context).apply {
                    key = "import"
                    title = getString(R.string.import_settings)
                    summary = getString(R.string.import_settings_summary)
                    layoutResource = R.layout.preference
                    isIconSpaceReserved = false
                    addPreference(this)
                    setOnPreferenceClickListener {
                        val contract = ActivityResultContracts.OpenDocument()
                        requireActivity().registerActivityResultLauncher(contract) {
                            it?.let {
                                context.importSettings(it)
                                requireActivity().recreate()
                            }
                        }.launch(arrayOf("application/json"))
                        true
                    }
                }
            }

            val prefs = preferenceManager.sharedPreferences
            val sourceFusion = prefs?.getBoolean(Keys.SOURCE_FUSION, true) ?: true
            screen.findPreference<Preference>(Keys.AUDIO_FINGERPRINT)?.isEnabled = sourceFusion
        }

        private fun showStatusDialog() {
            val prefs = preferenceManager.sharedPreferences ?: return
            val enabled = Keys.switchDefaults.count { (key, default) ->
                prefs.getBoolean(key, default)
            }

            val unified = extensionLoader.music.value.find {
                it.id == UnifiedExtension.UNIFIED_ID
            }?.instance?.value as? UnifiedExtension

            lifecycleScope.launch {
                val health = if (prefs.getBoolean(Keys.LIBRARY_HEALTH, true)) {
                    unified?.db?.getLibraryHealth() ?: mapOf()
                } else mapOf()
                val history = unified?.db?.getRecentlyPlayed(1000) ?: listOf()
                val plays = history.sumOf { (unified?.db?.getHistory(it.id, it.extras.extensionId)?.playCount ?: 0) }
                val skips = history.sumOf { (unified?.db?.getHistory(it.id, it.extras.extensionId)?.skipCount ?: 0) }
                val mostPlayed = unified?.db?.getMostPlayed(5) ?: listOf()
                val topTracks = mostPlayed.joinToString(", ") { it.title.take(20) }

                val message = getString(
                    R.string.v4_release_dashboard_message,
                    enabled,
                    health["broken"] ?: 0,
                    health["duplicates"] ?: 0,
                    plays,
                    skips,
                    topTracks.ifEmpty { "—" },
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
}
