package dev.brahmkshatriya.echo.playback

import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.CROSSFADE
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.CROSSFADE_DURATION
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.FADE_CONTROLS
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.FADE_DURATION

class FadePlayer(
    private val player: Player,
    private val settings: SharedPreferences,
) : ForwardingPlayer(player) {

    private var fadeAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var transitionInProgress = false
    private var lastMediaItemIndex = C.INDEX_UNSET
    private var crossfadeStartedForIndex = C.INDEX_UNSET

    private val crossfadePoll = object : Runnable {
        override fun run() {
            maybeCrossfade()
            handler.postDelayed(this, 250)
        }
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startCrossfadePolling()
                else stopCrossfadePolling()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                fadeAnimator?.cancel()
                transitionInProgress = false
                lastMediaItemIndex = player.currentMediaItemIndex
                crossfadeStartedForIndex = C.INDEX_UNSET
                if (settings.getBoolean(CROSSFADE, false) && player.playWhenReady) {
                    fadeVolume(player.volume.coerceIn(0f, 1f), 1f, fadeDuration())
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                crossfadeStartedForIndex = C.INDEX_UNSET
                transitionInProgress = false
            }
        })
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady == player.playWhenReady) return

        fadeAnimator?.cancel()
        if (!settings.getBoolean(FADE_CONTROLS, true)) {
            player.volume = 1f
            super.setPlayWhenReady(playWhenReady)
            return
        }
        if (playWhenReady) {
            player.volume = 0f
            super.setPlayWhenReady(true)
            fadeVolume(0f, 1f, fadeDuration())
        } else {
            fadeVolume(player.volume, 0f, fadeDuration()) {
                super.setPlayWhenReady(false)
                player.volume = 1f
            }
        }
    }

    override fun play() {
        setPlayWhenReady(true)
    }

    override fun pause() {
        setPlayWhenReady(false)
    }

    private fun startCrossfadePolling() {
        handler.removeCallbacks(crossfadePoll)
        handler.post(crossfadePoll)
    }

    private fun stopCrossfadePolling() {
        handler.removeCallbacks(crossfadePoll)
        transitionInProgress = false
    }

    private fun maybeCrossfade() {
        if (!settings.getBoolean(CROSSFADE, false)) return
        if (transitionInProgress || !player.isPlaying) return
        if (!player.hasNextMediaItem()) return
        if (player.currentMediaItemIndex == crossfadeStartedForIndex) return

        val duration = player.duration
        if (duration == C.TIME_UNSET || duration <= 0) return

        val durationMs = crossfadeDuration()
        if (duration <= durationMs + 1000) return

        val remaining = duration - player.currentPosition
        if (remaining !in 1..durationMs) return

        transitionInProgress = true
        crossfadeStartedForIndex = player.currentMediaItemIndex
        lastMediaItemIndex = player.currentMediaItemIndex
        fadeVolume(player.volume.coerceIn(0f, 1f), 0f, fadeOutBeforeSwitchDuration(remaining)) {
            if (player.currentMediaItemIndex == lastMediaItemIndex && player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            }
            fadeVolume(0f, 1f, fadeDuration())
        }
    }

    private fun fadeDuration() = settings.getInt(FADE_DURATION, 300).coerceIn(0, 5000).toLong()

    private fun crossfadeDuration() =
        settings.getInt(CROSSFADE_DURATION, 5).coerceIn(1, 60) * 1000L

    private fun fadeOutBeforeSwitchDuration(remaining: Long): Long {
        val fade = fadeDuration()
        return when {
            fade <= 0 -> 0
            remaining <= fade -> remaining
            else -> fade
        }
    }

    private fun fadeVolume(from: Float, to: Float, duration: Long, onEnd: (() -> Unit)? = null) {
        fadeAnimator?.cancel()
        if (duration <= 0) {
            player.volume = to
            onEnd?.invoke()
            return
        }
        fadeAnimator = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener {
                player.volume = it.animatedValue as Float
            }
            if (onEnd != null) {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    private var canceled = false

                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        canceled = true
                    }

                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (!canceled) onEnd()
                    }
                })
            }
            start()
        }
    }

    override fun stop() {
        stopCrossfadePolling()
        fadeAnimator?.cancel()
        player.volume = 1f
        super.stop()
    }

    override fun release() {
        stopCrossfadePolling()
        fadeAnimator?.cancel()
        super.release()
    }
}
