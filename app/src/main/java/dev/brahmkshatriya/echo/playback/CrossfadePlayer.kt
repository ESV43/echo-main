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

class CrossfadePlayer(
    private val mainPlayer: Player,
    private val secondaryPlayer: Player,
    private val settings: SharedPreferences,
) : ForwardingPlayer(mainPlayer) {

    private var fadeAnimatorMain: ValueAnimator? = null
    private var fadeAnimatorSecondary: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var transitionInProgress = false
    private var crossfadeStartedForIndex = C.INDEX_UNSET

    private val crossfadePoll = object : Runnable {
        override fun run() {
            maybeCrossfade()
            handler.postDelayed(this, 500)
        }
    }

    init {
        mainPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startCrossfadePolling()
                else stopCrossfadePolling()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!transitionInProgress) {
                    fadeAnimatorMain?.cancel()
                    mainPlayer.volume = 1f
                }
                crossfadeStartedForIndex = C.INDEX_UNSET
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                crossfadeStartedForIndex = C.INDEX_UNSET
            }
        })
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
        if (transitionInProgress || !mainPlayer.isPlaying) return
        if (!mainPlayer.hasNextMediaItem()) return
        if (mainPlayer.currentMediaItemIndex == crossfadeStartedForIndex) return

        val duration = mainPlayer.duration
        if (duration == C.TIME_UNSET || duration <= 0) return

        val crossfadeMs = crossfadeDuration()
        val remaining = duration - mainPlayer.currentPosition
        
        if (remaining in 1..crossfadeMs) {
            startOverlap(crossfadeMs)
        }
    }

    private fun startOverlap(durationMs: Long) {
        transitionInProgress = true
        crossfadeStartedForIndex = mainPlayer.currentMediaItemIndex

        val nextMediaItem = mainPlayer.getMediaItemAt(mainPlayer.nextMediaItemIndex)
        secondaryPlayer.setMediaItem(nextMediaItem)
        secondaryPlayer.prepare()
        secondaryPlayer.volume = 0f
        secondaryPlayer.play()

        fadeVolume(mainPlayer, 1f, 0f, durationMs) {
            mainPlayer.pause()
            mainPlayer.seekToNextMediaItem()
            mainPlayer.volume = 1f
            transitionInProgress = false
        }
        fadeVolume(secondaryPlayer, 0f, 1f, durationMs)
    }

    private fun fadeVolume(player: Player, from: Float, to: Float, duration: Long, onEnd: (() -> Unit)? = null) {
        val animator = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener {
                player.volume = it.animatedValue as Float
            }
            if (onEnd != null) {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onEnd()
                    }
                })
            }
        }
        if (player === mainPlayer) {
            fadeAnimatorMain?.cancel()
            fadeAnimatorMain = animator
        } else {
            fadeAnimatorSecondary?.cancel()
            fadeAnimatorSecondary = animator
        }
        animator.start()
    }

    private fun crossfadeDuration() =
        settings.getInt(CROSSFADE_DURATION, 5).coerceIn(1, 15) * 1000L

    override fun release() {
        stopCrossfadePolling()
        fadeAnimatorMain?.cancel()
        fadeAnimatorSecondary?.cancel()
        secondaryPlayer.release()
        super.release()
    }
}
