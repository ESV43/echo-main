package dev.brahmkshatriya.echo.utils.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import dev.brahmkshatriya.echo.R

class FloatingLyricsService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var lyricsText: TextView? = null
    private var currentLyrics: String = ""
    private var isDragging = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialX = 0
    private var initialY = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        val notification = buildNotification("Lyrics overlay active")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Lyrics",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows lyrics overlay from Eko"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Eko Lyrics")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay(intent.getStringExtra(EXTRA_LYRICS) ?: "")
            ACTION_HIDE -> {
                hideOverlay()
                stopSelf()
            }
            ACTION_UPDATE -> updateLyrics(intent.getStringExtra(EXTRA_LYRICS) ?: "")
            ACTION_INIT -> {
                val notification = buildNotification("Lyrics overlay active")
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    private val overlayParams by lazy {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            flags,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
    }

    private fun showOverlay(lyrics: String) {
        if (overlayView != null) return
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.layout_floating_lyrics, null)
        lyricsText = overlayView?.findViewById(R.id.floatingLyricsText)
        lyricsText?.text = lyrics

        overlayView?.setOnTouchListener { _, event ->
            val params = overlayParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try {
                        windowManager?.updateViewLayout(overlayView, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> !isDragging
                else -> false
            }
        }

        try {
            windowManager?.addView(overlayView, overlayParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideOverlay() {
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
        lyricsText = null
    }

    private fun updateLyrics(lyrics: String) {
        lyricsText?.text = lyrics
        currentLyrics = lyrics
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "show"
        const val ACTION_HIDE = "hide"
        const val ACTION_UPDATE = "update"
        const val ACTION_INIT = "init"
        const val EXTRA_LYRICS = "extra_lyrics"
        private const val CHANNEL_ID = "floating_lyrics"
        private const val NOTIFICATION_ID = 1001

        fun show(context: android.content.Context, lyrics: String) {
            runCatching {
                val intent = Intent(context, FloatingLyricsService::class.java).apply {
                    action = ACTION_SHOW
                    putExtra(EXTRA_LYRICS, lyrics)
                }
                context.startService(intent)
            }
        }

        fun update(context: android.content.Context, lyrics: String) {
            runCatching {
                val intent = Intent(context, FloatingLyricsService::class.java).apply {
                    action = ACTION_UPDATE
                    putExtra(EXTRA_LYRICS, lyrics)
                }
                context.startService(intent)
            }
        }

        fun hide(context: android.content.Context) {
            runCatching {
                val intent = Intent(context, FloatingLyricsService::class.java).apply {
                    action = ACTION_HIDE
                }
                context.startService(intent)
            }
        }

        fun init(context: android.content.Context) {
            runCatching {
                val intent = Intent(context, FloatingLyricsService::class.java).apply {
                    action = ACTION_INIT
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
