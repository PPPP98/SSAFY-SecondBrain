package com.example.secondbrain

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.secondbrain.service.WakeWordService
import com.example.secondbrain.wakeword.WakeWordDetector
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var wakeWordDetector: WakeWordDetector

    private lateinit var tvStatus: TextView
    private lateinit var tvRecognizedText: TextView
    private lateinit var tvMicStatus: TextView
    private lateinit var btnToggleListening: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            tvStatus.text = "마이크 권한이 필요합니다"
            tvStatus.setTextColor(Color.RED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // View 초기화
        tvStatus = findViewById(R.id.tvStatus)
        tvRecognizedText = findViewById(R.id.tvRecognizedText)
        tvMicStatus = findViewById(R.id.tvMicStatus)
        btnToggleListening = findViewById(R.id.btnToggleListening)

        // 웨이크워드 감지기 초기화
        wakeWordDetector = WakeWordDetector(this)

        // 웨이크워드 감지 상태 관찰
        observeWakeWordDetector()

        // 버튼 클릭 리스너
        btnToggleListening.setOnClickListener {
            if (wakeWordDetector.isCurrentlyListening()) {
                stopListening()
            } else {
                checkAndRequestPermission()
            }
        }
    }

    private fun observeWakeWordDetector() {
        // 웨이크워드 감지 상태 관찰
        lifecycleScope.launch {
            wakeWordDetector.wakeWordDetected.collect { detected ->
                if (detected) {
                    tvStatus.text = "헤이스비 감지됨!"
                    tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                } else {
                    tvStatus.text = "웨이크워드 대기 중..."
                    tvStatus.setTextColor(getColor(android.R.color.darker_gray))
                }
            }
        }

        // 인식된 텍스트 관찰
        lifecycleScope.launch {
            wakeWordDetector.recognizedText.collect { text ->
                tvRecognizedText.text = if (text.isNotEmpty()) "인식: $text" else ""
            }
        }
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startListening()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        // 백그라운드 서비스 시작
        val serviceIntent = Intent(this, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // UI용 감지기도 시작
        wakeWordDetector.startListening()
        updateUI(isListening = true)
    }

    private fun stopListening() {
        // 백그라운드 서비스 중지
        val serviceIntent = Intent(this, WakeWordService::class.java)
        stopService(serviceIntent)

        // UI용 감지기도 중지
        wakeWordDetector.stopListening()
        updateUI(isListening = false)
    }

    private fun updateUI(isListening: Boolean) {
        if (isListening) {
            btnToggleListening.text = "중지"
            tvMicStatus.text = "🎤 듣는 중..."
            tvMicStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        } else {
            btnToggleListening.text = "시작"
            tvMicStatus.text = "마이크 꺼짐"
            tvMicStatus.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeWordDetector.stopListening()
    }
}