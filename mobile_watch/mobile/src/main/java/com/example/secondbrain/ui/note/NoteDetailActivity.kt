package com.example.secondbrain.ui.note

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.secondbrain.R
import com.example.secondbrain.data.local.TokenManager
import com.example.secondbrain.data.model.Note
import com.example.secondbrain.data.network.RetrofitClient
import kotlinx.coroutines.launch

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var progressBar: ProgressBar
    private lateinit var tokenManager: TokenManager
    private lateinit var pagerAdapter: NotePagerAdapter

    private var noteId: Long = 0
    private val allNotes = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // TokenManager 초기화
        tokenManager = TokenManager(this)

        // Intent에서 노트 ID 가져오기
        noteId = intent.getLongExtra("NOTE_ID", 0)

        if (noteId == 0L) {
            // 노트 ID가 없으면 에러 표시 후 종료
            finish()
            return
        }

        // View 초기화
        initViews()

        // 노트 데이터 로드
        loadAllNotes()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        viewPager = findViewById(R.id.viewPager)
        progressBar = findViewById(R.id.progressBar)

        // 뒤로가기 버튼
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadAllNotes() {
        progressBar.visibility = View.VISIBLE
        viewPager.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // JWT 토큰에서 userId 추출
                val userId = tokenManager.getUserId()
                if (userId == null) {
                    android.util.Log.e("NoteDetailActivity", "Failed to get userId from token")
                    progressBar.visibility = View.GONE
                    finish()
                    return@launch
                }

                // API 서비스 생성
                val apiService = RetrofitClient.createApiService {
                    tokenManager.getAccessToken()
                }

                // FastAPI 서비스 생성
                val fastApiService = RetrofitClient.createFastApiService {
                    tokenManager.getAccessToken()
                }

                // 1. 현재 노트 상세 조회
                val noteResponse = apiService.getNote(noteId)

                // 2. 연결된 노트 조회 (depth=1)
                val neighborsResponse = fastApiService.getNeighborNodes(
                    noteId = noteId,
                    depth = 1,
                    userId = userId
                )

                // 노트 리스트 구성: 현재 노트를 중심으로, 연결된 노트들을 추가
                allNotes.clear()

                if (noteResponse.code == 200 && noteResponse.data != null) {
                    // 현재 노트 추가
                    allNotes.add(noteResponse.data)

                    // 연결된 노트들 추가 (각 노트의 상세 정보 조회)
                    if (neighborsResponse.neighbors.isNotEmpty()) {
                        neighborsResponse.neighbors.forEach { neighbor ->
                            try {
                                val neighborDetail = apiService.getNote(neighbor.neighborId)
                                if (neighborDetail.code == 200 && neighborDetail.data != null) {
                                    allNotes.add(neighborDetail.data)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("NoteDetailActivity", "Failed to load neighbor: ${neighbor.neighborId}", e)
                            }
                        }
                    }

                    // ViewPager2 설정
                    setupViewPager()
                } else {
                    android.util.Log.e("NoteDetailActivity", "Failed to load note: ${noteResponse.message}")
                    finish()
                }

                progressBar.visibility = View.GONE
                viewPager.visibility = View.VISIBLE

            } catch (e: Exception) {
                android.util.Log.e("NoteDetailActivity", "Failed to load notes", e)
                progressBar.visibility = View.GONE
                finish()
            }
        }
    }

    private fun setupViewPager() {
        pagerAdapter = NotePagerAdapter(this, allNotes)
        viewPager.adapter = pagerAdapter

        // 현재 노트를 첫 번째 페이지로 설정 (이미 첫 번째에 추가했으므로 position 0)
        viewPager.setCurrentItem(0, false)
    }
}
