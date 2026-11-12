package com.aai.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private var characterPanel: TextView? = null
    private var panelVisible = false
    private var downX = 0
    private var downY = 0
    private var startX = 0
    private var startY = 0
    private var lastTapAt = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Settings.canDrawOverlays(this)) addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (bubble == null && Settings.canDrawOverlays(this)) addBubble()
        return START_STICKY
    }

    private fun addBubble() {
        if (bubble != null) return
        val view = TextView(this).apply {
            text = "A"
            textSize = 21f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundResource(android.R.drawable.btn_default)
            elevation = 12f
            setOnTouchListener { v, event ->
                val params = v.layoutParams as WindowManager.LayoutParams
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX.toInt()
                        downY = event.rawY.toInt()
                        startX = params.x
                        startY = params.y
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX + (downX - event.rawX.toInt())
                        params.y = startY + (event.rawY.toInt() - downY)
                        windowManager.updateViewLayout(v, params)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val moved = kotlin.math.abs(event.rawX.toInt() - downX) > 12 ||
                            kotlin.math.abs(event.rawY.toInt() - downY) > 12
                        if (!moved) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapAt < 320) {
                                openChat()
                                lastTapAt = 0L
                            } else {
                                lastTapAt = now
                                v.postDelayed({
                                    if (lastTapAt == now) toggleCharacterPanel()
                                }, 330)
                            }
                        }
                        true
                    }
                    else -> true
                }
            }
        }
        val params = WindowManager.LayoutParams(
            dp(56), dp(56),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(180)
        }
        windowManager.addView(view, params)
        bubble = view
    }

    private fun toggleCharacterPanel() {
        if (panelVisible) {
            characterPanel?.let { windowManager.removeView(it) }
            characterPanel = null
            panelVisible = false
            return
        }
        val panel = TextView(this).apply {
            text = "Aai\n캐릭터 모델을 기다리는 중이에요."
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(0xDD29243D.toInt())
            setOnClickListener { openChat() }
        }
        val params = WindowManager.LayoutParams(
            dp(240), dp(300),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        windowManager.addView(panel, params)
        characterPanel = panel
        panelVisible = true
    }

    private fun openChat() {
        val intent = Intent(this, ChatActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun buildNotification(): Notification {
        val launch = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Aai 실행 중")
                .setContentText("플로팅 캐릭터 아이콘을 표시하고 있습니다.")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(launch)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Aai 실행 중")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setOngoing(true)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Aai 플로팅 캐릭터", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        characterPanel?.let { windowManager.removeView(it) }
        bubble?.let { windowManager.removeView(it) }
        characterPanel = null
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "aai_overlay"
        private const val NOTIFICATION_ID = 101
    }
}
