package dev.brahmkshatriya.echo.ui.player.audiofx

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogPlayerAudioFxBinding
import dev.brahmkshatriya.echo.databinding.FragmentAudioFxBinding
import dev.brahmkshatriya.echo.playback.listener.EffectsListener.Companion.BASS_BOOST
import dev.brahmkshatriya.echo.playback.listener.EffectsListener.Companion.CUSTOM_EFFECTS
import dev.brahmkshatriya.echo.playback.listener.EffectsListener.Companion.PLAYBACK_PITCH
import dev.brahmkshatriya.echo.playback.listener.EffectsListener.Companion.PLAYBACK_SPEED
import dev.brahmkshatriya.echo.playback.listener.EffectsListener.Companion.deleteFxPrefs
import dev.brahmkshatriya.echo.playback.listener.EffectsListener.Companion.getFxPrefs
import dev.brahmkshatriya.echo.playback.listener.EffectsListener.Companion.globalFx
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.PermsUtils.registerActivityResultLauncher
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AudioEffectsBottomSheet : BottomSheetDialogFragment() {

    var binding by autoCleared<DialogPlayerAudioFxBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPlayerAudioFxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewModel by activityViewModel<PlayerViewModel>()
        var mediaId: String? = null

        fun bind() {
            val settings = requireContext().globalFx()
            settings.edit {
                val customEffects = settings.getStringSet(CUSTOM_EFFECTS, null) ?: emptySet()
                putStringSet(CUSTOM_EFFECTS, customEffects + mediaId?.hashCode()?.toString())
            }
            binding.audioFxDescription.isVisible = mediaId != null
            val mediaSettings =
                requireContext().getFxPrefs(settings, mediaId?.hashCode()) ?: settings
            binding.audioFxFragment.bind(mediaSettings) { onEqualizerClicked() }
        }
        observe(viewModel.playerState.current) {
            mediaId = it?.mediaItem?.mediaId
            bind()
        }
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_refresh -> {
                    val context = requireContext()
                    val id = mediaId ?: return@setOnMenuItemClickListener false
                    context.deleteFxPrefs(id.hashCode())
                    bind()
                    true
                }

                else -> false
            }
        }
    }

    companion object {
        @SuppressLint("SetTextI18n")
        fun FragmentAudioFxBinding.bind(
            settings: SharedPreferences, onEqualizerClicked: () -> Unit
        ) {
            val currentSpeed = runCatching { settings.getFloat(PLAYBACK_SPEED, 1f) }
                .getOrDefault(1f).coerceIn(0.25f, 4f)
            val currentPitch = runCatching { settings.getFloat(PLAYBACK_PITCH, 1f) }
                .getOrDefault(1f).coerceIn(0.25f, 4f)

            speedValue.text = "%.2fx".format(currentSpeed)
            speedSlider.value = currentSpeed
            speedSlider.addOnChangeListener { _: Slider, value: Float, fromUser: Boolean ->
                if (fromUser) {
                    val v = value.coerceIn(0.25f, 4f)
                    speedValue.text = "%.2fx".format(v)
                    settings.edit { putFloat(PLAYBACK_SPEED, v) }
                }
            }

            pitchValue.text = "%.2fx".format(currentPitch)
            pitchSlider.value = currentPitch
            pitchSlider.addOnChangeListener { _: Slider, value: Float, fromUser: Boolean ->
                if (fromUser) {
                    val v = value.coerceIn(0.25f, 4f)
                    pitchValue.text = "%.2fx".format(v)
                    settings.edit { putFloat(PLAYBACK_PITCH, v) }
                }
            }

            bassBoostSlider.value = settings.getInt(BASS_BOOST, 0).toFloat()
            bassBoostSlider.addOnChangeListener { _, value, _ ->
                settings.edit { putInt(BASS_BOOST, value.toInt()) }
            }
            equalizer.setOnClickListener { onEqualizerClicked() }
        }

        private fun openEqualizer(activity: ComponentActivity, sessionId: Int) {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, activity.packageName)
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            val contract = ActivityResultContracts.StartActivityForResult()
            activity.registerActivityResultLauncher(contract) {}.launch(intent)
        }

        fun Fragment.onEqualizerClicked() {
            EqualizerBottomSheet().show(childFragmentManager, null)
        }
    }
}
