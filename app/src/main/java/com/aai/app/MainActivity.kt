package com.aai.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshPermissionState()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        status = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
        }
        val explain = TextView(this).apply {
            text = "Aai가 다른 앱 위에 작은 실행 아이콘을 표시하려면 권한이 필요합니다. 권한을 허용하면 이 화면으로 돌아와 플로팅 아이콘이 시작됩니다."
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        val permissionButton = Button(this).apply {
            text = "다른 앱 위에 표시 권한 설정"
            setOnClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        root.addView(status, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(explain, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(permissionButton)
        setContentView(root)
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        if (Settings.canDrawOverlays(this)) {
            status.text = "권한 허용됨 — Aai 플로팅 아이콘을 시작합니다."
            val intent = Intent(this, OverlayService::class.java)
            startForegroundService(intent)
        } else {
            status.text = "아직 권한이 필요합니다."
        }
    }
}
