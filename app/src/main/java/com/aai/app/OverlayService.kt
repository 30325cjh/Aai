package com.aai.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

/**
 * Floating character overlay.
 *
 * The character and chat bar are separate, tightly-sized windows. This is intentional:
 * a full-screen transparent overlay can still consume touches even when nothing is drawn.
 */
class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private var characterView: CharacterView? = null
    private var chatBar: LinearLayout? = null
    private var chatInput: EditText? = null
    private var chatStatus: TextView? = null
    private var characterParams: WindowManager.LayoutParams? = null
    private var chatParams: WindowManager.LayoutParams? = null
    private var chatVisible = false
    private var lastTapAt = 0L
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialWindowX = 0
    private var initialWindowY = 0
    private var gestureMoved = false
    private var currentScale = 1f
    private var baseWidth = 0
    private var baseHeight = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Settings.canDrawOverlays(this)) addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopOverlayAndService()
            return START_NOT_STICKY
        }
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
            elevation = dp(5).toFloat()
            setOnClickListener { toggleCharacter() }
        }
        val params = WindowManager.LayoutParams(
            dp(52), dp(52),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(170)
        }
        windowManager.addView(view, params)
        bubble = view
    }

    private fun toggleCharacter() {
        if (characterView == null) showCharacter() else hideCharacter()
    }

    private fun showCharacter() {
        if (characterView != null || !Settings.canDrawOverlays(this)) return

        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentScale = prefs.getFloat(KEY_SCALE, 1f).coerceIn(0.65f, 1.8f)
        val desiredHeight = (screenH * 0.42f).toInt().coerceIn(dp(250), dp(440))
        baseHeight = desiredHeight
        baseWidth = (desiredHeight * 0.786f).toInt()
        val width = (baseWidth * currentScale).toInt()
        val height = (baseHeight * currentScale).toInt()

        val view = CharacterView(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            contentDescription = "Aai 캐릭터. 드래그하거나 두 손가락으로 크기를 조절하고 두 번 누르면 채팅창이 열립니다."
        }
        characterView = view

        val params = WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = prefs.getInt(KEY_X, screenW - width - dp(8)).coerceAtLeast(0)
            y = prefs.getInt(KEY_Y, (screenH * 0.18f).toInt()).coerceAtLeast(dp(24))
        }
        characterParams = params

        val scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentScale = (currentScale * detector.scaleFactor).coerceIn(0.65f, 1.8f)
                params.width = (baseWidth * currentScale).toInt()
                params.height = (baseHeight * currentScale).toInt()
                keepCharacterOnScreen(params)
                runCatching { windowManager.updateViewLayout(view, params) }
                saveCharacterPlacement(params)
                return true
            }
        })

        view.setOnTouchListener { touched, event ->
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialWindowX = params.x
                    initialWindowY = params.y
                    gestureMoved = false
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> { gestureMoved = true; true }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (kotlin.math.abs(dx) > dp(4) || kotlin.math.abs(dy) > dp(4)) {
                            gestureMoved = true
                            params.x = initialWindowX + dx.toInt()
                            params.y = initialWindowY + dy.toInt()
                            keepCharacterOnScreen(params)
                            runCatching { windowManager.updateViewLayout(touched, params) }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (scaleDetector.isInProgress || event.pointerCount > 1) {
                        saveCharacterPlacement(params)
                    } else if (!gestureMoved) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapAt < 330L) {
                            lastTapAt = 0L
                            toggleChatBar()
                        } else {
                            lastTapAt = now
                        }
                    } else {
                        saveCharacterPlacement(params)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    saveCharacterPlacement(params)
                    true
                }
                else -> true
            }
        }

        windowManager.addView(view, params)
        updateChatPosition()
    }

    private fun hideCharacter() {
        hideChatBar()
        characterView?.let { runCatching { windowManager.removeView(it) } }
        characterView = null
        characterParams = null
    }

    private fun toggleChatBar() {
        if (chatVisible) hideChatBar() else showChatBar()
    }

    private fun showChatBar() {
        if (chatVisible || characterView == null) return
        val input = EditText(this).apply {
            hint = "Aai에게 명령하기  ·  예: 크롬 열어줘"
            textSize = 14f
            setSingleLine(true)
            setTextColor(0xFF202536.toInt())
            setHintTextColor(0xFF7D8494.toInt())
            background = null
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(10), 0, dp(6), 0)
        }
        chatInput = input

        val status = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFF34445C.toInt())
            visibility = View.GONE
            setPadding(dp(12), dp(2), dp(12), dp(4))
        }
        chatStatus = status

        val send = Button(this).apply {
            text = "➤"
            textSize = 18f
            setOnClickListener { sendCommand() }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(3), dp(5), dp(3))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFF7F9FF.toInt())
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), 0xFFCAD7EA.toInt())
            }
            elevation = dp(8).toFloat()
            addView(input, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(send, LinearLayout.LayoutParams(dp(48), dp(48)))
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCommand()
                true
            } else false
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            addView(status, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
            ))
        }
        val screenW = resources.displayMetrics.widthPixels
        val params = WindowManager.LayoutParams(
            min(screenW - dp(20), dp(520)), dp(78),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }
        chatBar = container
        chatParams = params
        windowManager.addView(container, params)
        chatVisible = true
        updateChatPosition()
    }

    private fun hideChatBar() {
        if (!chatVisible && chatBar == null) return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        chatInput?.let { runCatching { imm.hideSoftInputFromWindow(it.windowToken, 0) } }
        chatBar?.let { runCatching { windowManager.removeView(it) } }
        chatBar = null
        chatInput = null
        chatStatus = null
        chatParams = null
        chatVisible = false
    }

    private fun updateChatPosition() {
        val cp = characterParams ?: return
        val bp = chatParams ?: return
        val screenH = resources.displayMetrics.heightPixels
        bp.y = min(cp.y + cp.height + dp(8), screenH - dp(100)).coerceAtLeast(dp(24))
        runCatching { chatBar?.let { windowManager.updateViewLayout(it, bp) } }
    }

    private fun sendCommand() {
        val text = chatInput?.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        chatInput?.setText("")
        chatStatus?.text = "나: $text"
        chatStatus?.visibility = View.VISIBLE
        handleCommand(text)
    }

    private fun handleCommand(raw: String) {
        val text = raw.trim()
        val command = text.removePrefix("/").trim().lowercase()
        val expression = when (command) {
            "웃다", "웃어", "웃기", "웃는 표정", "happy" -> CharacterView.Expression.HAPPY
            "슬프다", "슬퍼", "슬픈 표정", "sad" -> CharacterView.Expression.SAD
            "놀라다", "놀라", "놀란 표정", "surprised" -> CharacterView.Expression.SURPRISED
            "울다", "울어", "우는 표정", "cry" -> CharacterView.Expression.CRYING
            "기본", "원래대로", "normal" -> CharacterView.Expression.NORMAL
            else -> null
        }
        if (text.startsWith("/") && expression != null) {
            characterView?.setExpression(expression)
            showStatus("표정을 변경했어요.")
            return
        }
        if (text.startsWith("/")) {
            showStatus("명령어: /웃다 · /슬프다 · /놀라다 · /울다 · /기본")
            return
        }

        val searchWords = listOf("검색해줘", "검색해", "검색", "찾아줘", "찾아봐", "찾아")
        val searchPrefix = searchWords.firstOrNull { text.startsWith(it) }
        if (searchPrefix != null) {
            val query = text.removePrefix(searchPrefix).trim()
            if (query.isBlank()) {
                showStatus("검색할 내용을 입력해 주세요.")
                return
            }
            characterView?.setExpression(CharacterView.Expression.HAPPY, 2600L)
            characterView?.setTalking(true)
            showStatus("$query 검색 결과를 보여드릴게요!")
            val url = "https://www.google.com/search?q=" + Uri.encode(query)
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (packageManager.getLaunchIntentForPackage("com.android.chrome") != null) {
                browserIntent.setPackage("com.android.chrome")
            }
            runCatching { startActivity(browserIntent) }
                .onFailure { showStatus("브라우저를 열지 못했어요.") }
            characterView?.postDelayed({ characterView?.setTalking(false) }, 2400L)
            return
        }

        val apps = listOf(
            Triple(listOf("크롬", "chrome"), "Chrome", "com.android.chrome"),
            Triple(listOf("유튜브", "youtube"), "YouTube", "com.google.android.youtube"),
            Triple(listOf("설정"), "설정", "com.android.settings"),
            Triple(listOf("계산기"), "계산기", "com.sec.android.app.popupcalculator"),
            Triple(listOf("카메라"), "카메라", "com.sec.android.app.camera"),
            Triple(listOf("지도", "구글맵"), "지도", "com.google.android.apps.maps"),
            Triple(listOf("플레이스토어", "play스토어"), "Play 스토어", "com.android.vending"),
            Triple(listOf("삼성인터넷", "인터넷"), "삼성 인터넷", "com.sec.android.app.sbrowser"),
            Triple(listOf("갤러리"), "갤러리", "com.sec.android.gallery3d"),
            Triple(listOf("내파일", "파일"), "내 파일", "com.sec.android.app.myfiles"),
            Triple(listOf("캘린더", "달력"), "캘린더", "com.samsung.android.calendar"),
            Triple(listOf("노트", "삼성노트"), "삼성 노트", "com.samsung.android.app.notes")
        )
        val target = apps.firstOrNull { item -> item.first.any { text.contains(it, ignoreCase = true) } }
        if (target != null && listOf("열어", "켜", "실행", "열기", "띄워").any { text.contains(it) }) {
            val launch = packageManager.getLaunchIntentForPackage(target.third)
            if (launch != null) {
                characterView?.setExpression(CharacterView.Expression.HAPPY, 2600L)
                characterView?.setTalking(true)
                showStatus("웃으며 ${target.second} 앱을 열어드릴게요!")
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { startActivity(launch) }
                characterView?.postDelayed({ characterView?.setTalking(false) }, 2400L)
            } else {
                showStatus("${target.second} 앱을 찾지 못했어요.")
            }
            return
        }
        showStatus("앱 열기: '크롬 열어줘' · 검색: '검색해 날씨'")
    }

    private fun showStatus(message: String) {
        chatStatus?.text = message
        chatStatus?.visibility = View.VISIBLE
    }

    private fun keepCharacterOnScreen(params: WindowManager.LayoutParams) {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        params.x = params.x.coerceIn(0, max(0, screenW - params.width))
        params.y = params.y.coerceIn(dp(24), max(dp(24), screenH - params.height - dp(24)))
    }

    private fun saveCharacterPlacement(params: WindowManager.LayoutParams) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_X, params.x)
            .putInt(KEY_Y, params.y)
            .putFloat(KEY_SCALE, currentScale)
            .apply()
        updateChatPosition()
    }

    private fun stopOverlayAndService() {
        hideCharacter()
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val launch = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Aai 실행 중")
                .setContentText("알림의 '오버레이 종료'로 언제든 종료할 수 있습니다.")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(launch)
                .addAction(Notification.Action.Builder(null, "오버레이 종료", stop).build())
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
        hideCharacter()
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "aai_overlay"
        private const val NOTIFICATION_ID = 101
        private const val ACTION_STOP = "com.aai.app.STOP_OVERLAY"
        private const val PREFS = "aai_overlay_preferences"
        private const val KEY_X = "character_x"
        private const val KEY_Y = "character_y"
        private const val KEY_SCALE = "character_scale"
    }
}
