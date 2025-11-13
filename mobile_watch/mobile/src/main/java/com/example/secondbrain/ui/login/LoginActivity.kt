package com.example.secondbrain.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.secondbrain.MainActivity
import com.example.secondbrain.R
import com.example.secondbrain.data.local.TokenManager
import com.example.secondbrain.data.model.GoogleAuthRequest
import com.example.secondbrain.data.network.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

// 임시 로그인 화면
// TODO: 예나가 추후 UI/UX 개선 예정
class LoginActivity : AppCompatActivity() {

    // Google Sign-In Client
    private lateinit var googleSignInClient: GoogleSignInClient

    // Token Manager
    private lateinit var tokenManager: TokenManager

    // UI 컴포넌트
    private lateinit var btnGoogleSignIn: SignInButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    // Google Sign-In Activity Result Launcher
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // TokenManager 초기화
        tokenManager = TokenManager(this)

        // UI 초기화
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        // Google Sign-In 옵션 설정
        // ⚠️ CRITICAL: requestIdToken()에는 Web Application Client ID를 사용해야 함!
        // Android Client ID를 사용하면 Error 10 발생
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("848577674557-9j4fe7rfata7unetfe176dadhegdtqdl.apps.googleusercontent.com") // Web OAuth Client ID (백엔드 GOOGLE_CLIENT_ID와 동일)
            .requestEmail()
            .build()

        // Google Sign-In Client 생성
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google 로그인 버튼 클릭 리스너
        btnGoogleSignIn.setOnClickListener {
            signIn()
        }
    }

    // Google Sign-In 시작
    private fun signIn() {
        // 기존 계정 로그아웃 (새로운 계정 선택 가능)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }
    }

    // Google Sign-In 결과 처리
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        android.util.Log.d("LoginActivity", "=== Google Sign-In 결과 처리 ===")
        try {
            val account = completedTask.getResult(ApiException::class.java)
            android.util.Log.d("LoginActivity", "Google 계정: ${account?.email}")
            val idToken = account?.idToken

            if (idToken != null) {
                android.util.Log.d("LoginActivity", "ID Token 획득 성공")
                // Google ID Token을 백엔드로 전송
                authenticateWithBackend(idToken)
            } else {
                android.util.Log.e("LoginActivity", "ID Token이 null입니다!")
                showError("ID Token을 가져올 수 없습니다")
            }
        } catch (e: ApiException) {
            android.util.Log.e("LoginActivity", "Google Sign-In 실패!", e)
            android.util.Log.e("LoginActivity", "에러 코드: ${e.statusCode}")
            showError("로그인 실패: ${e.statusCode}")
        }
    }

    // 백엔드로 Google ID Token 전송 및 JWT 토큰 획득
    private fun authenticateWithBackend(idToken: String) {
        showLoading(true)
        android.util.Log.d("LoginActivity", "=== 백엔드 인증 시작 ===")
        android.util.Log.d("LoginActivity", "ID Token 길이: ${idToken.length}")

        lifecycleScope.launch {
            try {
                // API Service 생성
                val apiService = RetrofitClient.createApiService { tokenManager.getAccessToken() }
                android.util.Log.d("LoginActivity", "API Service 생성 완료")

                // Google ID Token을 백엔드로 전송
                val request = GoogleAuthRequest(idToken)
                android.util.Log.d("LoginActivity", "백엔드로 요청 전송 시작...")
                val response = apiService.authenticateWithGoogle(request)
                android.util.Log.d("LoginActivity", "백엔드 응답: code=${response.code}, message=${response.message}")

                if (response.code == 200 && response.data != null) {
                    // JWT 토큰 저장
                    android.util.Log.d("LoginActivity", "JWT 토큰 저장 중...")
                    tokenManager.saveAccessToken(
                        token = response.data.accessToken,
                        tokenType = response.data.tokenType
                    )
                    android.util.Log.d("LoginActivity", "로그인 성공! MainActivity로 이동")

                    // MainActivity로 이동
                    navigateToMain()
                } else {
                    android.util.Log.e("LoginActivity", "인증 실패: ${response.message}")
                    showError("인증 실패: ${response.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "예외 발생!", e)
                android.util.Log.e("LoginActivity", "예외 타입: ${e.javaClass.simpleName}")
                android.util.Log.e("LoginActivity", "예외 메시지: ${e.message}")
                e.printStackTrace()
                showError("네트워크 오류: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    // MainActivity로 이동
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // 로딩 상태 표시
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnGoogleSignIn.isEnabled = !isLoading
    }

    // 에러 메시지 표시
    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
