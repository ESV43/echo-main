package dev.brahmkshatriya.echo.playback

import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.view.animation.LinearInterpolator
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.FADE_CONTROLS
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.FADE_DURATION

class FadePlayer(
    private val player: Player,
    private val settings: SharedPreferences,
) : ForwardingPlayer(player) {

    private var fadeAnimator: ValueAnimator? = null

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
            super.setPlayWhenReady(false)
            fadeVolume(player.volume, 0f, fadeDuration()) {
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

    private fun fadeDuration() = settings.getInt(FADE_DURATION, 300).coerceIn(0, 5000).toLong()

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
        fadeAnimator?.cancel()
        player.volume = 1f
        super.stop()
    }

    override fun release() {
        fadeAnimator?.cancel()
        player.volume = 1f
        super.release()
    }
}
