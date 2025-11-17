package com.example.secondbrain.ui.search

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.secondbrain.R
import com.example.secondbrain.data.model.AgentSearchResponse

/**
 * 검색 입력 화면
 * - 텍스트 검색 및 STT(음성) 검색 지원
 * - 워치 검색 결과 처리 및 SearchResultActivity로 전달
 */
class SearchActivity : AppCompatActivity() {

    // UI 컴포넌트
    private lateinit var etSearchQuery: EditText
    private lateinit var btnVoiceSearch: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var tvVoiceStatus: TextView

    // 음성 인식 결과 처리
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        tvVoiceStatus.visibility = View.GONE

        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                etSearchQuery.setText(recognizedText)
                performSearch(recognizedText)
            }
        } else {
            Toast.makeText(this, "음성 인식 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // 마이크 권한 요청
    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(this, "마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initializeViews()
        setupListeners()

        // 워치에서 전달된 검색 결과 처리
        if (intent.getBooleanExtra("FROM_WEARABLE", false)) {
            handleWearableSearchResult()
        }
        // 워치 알림에서 Deep Link로 실행된 경우
        else if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            handleDeepLink()
        }
        // 웨이크워드로 실행된 경우 자동으로 STT 시작
        else if (intent.getBooleanExtra("auto_start_stt", false)) {
            android.util.Log.d("SearchActivity", "웨이크워드 감지로 실행됨 - STT 자동 시작")
            // UI가 완전히 준비된 후 STT 시작
            btnVoiceSearch.postDelayed({
                checkMicPermissionAndStartVoice()
            }, 500)
        }
    }

    private fun initializeViews() {
        etSearchQuery = findViewById(R.id.etSearchQuery)
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch)
        btnSearch = findViewById(R.id.btnSearch)
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus)
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            val query = etSearchQuery.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(this, "검색어를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        btnVoiceSearch.setOnClickListener {
            checkMicPermissionAndStartVoice()
        }

        // Enter 키로 검색
        etSearchQuery.setOnEditorActionListener { _, _, _ ->
            val query = etSearchQuery.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
                true
            } else {
                false
            }
        }
    }

    private fun checkMicPermissionAndStartVoice() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceRecognition()
            }
            else -> {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            // 한국어로 명시적 설정
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "검색어를 말씀하세요")
        }

        try {
            tvVoiceStatus.visibility = View.VISIBLE
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            tvVoiceStatus.visibility = View.GONE
            Toast.makeText(this, "음성 인식을 시작할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch(query: String) {
        // 검색 결과 페이지로 이동
        val intent = Intent(this, SearchResultActivity::class.java).apply {
            putExtra("SEARCH_QUERY", query)
        }
        startActivity(intent)
    }

    /**
     * 워치에서 전달된 검색 결과 처리
     * SearchResultActivity로 데이터 전달
     */
    private fun handleWearableSearchResult() {
        try {
            // 화면 켜기 및 잠금 화면 위에 표시
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }

            val query = intent.getStringExtra("SEARCH_QUERY") ?: ""
            val responseMessage = intent.getStringExtra("SEARCH_RESPONSE") ?: ""
            val searchResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("SEARCH_RESULT", AgentSearchResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra("SEARCH_RESULT") as? AgentSearchResponse
            }

            android.util.Log.d("SearchActivity", "워치 검색 결과 수신: query='$query', response='$responseMessage'")

            // SearchResultActivity로 이동하여 결과 표시
            val resultIntent = Intent(this, SearchResultActivity::class.java).apply {
                putExtra("SEARCH_QUERY", query)
                putExtra("FROM_WEARABLE", true)
                putExtra("SEARCH_RESPONSE", responseMessage)
                if (searchResult != null) {
                    putExtra("SEARCH_RESULT", searchResult)
                }
            }
            startActivity(resultIntent)
            finish() // SearchActivity 종료

        } catch (e: Exception) {
            android.util.Log.e("SearchActivity", "워치 검색 결과 처리 실패", e)
            Toast.makeText(this, "검색 결과를 표시할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 워치 알림에서 Deep Link로 열렸을 때 처리
     * URI: secondbrain://search?message=<응답 메시지>
     */
    private fun handleDeepLink() {
        try {
            val uri = intent.data
            android.util.Log.d("SearchActivity", "Deep Link 수신: $uri")

            // 화면 켜기 및 잠금 화면 위에 표시
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }

            if (uri?.scheme == "secondbrain" && uri.host == "search") {
                val message = uri.getQueryParameter("message")
                android.util.Log.d("SearchActivity", "Deep Link 메시지: '$message'")

                // SearchResultActivity로 이동하여 메시지 표시
                if (!message.isNullOrBlank()) {
                    val resultIntent = Intent(this, SearchResultActivity::class.java).apply {
                        putExtra("FROM_WEARABLE", true)
                        putExtra("SEARCH_RESPONSE", message)
                    }
                    startActivity(resultIntent)
                    finish() // SearchActivity 종료
                }
            } else {
                android.util.Log.w("SearchActivity", "알 수 없는 Deep Link: $uri")
            }

        } catch (e: Exception) {
            android.util.Log.e("SearchActivity", "Deep Link 처리 실패", e)
            Toast.makeText(this, "알림을 열 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
}
