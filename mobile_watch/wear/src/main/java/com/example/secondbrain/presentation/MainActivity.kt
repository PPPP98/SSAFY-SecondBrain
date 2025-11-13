/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.secondbrain.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.secondbrain.BuildConfig
import com.example.secondbrain.presentation.theme.SecondBrainTheme
import com.example.secondbrain.voicerecognition.VoiceRecognitionManager

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"

        // 디버그 로깅 헬퍼
        private fun logD(message: String) {
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, message)
            }
        }

        private fun logI(message: String) {
            if (BuildConfig.DEBUG) {
                android.util.Log.i(TAG, message)
            }
        }

        private fun logW(message: String) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w(TAG, message)
            }
        }

        private fun logE(message: String, e: Throwable? = null) {
            if (BuildConfig.DEBUG) {
                if (e != null) {
                    android.util.Log.e(TAG, message, e)
                } else {
                    android.util.Log.e(TAG, message)
                }
            }
        }
    }

    private lateinit var voiceRecognitionManager: VoiceRecognitionManager

    // 권한 거부 횟수 추적 (SharedPreferences로 저장)
    private val prefs by lazy {
        getSharedPreferences("voice_recognition_prefs", MODE_PRIVATE)
    }

    private var permissionDeniedCount: Int
        get() = prefs.getInt("permission_denied_count", 0)
        set(value) = prefs.edit().putInt("permission_denied_count", value).apply()

    // 온보딩 표시 여부 (한 번만 표시)
    private var showOnboarding: Boolean
        get() = prefs.getBoolean("show_onboarding", true)
        set(value) = prefs.edit().putBoolean("show_onboarding", value).apply()

    // 음성 인식 시작 플래그 (중복 실행 방지)
    private var hasStartedRecognition = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        logD("권한 결과: $isGranted")
        if (isGranted) {
            logD("권한 승인됨 - 음성 인식 시작")
            permissionDeniedCount = 0
            startVoiceRecognitionActivity()
        } else {
            permissionDeniedCount++
            logE("권한 거부됨 (${permissionDeniedCount}번째) - 음성 인식 불가")

            if (permissionDeniedCount >= 2) {
                // 두 번 이상 거부시 설정으로 이동 안내
                voiceRecognitionManager.setError("마이크 권한이 필요합니다.\n설정에서 권한을 허용해주세요.")
                showPermissionSettingsDialog()
            } else {
                voiceRecognitionManager.setError("마이크 권한이 필요합니다")
            }
        }
    }

    // 음성 인식 결과를 받기 위한 launcher
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        logD("음성 인식 결과 코드: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                if (recognizedText.isNotBlank()) {
                    logI("✓ 인식 완료 (Activity): '$recognizedText'")
                    voiceRecognitionManager.setRecognizedText(recognizedText)

                    // TODO: 여기서 텍스트를 서버로 전송하거나 처리
                    // 처리 완료 후 앱 종료 (메인 화면으로 이동)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        logD("음성 인식 완료 - 앱 최소화")
                        moveTaskToBack(true)
                    }, 1500) // 1.5초 후 앱 최소화
                } else {
                    logW("인식된 텍스트가 비어있음")
                    voiceRecognitionManager.setError("음성을 인식하지 못했습니다")
                }
            } else {
                logW("인식 결과가 null이거나 비어있음")
                voiceRecognitionManager.setError("음성을 인식하지 못했습니다")
            }
        } else {
            logW("음성 인식 취소 또는 실패")
            voiceRecognitionManager.setError("음성 인식이 취소되었습니다")
        }
        voiceRecognitionManager.setListening(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        voiceRecognitionManager = VoiceRecognitionManager(this)

        setContent {
            WearApp(
                voiceRecognitionManager = voiceRecognitionManager,
                showOnboarding = showOnboarding,
                onDismissOnboarding = {
                    showOnboarding = false
                    // 온보딩 종료 후 자동으로 음성 인식 시작
                    checkAndRequestPermission()
                },
                onStartListening = { checkAndRequestPermission() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // 앱이 포그라운드로 올 때마다 자동으로 음성 인식 시작
        // 단, 온보딩 화면이 아닐 때만, 그리고 아직 시작하지 않았을 때만
        if (!showOnboarding && !hasStartedRecognition && !voiceRecognitionManager.isCurrentlyListening()) {
            logD("앱 실행 - 자동으로 음성 인식 시작")
            hasStartedRecognition = true
            checkAndRequestPermission()
        } else {
            logD("온보딩 화면 표시 중이거나 이미 음성 인식 시작됨 - 스킵")
        }
    }

    private fun checkAndRequestPermission() {
        // 이미 음성 인식 중이면 중복 실행 방지
        if (voiceRecognitionManager.isCurrentlyListening()) {
            logD("이미 음성 인식 중 - 중복 실행 방지")
            return
        }

        logD("권한 체크 시작...")
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                logD("권한 있음 - 음성 인식 Activity 실행")
                startVoiceRecognitionActivity()
            }
            else -> {
                logD("권한 없음 - 권한 요청")
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    /**
     * 권한 설정 화면으로 이동
     */
    private fun showPermissionSettingsDialog() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            logE("설정 화면 열기 실패", e)
        }
    }

    private fun startVoiceRecognitionActivity() {
        try {
            val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "말씀하세요")
                putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }

            voiceRecognitionManager.setListening(true)
            voiceRecognitionManager.clearMessages()

            logD("음성 인식 Activity 실행 시도...")
            speechRecognitionLauncher.launch(intent)
        } catch (e: SecurityException) {
            logE("권한 부족으로 음성 인식 실패", e)
            voiceRecognitionManager.setError("마이크 권한이 필요합니다")
            voiceRecognitionManager.setListening(false)
        } catch (e: android.content.ActivityNotFoundException) {
            logE("음성 인식 서비스를 찾을 수 없습니다", e)
            voiceRecognitionManager.setError("음성 인식 서비스가 설치되어 있지 않습니다")
            voiceRecognitionManager.setListening(false)
        } catch (e: Exception) {
            logE("음성 인식 Activity 실행 실패", e)
            voiceRecognitionManager.setError("음성 인식을 시작할 수 없습니다")
            voiceRecognitionManager.setListening(false)
        }
    }

    override fun onPause() {
        super.onPause()
        logD("Activity paused - 음성 인식 정리")
        // 백그라운드로 갈 때 음성 인식 리소스 해제
        if (voiceRecognitionManager.isCurrentlyListening()) {
            voiceRecognitionManager.stopListening()
        }
        // 플래그 리셋 - 다음에 다시 앱을 열 때 음성 인식을 시작할 수 있도록
        hasStartedRecognition = false
    }

    override fun onDestroy() {
        super.onDestroy()
        logD("Activity 종료 - 리소스 정리 중")

        // 음성 인식 리소스 정리
        voiceRecognitionManager.cleanup()
    }
}

@Composable
fun WearApp(
    voiceRecognitionManager: VoiceRecognitionManager,
    showOnboarding: Boolean,
    onDismissOnboarding: () -> Unit,
    onStartListening: () -> Unit
) {
    val recognizedText by voiceRecognitionManager.recognizedText.collectAsState()
    val isListening by voiceRecognitionManager.isListening.collectAsState()
    val errorMessage by voiceRecognitionManager.errorMessage.collectAsState()
    var showHelp by remember { mutableStateOf(showOnboarding) }

    SecondBrainTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()

            if (showHelp) {
                // 온보딩/도움말 화면
                OnboardingScreen(
                    onDismiss = {
                        showHelp = false
                        onDismissOnboarding()
                    }
                )
            } else {
                // 메인 화면
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isListening) "듣는 중..." else "음성 인식",
                        style = MaterialTheme.typography.title3,
                        color = if (isListening) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (recognizedText.isNotEmpty()) {
                        Text(
                            text = recognizedText,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isListening) {
                                voiceRecognitionManager.stopListening()
                            } else {
                                onStartListening()
                            }
                        },
                        enabled = !isListening || errorMessage.isEmpty()
                    ) {
                        Text(if (isListening) "중지" else "말하기")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isListening) "🎤 음성 인식 중" else "앱 실행 시 자동 시작\n또는 '말하기' 버튼",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 도움말 버튼
                    Button(
                        onClick = { showHelp = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("?")
                    }
                }
            }
        }
    }
}