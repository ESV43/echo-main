package dev.brahmkshatriya.echo.playback.listener

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import androidx.media3.common.Player

@Suppress("DEPRECATION")
class AudioFocusListener(
    val context: Context,
    val player: Player
) : Player.Listener {
    private val handler = Handler(context.mainLooper)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private lateinit var focusRequest: AudioFocusRequest

    private var preDuckVolume = 1f

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                player.volume = preDuckVolume
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                player.playWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                player.playWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                preDuckVolume = player.volume
                player.volume = 0.2f
            }
        }
    }

    private fun requestFocus(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            audioManager.requestAudioFocus(focusRequest)
        else audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            audioManager.abandonAudioFocusRequest(focusRequest)
        else audioManager.abandonAudioFocus(audioFocusChangeListener)
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener(audioFocusChangeListener, handler)
                build()
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady && player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
            val result = requestFocus()
            if (result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED || result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                player.playWhenReady = false
            }
        } else if (!playWhenReady) {
            abandonFocus()
        }
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
            requestFocus()
        }
    }
}
