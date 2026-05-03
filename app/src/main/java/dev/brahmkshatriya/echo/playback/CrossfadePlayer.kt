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

class CrossfadePlayer(
    private val mainPlayer: Player,
    private val secondaryPlayer: Player,
    private val settings: SharedPreferences,
) : ForwardingPlayer(mainPlayer) {

    private var fadeAnimatorMain: ValueAnimator? = null
    private var fadeAnimatorSecondary: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var transitionInProgress = false
    private var isAutoTransitioning = false
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
                if (isAutoTransitioning) {
                    isAutoTransitioning = false
                    return
                }

                if (transitionInProgress) {
                    secondaryPlayer.pause()
                    transitionInProgress = false
                }
                fadeAnimatorMain?.cancel()
                fadeAnimatorSecondary?.cancel()
                mainPlayer.volume = 1f
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
    }

    private fun maybeCrossfade() {
        if (!settings.getBoolean(CROSSFADE, false)) return
        if (transitionInProgress || !mainPlayer.isPlaying || !mainPlayer.playWhenReady) return
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
        val currentItem = mainPlayer.currentMediaItem ?: return
        val currentPosition = mainPlayer.currentPosition
        if (!mainPlayer.hasNextMediaItem()) return

        transitionInProgress = true
        crossfadeStartedForIndex = mainPlayer.currentMediaItemIndex

        // Start old track in secondary player for fade out
        secondaryPlayer.setMediaItem(currentItem)
        secondaryPlayer.prepare()
        secondaryPlayer.seekTo(currentPosition)
        secondaryPlayer.play()
        secondaryPlayer.volume = 1f

        // Move main player to next track for fade in
        isAutoTransitioning = true
        mainPlayer.seekToNext()
        mainPlayer.volume = 0f
        mainPlayer.play()

        fadeVolume(mainPlayer, 0f, 1f, durationMs) {
            transitionInProgress = false
        }
        fadeVolume(secondaryPlayer, 1f, 0f, durationMs) {
            secondaryPlayer.pause()
        }
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
                    private var canceled = false
                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        canceled = true
                    }
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (!canceled) onEnd()
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