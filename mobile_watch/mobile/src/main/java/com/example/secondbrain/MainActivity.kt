package com.example.secondbrain

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.secondbrain.data.local.TokenManager
import com.example.secondbrain.service.WakeWordService
import com.example.secondbrain.ui.login.LoginActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnExit: Button
    private lateinit var btnTestNote: Button
    private lateinit var tvTestResult: TextView
    private lateinit var tokenManager: TokenManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startWakeWordService()
        } else {
            tvStatus.text = "마이크 권한 필요"
            tvStatus.setTextColor(Color.RED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TokenManager 초기화
        tokenManager = TokenManager(this)

        // 로그인 상태 확인
        lifecycleScope.launch {
            val isLoggedIn = tokenManager.isLoggedIn.first()
            if (!isLoggedIn) {
                // 로그인 안 되어있으면 LoginActivity로 이동
                navigateToLogin()
                return@launch
            }

            // 로그인 되어있으면 메인 화면 표시
            initializeMainScreen()
        }
    }

    // 로그인 화면으로 이동
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // 메인 화면 초기화
    private fun initializeMainScreen() {
        setContentView(R.layout.activity_main)

        // View 초기화
        tvStatus = findViewById(R.id.tvStatus)
        btnLogout = findViewById(R.id.btnLogout)
        btnExit = findViewById(R.id.btnExit)
        btnTestNote = findViewById(R.id.btnTestNote)
        tvTestResult = findViewById(R.id.tvTestResult)

        // 웨이크워드로 앱이 실행된 경우
        if (intent.getBooleanExtra("wake_word_detected", false)) {
            tvStatus.text = "헤이스비 감지!"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))

            // 3초 후 자동으로 뒤로가기 (백그라운드로 전환)
            Handler(Looper.getMainLooper()).postDelayed({
                moveTaskToBack(true)
                tvStatus.text = "대기 중..."
                tvStatus.setTextColor(Color.parseColor("#666666"))
            }, 3000)
        } else {
            // 일반 실행 시 권한 확인 및 서비스 시작
            checkAndRequestPermission()
        }

        // 로그아웃 버튼
        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                tokenManager.clearTokens()
                navigateToLogin()
            }
        }

        // 앱 종료 버튼
        btnExit.setOnClickListener {
            stopWakeWordService()
            finishAffinity() // 모든 액티비티 종료
            exitProcess(0) // 프로세스 완전 종료
        }

        // 노트 조회 테스트 버튼
        btnTestNote.setOnClickListener {
            testNoteApi()
        }
    }

    // 노트 API 테스트
    private fun testNoteApi() {
        android.util.Log.e("MainActivity", "🔥🔥🔥 testNoteApi 호출됨! 🔥🔥🔥")
        lifecycleScope.launch {
            try {
                android.util.Log.e("MainActivity", "🔥 코루틴 시작!")
                tvTestResult.text = "로딩 중..."

                // 토큰 확인
                val token = tokenManager.getAccessToken()
                android.util.Log.d("MainActivity", "저장된 토큰: ${token?.take(20)}...")

                if (token.isNullOrEmpty()) {
                    tvTestResult.text = "❌ 토큰이 없습니다. 다시 로그인해주세요."
                    return@launch
                }

                // API 서비스 생성
                val apiService = com.example.secondbrain.data.network.RetrofitClient.createApiService {
                    tokenManager.getAccessToken()
                }

                // 노트 상세 조회 (ID: 55)
                val response = apiService.getNote(55)

                if (response.code == 200 && response.data != null) {
                    val note = response.data
                    tvTestResult.text = """
                        ✅ 성공!

                        제목: ${note.title}
                        내용: ${note.content?.take(100)}...
                        생성일: ${note.createdAt}
                    """.trimIndent()
                } else {
                    tvTestResult.text = "❌ 실패: ${response.message}"
                }
            } catch (e: Exception) {
                tvTestResult.text = "❌ 에러: ${e.message}"
                android.util.Log.e("MainActivity", "Note API test failed", e)
            }
        }
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startWakeWordService()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startWakeWordService() {
        val serviceIntent = Intent(this, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        tvStatus.text = "대기 중..."
        tvStatus.setTextColor(Color.parseColor("#666666"))
    }

    private fun stopWakeWordService() {
        val serviceIntent = Intent(this, WakeWordService::class.java)
        stopService(serviceIntent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // 웨이크워드로 다시 실행된 경우
        if (intent.getBooleanExtra("wake_word_detected", false)) {
            tvStatus.text = "헤이스비 감지!"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))

            Handler(Looper.getMainLooper()).postDelayed({
                moveTaskToBack(true)
                tvStatus.text = "대기 중..."
                tvStatus.setTextColor(Color.parseColor("#666666"))
            }, 3000)
        }
    }
}