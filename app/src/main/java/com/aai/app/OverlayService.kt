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
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private var characterPanel: LinearLayout? = null
    private var chatBar: LinearLayout? = null
    private var chatHistory: TextView? = null
    private var messageInput: EditText? = null
    private var panelVisible = false
    private var chatVisible = false
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
                                if (!panelVisible) showCharacterPanel()
                                toggleChatBar()
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
            removeCharacterPanel()
        } else {
            showCharacterPanel()
        }
    }

    private fun showCharacterPanel() {
        if (panelVisible) return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xE6222635.toInt())
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), 0x665ACDFF)
            }
        }

        // Character stage: sized to roughly 45% of the screen height.
        val characterStage = TextView(this).apply {
            text = "Aai\n\n캐릭터 일러스트 준비 중"
            textSize = 22f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x332D8DCA)
        }
        root.addView(characterStage, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val history = TextView(this).apply {
            text = "Aai와 나눈 대화가 여기에 표시돼요."
            textSize = 13f
            setTextColor(0xFFEAF4FF.toInt())
            setPadding(dp(8), dp(6), dp(8), dp(4))
            visibility = View.GONE
        }
        chatHistory = history
        root.addView(history, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ))

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(6), dp(4))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFF4F6FC.toInt())
                cornerRadius = dp(30).toFloat()
            }
            visibility = View.GONE
        }
        chatBar = bar

        val input = EditText(this).apply {
            hint = "Aai에게 무엇이든 물어보세요"
            textSize = 14f
            setSingleLine(true)
            setTextColor(0xFF202536.toInt())
            setHintTextColor(0xFF7D8494.toInt())
            background = null
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
        }
        messageInput = input
        bar.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f))

        val send = Button(this).apply {
            text = "➤"
            textSize = 18f
            setOnClickListener { sendMessage() }
        }
        bar.addView(send, LinearLayout.LayoutParams(dp(48), dp(48)))
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
        root.addView(bar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
        ))

        val metrics = resources.displayMetrics
        val panelWidth = (metrics.widthPixels * 0.94f).toInt()
        val panelHeight = (metrics.heightPixels * 0.62f).toInt()
        val params = WindowManager.LayoutParams(
            panelWidth, panelHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        windowManager.addView(root, params)
        characterPanel = root
        panelVisible = true
    }

    private fun toggleChatBar() {
        val bar = chatBar ?: return
        chatVisible = !chatVisible
        bar.visibility = if (chatVisible) View.VISIBLE else View.GONE
        chatHistory?.visibility = if (chatVisible) View.VISIBLE else View.GONE
        if (chatVisible) {
            characterPanel?.let {
                val p = it.layoutParams as WindowManager.LayoutParams
                p.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                windowManager.updateViewLayout(it, p)
            }
            messageInput?.requestFocus()
            messageInput?.postDelayed({
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                messageInput?.let { imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
            }, 180)
        } else {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            messageInput?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
            characterPanel?.let {
                val p = it.layoutParams as WindowManager.LayoutParams
                p.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                windowManager.updateViewLayout(it, p)
            }
        }
    }

    private fun sendMessage() {
        val text = messageInput?.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        val current = chatHistory?.text?.toString().orEmpty()
        chatHistory?.text = "$current\n나: $text\nAai: 아직 AI 연결 전이에요."
        messageInput?.setText("")
    }

    private fun removeCharacterPanel() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        messageInput?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        characterPanel?.let { windowManager.removeView(it) }
        characterPanel = null
        chatBar = null
        chatHistory = null
        messageInput = null
        panelVisible = false
        chatVisible = false
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
        removeCharacterPanel()
        bubble?.let { windowManager.removeView(it) }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "aai_overlay"
        private const val NOTIFICATION_ID = 101
    }
}
