package com.example.secondbrain

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.secondbrain.data.local.TokenManager
import com.example.secondbrain.service.WakeWordService
import com.example.secondbrain.ui.login.LoginActivity
import com.example.secondbrain.ui.note.NoteDetailActivity
import com.example.secondbrain.ui.search.SearchResultAdapter
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnExit: Button
    private lateinit var etNoteId: EditText
    private lateinit var btnSearchNote: Button
    private lateinit var tvTestResult: TextView
    private lateinit var etSearchKeyword: EditText
    private lateinit var btnSearch: Button
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var searchAdapter: SearchResultAdapter
    private lateinit var btnCheckWearConnection: Button
    private lateinit var tvWearStatus: TextView
    private lateinit var btnOpenSettings: Button
    private lateinit var btnGoToVoiceSearch: Button
    private lateinit var tokenManager: TokenManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkFullScreenIntentPermission()
        } else {
            tvStatus.text = "마이크 권한 필요"
            tvStatus.setTextColor(Color.RED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Application 클래스 확인
        android.util.Log.e("MainActivity", "Application 클래스: ${application.javaClass.name}")
        if (application is SecondBrainApplication) {
            android.util.Log.i("MainActivity", "✓ SecondBrainApplication 사용 중")
        } else {
            android.util.Log.e("MainActivity", "❌ SecondBrainApplication이 아님! 기본 Application 사용 중")
        }

        // Full-Screen Intent로 열릴 때 화면 켜기 및 잠금 해제
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

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

            // 로그인 되어있으면 SearchActivity로 바로 이동
            navigateToSearch()
        }
    }

    // 로그인 화면으로 이동
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // 검색 화면으로 이동
    private fun navigateToSearch() {
        val intent = Intent(this, com.example.secondbrain.ui.search.SearchActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // 메인 화면 초기화
    private fun initializeMainScreen() {
        setContentView(R.layout.activity_main)

        // WearableListenerService 활성화 (중요!)
        enableWearableListenerService()

        // View 초기화
        tvStatus = findViewById(R.id.tvStatus)
        btnLogout = findViewById(R.id.btnLogout)
        btnExit = findViewById(R.id.btnExit)
        etNoteId = findViewById(R.id.etNoteId)
        btnSearchNote = findViewById(R.id.btnSearchNote)
        tvTestResult = findViewById(R.id.tvTestResult)
        etSearchKeyword = findViewById(R.id.etSearchKeyword)
        btnSearch = findViewById(R.id.btnSearch)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        btnCheckWearConnection = findViewById(R.id.btnCheckWearConnection)
        tvWearStatus = findViewById(R.id.tvWearStatus)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)
        btnGoToVoiceSearch = findViewById(R.id.btnGoToVoiceSearch)

        // RecyclerView 설정
        setupRecyclerView()

        // 웨이크워드로 앱이 실행된 경우 표시
        if (intent.getBooleanExtra("wake_word_detected", false)) {
            intent.removeExtra("wake_word_detected")
            intent.removeExtra("auto_opened")
            tvStatus.text = "헤이스비 감지!"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        }

        // 일반 실행 시 권한 확인 및 서비스 시작
        checkAndRequestPermission()

        // 음성 검색 페이지 이동 버튼
        btnGoToVoiceSearch.setOnClickListener {
            val intent = Intent(this, com.example.secondbrain.ui.search.SearchActivity::class.java)
            startActivity(intent)
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

        // 노트 조회 버튼
        btnSearchNote.setOnClickListener {
            searchNoteById()
        }

        // 검색 버튼
        btnSearch.setOnClickListener {
            performSearch()
        }

        // 검색어 입력 시 엔터 키로 검색
        etSearchKeyword.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performSearch()
                true
            } else {
                false
            }
        }

        // 워치 연결 확인 버튼
        btnCheckWearConnection.setOnClickListener {
            checkWearableConnection()
        }

        // 앱 설정 열기 버튼
        btnOpenSettings.setOnClickListener {
            openAppSettings()
        }
    }

    // RecyclerView 설정
    private fun setupRecyclerView() {
        searchAdapter = SearchResultAdapter { noteId ->
            // 검색 결과 클릭 시 상세 페이지로 이동
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra("NOTE_ID", noteId)
            startActivity(intent)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = searchAdapter
    }

    // 노트 검색 수행
    private fun performSearch() {
        val keyword = etSearchKeyword.text.toString()

        if (keyword.isEmpty()) {
            rvSearchResults.visibility = View.GONE
            return
        }

        // 키보드 숨김
        hideKeyboard()

        lifecycleScope.launch {
            try {
                // 토큰 확인
                val token = tokenManager.getAccessToken()
                if (token.isNullOrEmpty()) {
                    android.util.Log.w("MainActivity", "검색 실패: 액세스 토큰 없음")
                    searchAdapter.updateResults(emptyList())
                    rvSearchResults.visibility = View.GONE
                    return@launch
                }

                // API 서비스 생성
                val apiService = com.example.secondbrain.data.network.RetrofitClient.createApiService {
                    tokenManager.getAccessToken()
                }

                // 검색 실행
                val response = apiService.searchNotes(keyword)

                when (response.code) {
                    200 -> {
                        if (response.data != null) {
                            val searchResponse = response.data
                            searchAdapter.updateResults(searchResponse.results, keyword)
                            if (searchResponse.results.isNotEmpty()) {
                                rvSearchResults.visibility = View.VISIBLE
                            } else {
                                rvSearchResults.visibility = View.GONE
                            }
                        } else {
                            android.util.Log.w("MainActivity", "검색 결과가 없습니다")
                            searchAdapter.updateResults(emptyList(), keyword)
                            rvSearchResults.visibility = View.GONE
                        }
                    }
                    401 -> {
                        // 인증 오류 (토큰 만료 등)
                        android.util.Log.e("MainActivity", "검색 실패: 인증 오류 (401)")
                        searchAdapter.updateResults(emptyList(), keyword)
                        rvSearchResults.visibility = View.GONE
                    }
                    in 500..599 -> {
                        // 서버 오류
                        android.util.Log.e("MainActivity", "검색 실패: 서버 오류 (${response.code})")
                        searchAdapter.updateResults(emptyList(), keyword)
                        rvSearchResults.visibility = View.GONE
                    }
                    else -> {
                        // 기타 오류
                        android.util.Log.e("MainActivity", "검색 실패: 예상치 못한 응답 코드 (${response.code})")
                        searchAdapter.updateResults(emptyList(), keyword)
                        rvSearchResults.visibility = View.GONE
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                // 네트워크 연결 오류
                android.util.Log.e("MainActivity", "검색 실패: 네트워크 연결 오류", e)
                searchAdapter.updateResults(emptyList(), keyword)
                rvSearchResults.visibility = View.GONE
            } catch (e: java.net.SocketTimeoutException) {
                // 네트워크 타임아웃
                android.util.Log.e("MainActivity", "검색 실패: 네트워크 타임아웃", e)
                searchAdapter.updateResults(emptyList(), keyword)
                rvSearchResults.visibility = View.GONE
            } catch (e: java.io.IOException) {
                // 기타 네트워크 오류
                android.util.Log.e("MainActivity", "검색 실패: 네트워크 I/O 오류", e)
                searchAdapter.updateResults(emptyList(), keyword)
                rvSearchResults.visibility = View.GONE
            } catch (e: Exception) {
                // 예상치 못한 오류
                android.util.Log.e("MainActivity", "검색 실패: 예상치 못한 오류", e)
                searchAdapter.updateResults(emptyList(), keyword)
                rvSearchResults.visibility = View.GONE
            }
        }
    }

    // 키보드 숨김
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // 앱 설정 화면 열기
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "설정 화면 열기 실패", e)
            tvStatus.text = "설정 열기 실패"
            tvStatus.setTextColor(Color.RED)
        }
    }

    // 노트 ID로 조회
    private fun searchNoteById() {
        val noteIdText = etNoteId.text.toString()

        if (noteIdText.isEmpty()) {
            tvTestResult.text = "노트 ID를 입력해주세요."
            return
        }

        val noteId = noteIdText.toLongOrNull()
        if (noteId == null) {
            tvTestResult.text = "올바른 노트 ID를 입력해주세요."
            return
        }

        lifecycleScope.launch {
            try {
                tvTestResult.text = "노트 조회 중..."

                // 토큰 확인
                val token = tokenManager.getAccessToken()
                if (token.isNullOrEmpty()) {
                    tvTestResult.text = "❌ 토큰이 없습니다. 다시 로그인해주세요."
                    return@launch
                }

                // API 서비스 생성
                val apiService = com.example.secondbrain.data.network.RetrofitClient.createApiService {
                    tokenManager.getAccessToken()
                }

                // 노트 상세 조회
                val response = apiService.getNote(noteId)

                if (response.code == 200 && response.data != null) {
                    val note = response.data
                    tvTestResult.text = "✅ ${note.title}\n\n탭하여 상세보기"

                    // 결과 텍스트를 클릭하면 상세 페이지로 이동
                    tvTestResult.setOnClickListener {
                        val intent = Intent(this@MainActivity, NoteDetailActivity::class.java)
                        intent.putExtra("NOTE_ID", noteId)
                        startActivity(intent)
                    }
                } else {
                    tvTestResult.text = "❌ 실패: ${response.message}"
                    tvTestResult.setOnClickListener(null)
                }
            } catch (e: Exception) {
                tvTestResult.text = "❌ 에러: ${e.message}"
                tvTestResult.setOnClickListener(null)
                android.util.Log.e("MainActivity", "Note search failed", e)
            }
        }
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 마이크 권한 있음 - Full-Screen Intent 권한 확인
                checkFullScreenIntentPermission()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.canUseFullScreenIntent()) {
                android.util.Log.w("MainActivity", "Full-Screen Intent 권한 없음 - 설정으로 안내")

                // UI에 안내 메시지 표시
                tvStatus.text = "⚠️ 전체 화면 알림 권한 필요\n설정 화면으로 이동합니다..."
                tvStatus.setTextColor(Color.parseColor("#FF9800"))

                // 사용자를 설정 화면으로 안내
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }

                    // 안내 다이얼로그 표시 (선택적)
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("권한 필요")
                        .setMessage("웨이크워드 감지 시 자동으로 앱을 열기 위해서는 '전체 화면 알림' 권한이 필요합니다.\n\n설정 화면에서 허용해주세요.")
                        .setPositiveButton("설정으로 이동") { _, _ ->
                            startActivity(intent)
                        }
                        .setNegativeButton("나중에") { dialog, _ ->
                            dialog.dismiss()
                            startWakeWordService()
                        }
                        .show()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "설정 화면 열기 실패", e)
                    // 설정 화면을 열 수 없으면 일반 설정으로 이동
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                    startWakeWordService()
                }
            } else {
                android.util.Log.i("MainActivity", "Full-Screen Intent 권한 있음")
                startWakeWordService()
            }
        } else {
            // Android 13 이하는 권한 불필요
            startWakeWordService()
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

    /**
     * WearableListenerService 활성화
     *
     * Google Play Services가 WearableListenerService를 자동으로 바인딩하도록 합니다.
     */
    private fun enableWearableListenerService() {
        try {
            android.util.Log.i("MainActivity", "WearableListenerService 활성화 시작")

            // PackageManager를 통해 서비스 컴포넌트 강제 활성화
            try {
                val componentName = android.content.ComponentName(
                    this,
                    com.example.secondbrain.service.MobileWearableListenerService::class.java
                )
                packageManager.setComponentEnabledSetting(
                    componentName,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                android.util.Log.i("MainActivity", "서비스 컴포넌트 활성화 완료")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "서비스 컴포넌트 활성화 실패", e)
            }

            // Activity 생명주기와 무관하게 실행되도록 독립적인 CoroutineScope 사용
            CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                try {
                    // Google Play Services Wearable API 초기화
                    val dataClient = Wearable.getDataClient(this@MainActivity)
                    val nodeClient = Wearable.getNodeClient(this@MainActivity)

                    android.util.Log.i("MainActivity", "Wearable API 클라이언트 초기화 완료")

                    // DataClient 리스너 등록 (이것이 서비스 바인딩 트리거)
                    val testDataReq = com.google.android.gms.wearable.PutDataMapRequest.create("/test_trigger").apply {
                        dataMap.putLong("timestamp", System.currentTimeMillis())
                    }
                    dataClient.putDataItem(testDataReq.asPutDataRequest()).await()
                    android.util.Log.i("MainActivity", "테스트 DataItem 전송 - 서비스 바인딩 트리거")

                    // 연결된 노드 확인
                    val nodes = nodeClient.connectedNodes.await()
                    android.util.Log.i("MainActivity", "연결된 노드: ${nodes.size}개")

                    kotlinx.coroutines.delay(500) // 서비스 바인딩 대기
                    android.util.Log.i("MainActivity", "WearableListenerService 바인딩 트리거 완료")
                    android.util.Log.i("MainActivity", "만약 'MobileWearableListenerService onCreate()' 로그가 안 보이면 앱 재설치 필요")

                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Wearable API 초기화 실패", e)
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "WearableListenerService 활성화 실패", e)
        }
    }

    // 워치 연결 상태 확인
    private fun checkWearableConnection() {
        lifecycleScope.launch {
            try {
                tvWearStatus.text = "확인 중..."
                android.util.Log.i("MainActivity", "워치 연결 상태 확인")

                // 연결된 노드 확인
                val nodeClient = Wearable.getNodeClient(this@MainActivity)
                val nodes = nodeClient.connectedNodes.await()

                val statusBuilder = StringBuilder()
                statusBuilder.append("연결된 기기: ${nodes.size}개\n\n")

                if (nodes.isEmpty()) {
                    statusBuilder.append("❌ 연결된 워치가 없습니다.\n")
                    statusBuilder.append("\n확인사항:\n")
                    statusBuilder.append("1. 워치와 폰이 블루투스로 연결되어 있나요?\n")
                    statusBuilder.append("2. 워치 앱이 설치되어 있나요?\n")
                    statusBuilder.append("3. 양쪽 앱이 모두 실행되어 있나요?")
                    android.util.Log.w("MainActivity", "연결된 워치 없음")
                } else {
                    statusBuilder.append("✅ 연결됨:\n\n")
                    nodes.forEachIndexed { index, node ->
                        statusBuilder.append("${index + 1}. ${node.displayName}\n")
                        statusBuilder.append("   ID: ${node.id}\n")
                        statusBuilder.append("   근처: ${if (node.isNearby) "예" else "아니오"}\n\n")

                        android.util.Log.i("MainActivity", "노드: ${node.displayName} (${node.id})")
                    }
                }

                // Capability 확인 (선택적)
                val capabilityClient = Wearable.getCapabilityClient(this@MainActivity)
                val capability = try {
                    capabilityClient.getCapability("voice_recognition", 0).await()
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Capability 확인 실패: ${e.message}")
                    null
                }

                if (capability != null && capability.nodes.isNotEmpty()) {
                    statusBuilder.append("\n음성 인식 가능 기기: ${capability.nodes.size}개")
                }

                tvWearStatus.text = statusBuilder.toString()
                android.util.Log.i("MainActivity", "워치 연결 확인 완료: ${nodes.size}개")

            } catch (e: Exception) {
                val errorMsg = "❌ 오류: ${e.message}"
                tvWearStatus.text = errorMsg
                android.util.Log.e("MainActivity", "워치 연결 확인 실패", e)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // 웨이크워드로 다시 실행된 경우 표시
        if (intent.getBooleanExtra("wake_word_detected", false)) {
            intent.removeExtra("wake_word_detected")
            intent.removeExtra("auto_opened")
            tvStatus.text = "헤이스비 감지!"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }
}
