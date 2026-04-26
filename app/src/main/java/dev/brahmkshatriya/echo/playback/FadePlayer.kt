package dev.brahmkshatriya.echo.playback

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

class FadePlayer(private val player: Player) : ForwardingPlayer(player) {

    private var fadeAnimator: ValueAnimator? = null

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady == player.playWhenReady) return

        fadeAnimator?.cancel()
        if (playWhenReady) {
            player.volume = 0f
            super.setPlayWhenReady(true)
            fadeVolume(0f, 1f, 300)
        } else {
            fadeVolume(player.volume, 0f, 300) {
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

    private fun fadeVolume(from: Float, to: Float, duration: Long, onEnd: (() -> Unit)? = null) {
        fadeAnimator = ValueAnimator.ofFloat(from, to).apply {
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
        super.release()
    }
}
