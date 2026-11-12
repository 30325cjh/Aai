package com.aai.app

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class ChatActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val title = TextView(this).apply {
            text = "Aai 대화"
            textSize = 22f
        }
        val message = TextView(this).apply {
            text = "안녕하세요! 캐릭터와 AI 연결은 다음 단계에서 추가할게요."
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }
        val input = EditText(this).apply {
            hint = "메시지를 입력하세요"
            isSingleLine = true
        }
        root.addView(title)
        root.addView(message, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setContentView(root)
    }
}
