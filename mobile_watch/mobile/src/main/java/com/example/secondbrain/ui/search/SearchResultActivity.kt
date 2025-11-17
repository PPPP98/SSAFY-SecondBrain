package com.example.secondbrain.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.secondbrain.R
import com.example.secondbrain.data.local.TokenManager
import com.example.secondbrain.data.model.AgentNoteResult
import com.example.secondbrain.data.model.NoteSearchResult
import com.example.secondbrain.data.network.RetrofitClient
import com.example.secondbrain.ui.note.NoteDetailActivity
import kotlinx.coroutines.launch

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

    // 어댑터
    private lateinit var elasticAdapter: SearchResultAdapter
    private lateinit var agentAdapter: AgentResultAdapter

    // 데이터
    private lateinit var tokenManager: TokenManager
    private var userId: Long = -1

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
            performSearch(query)
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

        // 검색어 표시
        val query = intent.getStringExtra("SEARCH_QUERY") ?: ""
        tvSearchQuery.text = query

        // 뒤로가기 버튼 클릭 리스너
        btnBack.setOnClickListener {
            finish()
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
}
