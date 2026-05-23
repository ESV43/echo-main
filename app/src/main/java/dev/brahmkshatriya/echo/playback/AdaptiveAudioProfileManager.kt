package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import dev.brahmkshatriya.echo.ui.settings.Keys
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AdaptiveAudioProfileManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val settings = context.getSettings()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    var onProfileChanged: ((String) -> Unit)? = null

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            updateProfile()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            updateProfile()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
    }

    fun updateProfile() {
        if (!settings.getBoolean(Keys.ADAPTIVE_AUDIO, true)) return

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val activeDevice = devices.firstOrNull { it.isSink } ?: return
        
        val deviceType = when (activeDevice.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "bluetooth"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "headphones"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "speaker"
            else -> "default"
        }

        val profileKey = "v4_audio_profile_$deviceType"
        val gains = settings.getString(profileKey, null) ?: defaultGains(deviceType)
        onProfileChanged?.invoke(gains)
    }

    private fun defaultGains(deviceType: String) = when (deviceType) {
        "speaker" -> "2.5,2,1.5,0.8,0,0.4,1.2,1.8,2.2,2"
        "headphones" -> "1.2,0.8,0.3,0,0,0.4,0.8,1,0.7,0.4"
        "bluetooth" -> "1.8,1.4,0.8,0.2,0,0.3,0.9,1.4,1.6,1.2"
        else -> "0,0,0,0,0,0,0,0,0,0"
    }

    fun release() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }
}
