package com.example.secondbrain.ui.note

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.secondbrain.R
import com.example.secondbrain.data.local.TokenManager
import com.example.secondbrain.data.network.RetrofitClient
import kotlinx.coroutines.launch

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var tvNoteTitle: TextView
    private lateinit var tvNoteContent: TextView
    private lateinit var tvNoteDate: TextView
    private lateinit var tvLoadingNeighbors: TextView
    private lateinit var layoutNeighborNotes: LinearLayout
    private lateinit var tvNoNeighbors: TextView
    private lateinit var tokenManager: TokenManager

    private var noteId: Long = 0

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

        // 노트 상세 정보 로드
        loadNoteDetail()

        // 연결된 노트 로드
        loadNeighborNotes()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvNoteTitle = findViewById(R.id.tvNoteTitle)
        tvNoteContent = findViewById(R.id.tvNoteContent)
        tvNoteDate = findViewById(R.id.tvNoteDate)
        tvLoadingNeighbors = findViewById(R.id.tvLoadingNeighbors)
        layoutNeighborNotes = findViewById(R.id.layoutNeighborNotes)
        tvNoNeighbors = findViewById(R.id.tvNoNeighbors)

        // 뒤로가기 버튼
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadNoteDetail() {
        lifecycleScope.launch {
            try {
                // API 서비스 생성
                val apiService = RetrofitClient.createApiService {
                    tokenManager.getAccessToken()
                }

                // 노트 상세 조회
                val response = apiService.getNote(noteId)

                if (response.code == 200 && response.data != null) {
                    val note = response.data
                    tvNoteTitle.text = note.title
                    tvNoteContent.text = note.content
                    tvNoteDate.text = "생성일: ${note.createdAt}"
                } else {
                    tvNoteTitle.text = "오류"
                    tvNoteContent.text = response.message ?: "노트를 불러올 수 없습니다."
                }
            } catch (e: Exception) {
                tvNoteTitle.text = "오류"
                tvNoteContent.text = "네트워크 오류: ${e.message}"
                android.util.Log.e("NoteDetailActivity", "Failed to load note", e)
            }
        }
    }

    private fun loadNeighborNotes() {
        lifecycleScope.launch {
            try {
                // JWT 토큰에서 userId 추출
                val userId = tokenManager.getUserId()
                if (userId == null) {
                    tvLoadingNeighbors.visibility = TextView.GONE
                    tvNoNeighbors.text = "사용자 ID를 확인할 수 없습니다.\n다시 로그인해주세요."
                    tvNoNeighbors.visibility = TextView.VISIBLE
                    android.util.Log.e("NoteDetailActivity", "Failed to get userId from token")
                    return@launch
                }

                android.util.Log.d("NoteDetailActivity", "Extracted userId: $userId")

                // FastAPI 서비스 생성
                val fastApiService = RetrofitClient.createFastApiService {
                    tokenManager.getAccessToken()
                }

                // 연결된 노트 조회 (depth=1)
                val response = fastApiService.getNeighborNodes(
                    noteId = noteId,
                    depth = 1,
                    userId = userId
                )

                // 로딩 메시지 숨기기
                tvLoadingNeighbors.visibility = TextView.GONE

                if (response.neighbors.isNotEmpty()) {
                    // 연결된 노트가 있으면 표시
                    layoutNeighborNotes.visibility = LinearLayout.VISIBLE
                    layoutNeighborNotes.removeAllViews()

                    response.neighbors.forEach { neighbor ->
                        val noteCard = createNeighborNoteCard(neighbor.neighborId, neighbor.neighborTitle)
                        layoutNeighborNotes.addView(noteCard)
                    }
                } else {
                    // 연결된 노트가 없으면 메시지 표시
                    tvNoNeighbors.visibility = TextView.VISIBLE
                }
            } catch (e: Exception) {
                // 오류 발생 시 (서버 에러, 네트워크 오류 등)
                tvLoadingNeighbors.visibility = TextView.GONE
                tvNoNeighbors.text = "연결된 노트가 없습니다."
                tvNoNeighbors.visibility = TextView.VISIBLE
                android.util.Log.e("NoteDetailActivity", "Failed to load neighbors (showing as empty)", e)
            }
        }
    }

    private fun createNeighborNoteCard(neighborId: Long, neighborTitle: String): CardView {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            radius = dpToPx(8).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        val titleTextView = TextView(this).apply {
            text = neighborTitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.parseColor("#333333"))
        }

        val clickHintTextView = TextView(this).apply {
            text = "탭하여 노트 보기 →"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#007BFF"))
            setPadding(0, dpToPx(8), 0, 0)
        }

        contentLayout.addView(titleTextView)
        contentLayout.addView(clickHintTextView)
        cardView.addView(contentLayout)

        // 클릭 시 해당 노트로 이동
        cardView.setOnClickListener {
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra("NOTE_ID", neighborId)
            startActivity(intent)
        }

        return cardView
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
