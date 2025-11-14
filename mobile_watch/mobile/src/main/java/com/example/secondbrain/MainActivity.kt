package com.example.secondbrain

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        etNoteId = findViewById(R.id.etNoteId)
        btnSearchNote = findViewById(R.id.btnSearchNote)
        tvTestResult = findViewById(R.id.tvTestResult)
        etSearchKeyword = findViewById(R.id.etSearchKeyword)
        btnSearch = findViewById(R.id.btnSearch)
        rvSearchResults = findViewById(R.id.rvSearchResults)

        // RecyclerView 설정
        setupRecyclerView()

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

        lifecycleScope.launch {
            try {
                // 토큰 확인
                val token = tokenManager.getAccessToken()
                if (token.isNullOrEmpty()) {
                    rvSearchResults.visibility = View.GONE
                    return@launch
                }

                // API 서비스 생성
                val apiService = com.example.secondbrain.data.network.RetrofitClient.createApiService {
                    tokenManager.getAccessToken()
                }

                // 검색 실행
                val response = apiService.searchNotes(keyword)

                if (response.code == 200 && response.data != null) {
                    val searchResponse = response.data
                    if (searchResponse.results.isNotEmpty()) {
                        searchAdapter.updateResults(searchResponse.results)
                        rvSearchResults.visibility = View.VISIBLE
                    } else {
                        searchAdapter.updateResults(emptyList())
                        rvSearchResults.visibility = View.GONE
                    }
                } else {
                    searchAdapter.updateResults(emptyList())
                    rvSearchResults.visibility = View.GONE
                }
            } catch (e: Exception) {
                searchAdapter.updateResults(emptyList())
                rvSearchResults.visibility = View.GONE
                android.util.Log.e("MainActivity", "Search failed", e)
            }
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