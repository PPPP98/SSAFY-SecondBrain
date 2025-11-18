package com.example.secondbrain.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.secondbrain.R
import com.example.secondbrain.data.local.TokenManager
import com.example.secondbrain.data.model.AgentNoteResult
import com.example.secondbrain.data.model.NoteSearchResult
import com.example.secondbrain.data.network.RetrofitClient
import com.example.secondbrain.data.network.TtsRequest
import com.example.secondbrain.ui.note.NoteDetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 검색 결과 표시 화면
 * - ElasticSearch와 AI Agent 검색 결과를 병렬로 표시
 * - ElasticSearch 결과를 먼저 표시하고, AI Agent 결과가 오면 추가 표시
 */
class SearchResultActivity : AppCompatActivity() {

    // UI 컴포넌트
    private lateinit var btnBack: TextView
    private lateinit var tvSearchQuery: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResultsTitle: TextView
    private lateinit var tvElasticTitle: TextView
    private lateinit var tvAgentTitle: TextView
    private lateinit var tvAgentLoading: TextView
    private lateinit var tvAgentResponse: TextView
    private lateinit var rvElasticResults: RecyclerView
    private lateinit var rvAgentResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var tvError: TextView
    private lateinit var divider1: View
    private lateinit var divider2: View
    private lateinit var btnPlayTts: ImageButton

    // 어댑터
    private lateinit var elasticAdapter: SearchResultAdapter
    private lateinit var agentAdapter: AgentResultAdapter

    // 데이터
    private lateinit var tokenManager: TokenManager
    private var userId: Long = -1

    // TTS 재생 (ExoPlayer 사용)
    private var exoPlayer: ExoPlayer? = null
    private var agentResponseText: String = ""
    private var currentTempFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_result)

        tokenManager = TokenManager(this)

        // 검색어 가져오기
        val query = intent.getStringExtra("SEARCH_QUERY") ?: ""

        if (query.isEmpty()) {
            finish()
            return
        }

        lifecycleScope.launch {
            userId = tokenManager.getUserId() ?: -1

            if (userId == -1L) {
                tvError.text = "사용자 정보를 찾을 수 없습니다"
                tvError.visibility = View.VISIBLE
                return@launch
            }

            initializeViews()
            setupRecyclerViews()

            // 워치에서 전달받은 데이터가 있는지 확인
            val fromWearable = intent.getBooleanExtra("FROM_WEARABLE", false)
            if (fromWearable) {
                android.util.Log.d("SearchResultActivity", "워치 데이터 사용 (API 호출 없음)")
                handleWearableData()
            } else {
                // 일반 검색 (API 호출)
                performSearch(query)
            }
        }
    }

    /**
     * 워치에서 전달받은 검색 결과를 직접 표시 (API 호출 없음)
     */
    private fun handleWearableData() {
        val query = intent.getStringExtra("SEARCH_QUERY") ?: ""
        val responseMessage = intent.getStringExtra("SEARCH_RESPONSE") ?: ""
        val searchResult = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SEARCH_RESULT", com.example.secondbrain.data.model.AgentSearchResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SEARCH_RESULT") as? com.example.secondbrain.data.model.AgentSearchResponse
        }

        android.util.Log.d("SearchResultActivity", "워치 데이터: query='$query', response='$responseMessage', 노트=${searchResult?.documents?.size ?: 0}개")

        // 검색어 표시
        tvSearchQuery.text = query

        // Agent 응답 및 노트 결과 표시
        if (searchResult != null) {
            displayAgentResults(searchResult.response, searchResult.documents ?: emptyList())
        } else if (responseMessage.isNotEmpty()) {
            displayAgentResults(responseMessage, emptyList())
        }
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        tvSearchQuery = findViewById(R.id.tvSearchQuery)
        progressBar = findViewById(R.id.progressBar)
        tvResultsTitle = findViewById(R.id.tvResultsTitle)
        tvElasticTitle = findViewById(R.id.tvElasticTitle)
        tvAgentTitle = findViewById(R.id.tvAgentTitle)
        tvAgentLoading = findViewById(R.id.tvAgentLoading)
        tvAgentResponse = findViewById(R.id.tvAgentResponse)
        rvElasticResults = findViewById(R.id.rvElasticResults)
        rvAgentResults = findViewById(R.id.rvAgentResults)
        tvNoResults = findViewById(R.id.tvNoResults)
        tvError = findViewById(R.id.tvError)
        divider1 = findViewById(R.id.divider1)
        divider2 = findViewById(R.id.divider2)
        btnPlayTts = findViewById(R.id.btnPlayTts)

        // 검색어 표시
        val query = intent.getStringExtra("SEARCH_QUERY") ?: ""
        tvSearchQuery.text = query

        // 뒤로가기 버튼 클릭 리스너
        btnBack.setOnClickListener {
            finish()
        }

        // TTS 재생 버튼 클릭 리스너
        btnPlayTts.setOnClickListener {
            toggleTtsPlayback()
        }
    }

    private fun setupRecyclerViews() {
        // ElasticSearch 결과 어댑터
        elasticAdapter = SearchResultAdapter { noteId ->
            navigateToNoteDetail(noteId)
        }
        rvElasticResults.apply {
            layoutManager = LinearLayoutManager(this@SearchResultActivity)
            adapter = elasticAdapter
        }

        // AI Agent 결과 어댑터
        agentAdapter = AgentResultAdapter { noteId ->
            navigateToNoteDetail(noteId)
        }
        rvAgentResults.apply {
            layoutManager = LinearLayoutManager(this@SearchResultActivity)
            adapter = agentAdapter
        }
    }

    /**
     * 병렬 검색 실행
     * 1. ElasticSearch (SpringBoot)와 AI Agent (FastAPI)를 동시에 호출
     * 2. ElasticSearch 결과를 먼저 표시
     * 3. AI Agent 결과가 오면 비동기로 추가 표시
     */
    private fun performSearch(query: String) {
        hideAllResults()
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // TokenManager에서 토큰 가져오기
                val apiService = RetrofitClient.createApiService { tokenManager.getAccessToken() }
                val fastApiService = RetrofitClient.createFastApiService { tokenManager.getAccessToken() }

                // 병렬 API 호출 (각각 독립적으로 실행)
                launch {
                    // ElasticSearch 결과 처리
                    try {
                        val apiResponse = apiService.searchNotes(query)
                        apiResponse.data?.let { searchResponse ->
                            displayElasticResults(searchResponse.results)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SearchResultActivity", "ElasticSearch 실패: ${e.message}", e)
                        tvError.text = "검색 실패: ${e.message}"
                        tvError.visibility = View.VISIBLE
                    } finally {
                        progressBar.visibility = View.GONE
                    }
                }

                launch {
                    // AI Agent 결과 처리 (독립적)
                    try {
                        // AI 로딩 상태 표시
                        showAgentLoading()

                        val agentResponse = fastApiService.searchWithAgent(query, userId)

                        // 로딩 상태 숨기고 결과 표시
                        hideAgentLoading()
                        displayAgentResults(agentResponse.response, agentResponse.documents ?: emptyList())
                    } catch (e: Exception) {
                        android.util.Log.e("SearchResultActivity", "Agent 검색 실패: ${e.message}", e)
                        hideAgentLoading()
                        // Agent 실패는 조용히 처리 (선택적 기능)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("SearchResultActivity", "검색 중 오류: ${e.message}", e)
                progressBar.visibility = View.GONE
                tvError.text = "검색 중 오류 발생: ${e.message}"
                tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun displayElasticResults(results: List<NoteSearchResult>) {
        runOnUiThread {
            android.util.Log.d("SearchResultActivity", "displayElasticResults 호출: ${results.size}개")
            if (results.isNotEmpty()) {
                tvResultsTitle.visibility = View.VISIBLE
                tvElasticTitle.visibility = View.VISIBLE
                rvElasticResults.visibility = View.VISIBLE
                divider1.visibility = View.VISIBLE
                elasticAdapter.updateResults(results)
                android.util.Log.d("SearchResultActivity", "ElasticSearch 결과 화면에 표시 완료")
            } else {
                android.util.Log.d("SearchResultActivity", "ElasticSearch 결과가 비어있음")
            }
        }
    }

    private fun showAgentLoading() {
        runOnUiThread {
            tvResultsTitle.visibility = View.VISIBLE
            tvAgentTitle.visibility = View.VISIBLE
            tvAgentLoading.visibility = View.VISIBLE
        }
    }

    private fun hideAgentLoading() {
        runOnUiThread {
            tvAgentLoading.visibility = View.GONE
        }
    }

    private fun displayAgentResults(responseMessage: String, results: List<AgentNoteResult>) {
        runOnUiThread {
            android.util.Log.d("SearchResultActivity", "displayAgentResults 호출: 메시지='$responseMessage', 결과=${results.size}개")

            // AI 응답 메시지는 항상 표시
            if (responseMessage.isNotEmpty()) {
                tvResultsTitle.visibility = View.VISIBLE
                tvAgentTitle.visibility = View.VISIBLE
                tvAgentResponse.visibility = View.VISIBLE
                tvAgentResponse.text = responseMessage
                divider2.visibility = View.VISIBLE

                // 응답 텍스트 저장 및 TTS 버튼 표시
                agentResponseText = responseMessage
                btnPlayTts.visibility = View.VISIBLE

                // TTS 자동 재생
                playTts(responseMessage)
            }

            // 노트 결과가 있으면 RecyclerView 표시
            if (results.isNotEmpty()) {
                rvAgentResults.visibility = View.VISIBLE
                agentAdapter.submitList(results.toList())
                android.util.Log.d("SearchResultActivity", "Agent 노트 결과 ${results.size}개 화면에 표시 완료")
            } else {
                android.util.Log.d("SearchResultActivity", "Agent 노트 결과가 비어있음 (메시지만 표시)")
            }
        }
    }

    private fun hideAllResults() {
        tvResultsTitle.visibility = View.GONE
        tvElasticTitle.visibility = View.GONE
        tvAgentTitle.visibility = View.GONE
        tvAgentLoading.visibility = View.GONE
        tvAgentResponse.visibility = View.GONE
        rvElasticResults.visibility = View.GONE
        rvAgentResults.visibility = View.GONE
        tvNoResults.visibility = View.GONE
    }

    private fun navigateToNoteDetail(noteId: Long) {
        val intent = Intent(this, NoteDetailActivity::class.java).apply {
            putExtra("NOTE_ID", noteId)
        }
        startActivity(intent)
    }

    /**
     * TTS 재생/일시정지 토글
     */
    private fun toggleTtsPlayback() {
        try {
            exoPlayer?.let { player ->
                android.util.Log.d("SearchResultActivity", "현재 상태: isPlaying=${player.isPlaying}, playbackState=${player.playbackState}")

                when {
                    player.isPlaying -> {
                        // 재생 중 → 일시정지
                        player.pause()
                        updatePlayButton()
                        android.util.Log.d("SearchResultActivity", "TTS 일시정지")
                    }
                    player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE -> {
                        // 재생 완료 또는 초기 상태 → 처음부터 다시 재생
                        player.seekTo(0)
                        player.prepare()  // 다시 prepare 필요
                        player.play()
                        updatePlayButton()
                        android.util.Log.d("SearchResultActivity", "TTS 처음부터 재생 (prepare + play)")
                    }
                    else -> {
                        // 일시정지 상태 또는 준비 완료 → 재개
                        player.play()
                        updatePlayButton()
                        android.util.Log.d("SearchResultActivity", "TTS 재개")
                    }
                }
            } ?: run {
                // ExoPlayer가 없으면 처음부터 재생
                android.util.Log.d("SearchResultActivity", "ExoPlayer 없음 → 새로 생성")
                playTts(agentResponseText)
            }
        } catch (e: Exception) {
            android.util.Log.e("SearchResultActivity", "TTS 토글 실패", e)
            updatePlayButton()
        }
    }

    /**
     * TTS 재생 (ExoPlayer 사용 - ID3 태그 건너뛰기)
     */
    private fun playTts(text: String) {
        if (text.isEmpty()) {
            android.util.Log.w("SearchResultActivity", "TTS 텍스트가 비어있음")
            return
        }

        lifecycleScope.launch {
            try {
                android.util.Log.d("SearchResultActivity", "TTS 요청 시작: $text")
                btnPlayTts.isEnabled = false

                // API 서비스 생성
                val apiService = RetrofitClient.createApiService { tokenManager.getAccessToken() }

                // TTS API 호출 (IO 스레드) - JSON Request Body 전송
                val responseBody = withContext(Dispatchers.IO) {
                    apiService.textToSpeech(TtsRequest(text))
                }

                // InputStream으로 직접 읽어서 바이너리 손상 방지
                val tempFile = File.createTempFile("tts_", ".mp3", cacheDir)
                withContext(Dispatchers.IO) {
                    // 직접 InputStream에서 파일로 복사 (바이트 배열 변환 없이)
                    responseBody.byteStream().use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                android.util.Log.d("SearchResultActivity", "TTS 파일 저장 완료: ${tempFile.absolutePath}, 크기: ${tempFile.length()} bytes")

                // ExoPlayer로 재생
                withContext(Dispatchers.Main) {
                    // 기존 플레이어와 임시 파일 정리
                    currentTempFile?.delete()
                    exoPlayer?.release()

                    // 새 임시 파일 저장
                    currentTempFile = tempFile

                    // ExoPlayer 생성 및 설정
                    exoPlayer = ExoPlayer.Builder(this@SearchResultActivity).build().apply {
                        // 재생 완료/에러 리스너 설정
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                when (playbackState) {
                                    Player.STATE_READY -> {
                                        android.util.Log.d("SearchResultActivity", "ExoPlayer 준비 완료")
                                    }
                                    Player.STATE_ENDED -> {
                                        android.util.Log.d("SearchResultActivity", "TTS 재생 완료 (파일 유지)")
                                        updatePlayButton()
                                        // 파일 삭제하지 않음 (다시 재생 가능하도록)
                                    }
                                }
                            }

                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                android.util.Log.e("SearchResultActivity", "ExoPlayer 에러: ${error.message}", error)
                                updatePlayButton()
                                // 에러 시에도 파일 유지 (재시도 가능)
                            }

                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                updatePlayButton()
                            }
                        })

                        // 미디어 아이템 설정 및 재생
                        val mediaItem = MediaItem.fromUri(android.net.Uri.fromFile(tempFile))
                        setMediaItem(mediaItem)
                        prepare()
                        play()
                        android.util.Log.d("SearchResultActivity", "ExoPlayer 재생 시작")
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("SearchResultActivity", "TTS 재생 실패", e)
                withContext(Dispatchers.Main) {
                    updatePlayButton()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnPlayTts.isEnabled = true
                }
            }
        }
    }

    /**
     * 재생 버튼 아이콘 업데이트
     */
    private fun updatePlayButton() {
        val isPlaying = exoPlayer?.isPlaying == true
        btnPlayTts.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // ExoPlayer 해제 및 임시 파일 삭제
        exoPlayer?.release()
        exoPlayer = null
        currentTempFile?.delete()
        currentTempFile = null
    }
}
