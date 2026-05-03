package dev.brahmkshatriya.echo.playback

import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
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
    private var isReleased = false
    private var crossfadeStartedForIndex = C.INDEX_UNSET

    private val crossfadePoll = object : Runnable {
        override fun run() {
            if (isReleased) return
            maybeCrossfade()
            handler.postDelayed(this, 500)
        }
    }

    init {
        mainPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isReleased) return
                if (isPlaying) startCrossfadePolling()
                else stopCrossfadePolling()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (isReleased) return
                if (isAutoTransitioning) {
                    isAutoTransitioning = false
                    return
                }

                if (transitionInProgress) {
                    cancelCrossfade()
                }
                runCatching { mainPlayer.volume = 1f }
                crossfadeStartedForIndex = C.INDEX_UNSET
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                crossfadeStartedForIndex = C.INDEX_UNSET
            }
        })

        secondaryPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (isReleased) return
                cancelCrossfade()
            }
        })
    }

    private fun cancelCrossfade() {
        if (isReleased) return
        transitionInProgress = false
        fadeAnimatorMain?.cancel()
        fadeAnimatorSecondary?.cancel()
        runCatching {
            secondaryPlayer.pause()
            secondaryPlayer.clearMediaItems()
        }
    }

    private fun startCrossfadePolling() {
        if (isReleased) return
        handler.removeCallbacks(crossfadePoll)
        handler.post(crossfadePoll)
    }

    private fun stopCrossfadePolling() {
        handler.removeCallbacks(crossfadePoll)
    }

    private fun maybeCrossfade() {
        if (isReleased) return
        if (!settings.getBoolean(CROSSFADE, false)) return
        
        val isPlaying = runCatching { mainPlayer.isPlaying && mainPlayer.playWhenReady }.getOrDefault(false)
        if (transitionInProgress || !isPlaying) return
        
        val hasNext = runCatching { mainPlayer.hasNextMediaItem() }.getOrDefault(false)
        if (!hasNext) return
        
        val currentIndex = runCatching { mainPlayer.currentMediaItemIndex }.getOrDefault(C.INDEX_UNSET)
        if (currentIndex == crossfadeStartedForIndex || currentIndex == C.INDEX_UNSET) return

        val duration = runCatching { mainPlayer.duration }.getOrDefault(C.TIME_UNSET)
        if (duration == C.TIME_UNSET || duration <= 0) return

        val crossfadeMs = crossfadeDuration()
        val position = runCatching { mainPlayer.currentPosition }.getOrDefault(0L)
        val remaining = duration - position
        
        if (remaining in 1..crossfadeMs) {
            startOverlap(crossfadeMs)
        }
    }

    private fun startOverlap(durationMs: Long) {
        if (isReleased) return
        val currentItem = runCatching { mainPlayer.currentMediaItem }.getOrNull() ?: return
        val currentPosition = runCatching { mainPlayer.currentPosition }.getOrDefault(0L)
        val currentIndex = runCatching { mainPlayer.currentMediaItemIndex }.getOrDefault(C.INDEX_UNSET)
        
        val hasNext = runCatching { mainPlayer.hasNextMediaItem() }.getOrDefault(false)
        if (!hasNext) return

        transitionInProgress = true
        crossfadeStartedForIndex = currentIndex

        runCatching {
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
                if (!isReleased) {
                    secondaryPlayer.pause()
                    secondaryPlayer.clearMediaItems()
                }
            }
        }.onFailure {
            cancelCrossfade()
        }
    }

    private fun fadeVolume(player: Player, from: Float, to: Float, duration: Long, onEnd: (() -> Unit)? = null) {
        if (isReleased) return
        val animator = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener {
                if (!isReleased) {
                    runCatching { player.volume = it.animatedValue as Float }
                }
            }
            if (onEnd != null) {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    private var canceled = false
                    override fun onAnimationCancel(animation: android.animation.Animator) {
                        canceled = true
                    }
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (!canceled && !isReleased) onEnd()
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
        if (isReleased) return
        isReleased = true
        stopCrossfadePolling()
        fadeAnimatorMain?.cancel()
        fadeAnimatorSecondary?.cancel()
        runCatching {
            secondaryPlayer.stop()
            secondaryPlayer.release()
        }
        super.release()
    }
}