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
import androidx.lifecycle.lifecycleScope
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
import com.example.secondbrain.presentation.theme.SecondBrainTheme
import com.example.secondbrain.voicerecognition.VoiceRecognitionManager
import com.example.secondbrain.utils.LogUtils
import com.example.secondbrain.communication.WearableMessageSender
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SUCCESS_DELAY_MS = 1500L  // 전송 성공 시 대기 시간
        private const val FAILURE_DELAY_MS = 500L   // 전송 실패 시 빠른 최소화
    }

    private lateinit var messageSender: WearableMessageSender
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

    // 앱이 처음 실행되었는지 추적 (onCreate 시점)
    private var isFirstLaunch = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        LogUtils.d(TAG, "권한 결과: $isGranted")
        if (isGranted) {
            LogUtils.d(TAG, "권한 승인됨 - 음성 인식 시작")
            permissionDeniedCount = 0
            startVoiceRecognitionActivity()
        } else {
            permissionDeniedCount++
            LogUtils.e(TAG, "권한 거부됨 (${permissionDeniedCount}번째)")

            if (permissionDeniedCount >= 2) {
                // 두 번 이상 거부시 설정으로 이동 안내
                voiceRecognitionManager.setError("마이크 권한 필요\n설정에서 허용해주세요")
                showPermissionSettingsDialog()
            } else {
                voiceRecognitionManager.setError("마이크 권한 필요")
            }
        }
    }

    // Activity 기반 음성 인식 결과 처리
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        LogUtils.d(TAG, "음성 인식 결과: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                if (recognizedText.isNotBlank()) {
                    LogUtils.i(TAG, "인식 완료: '$recognizedText'")
                    voiceRecognitionManager.setRecognizedText(recognizedText)

                    // 모바일 앱으로 텍스트 전송
                    lifecycleScope.launch(Dispatchers.Main) {
                        // 전송 상태 표시 (UI 업데이트는 Main에서)
                        voiceRecognitionManager.setListening(false)
                        voiceRecognitionManager.clearMessages()
                        voiceRecognitionManager.setRecognizedText(recognizedText)

                        // 네트워크 작업은 IO 스레드에서
                        val successCount = withContext(Dispatchers.IO) {
                            messageSender.sendVoiceText(recognizedText)
                        }

                        // UI 업데이트는 다시 Main에서
                        val delayMillis = if (successCount > 0) {
                            LogUtils.i(TAG, "모바일로 전송 성공 (${successCount}개 노드)")
                            SUCCESS_DELAY_MS
                        } else {
                            LogUtils.w(TAG, "모바일로 전송 실패 (모든 노드)")
                            voiceRecognitionManager.setError("모바일 연결 없음")
                            FAILURE_DELAY_MS  // 실패 시 빠르게 최소화
                        }

                        // 전송 결과에 따른 딜레이 후 앱 최소화
                        delay(delayMillis)
                        LogUtils.d(TAG, "앱 최소화")
                        moveTaskToBack(true)
                    }
                } else {
                    LogUtils.w(TAG, "빈 텍스트")
                    voiceRecognitionManager.setError("인식 실패")
                }
            } else {
                LogUtils.w(TAG, "결과 없음")
                voiceRecognitionManager.setError("인식 실패")
            }
        } else {
            LogUtils.w(TAG, "취소됨")
            voiceRecognitionManager.setError("취소됨")
        }
        voiceRecognitionManager.setListening(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        LogUtils.d(TAG, "onCreate - 앱 초기화")

        messageSender = WearableMessageSender(this)

        // VoiceRecognitionManager 초기화 (Activity 기반만 사용하므로 콜백 불필요)
        voiceRecognitionManager = VoiceRecognitionManager(context = this)

        // 연결된 모바일 기기 확인 (디버깅용)
        lifecycleScope.launch {
            messageSender.logConnectedNodes()
        }

        setContent {
            WearApp(
                voiceRecognitionManager = voiceRecognitionManager,
                showOnboarding = showOnboarding,
                onDismissOnboarding = {
                    showOnboarding = false
                    // 온보딩 종료 후 자동으로 음성 인식 시작
                    isFirstLaunch = false
                    checkAndRequestPermission()
                },
                onStartListening = { checkAndRequestPermission() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        LogUtils.d(TAG, "onResume - isFirstLaunch: $isFirstLaunch")

        // 앱이 처음 실행될 때만 자동으로 음성 인식 시작
        // 뒤로가기 등으로 다시 돌아왔을 때는 자동 실행하지 않음
        if (isFirstLaunch && !showOnboarding && !voiceRecognitionManager.isCurrentlyListening()) {
            LogUtils.d(TAG, "첫 실행 - 자동 음성 인식 시작")
            isFirstLaunch = false
            checkAndRequestPermission()
        } else {
            LogUtils.d(TAG, "음성 인식 자동 시작 스킵 (firstLaunch: $isFirstLaunch, onboarding: $showOnboarding)")
        }
    }

    private fun checkAndRequestPermission() {
        // 이미 음성 인식 중이면 중복 실행 방지
        if (voiceRecognitionManager.isCurrentlyListening()) {
            LogUtils.d(TAG, "중복 실행 방지")
            return
        }

        LogUtils.d(TAG, "권한 체크")
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                LogUtils.d(TAG, "권한 있음")
                startVoiceRecognitionActivity()
            }
            else -> {
                LogUtils.d(TAG, "권한 요청")
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
            LogUtils.e(TAG, "설정 열기 실패", e)
        }
    }

    private fun startVoiceRecognitionActivity() {
        try {
            val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "말씀하세요")
                putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }

            voiceRecognitionManager.setListening(true)
            voiceRecognitionManager.clearMessages()

            LogUtils.d(TAG, "음성 인식 시작 (Activity 기반)")
            speechRecognitionLauncher.launch(intent)
        } catch (e: SecurityException) {
            LogUtils.e(TAG, "권한 부족", e)
            voiceRecognitionManager.setError("권한 필요")
            voiceRecognitionManager.setListening(false)
        } catch (e: android.content.ActivityNotFoundException) {
            LogUtils.e(TAG, "서비스 없음", e)
            voiceRecognitionManager.setError("서비스 없음")
            voiceRecognitionManager.setListening(false)
        } catch (e: Exception) {
            LogUtils.e(TAG, "시작 실패", e)
            voiceRecognitionManager.setError("시작 실패")
            voiceRecognitionManager.setListening(false)
        }
    }

    override fun onPause() {
        super.onPause()
        LogUtils.d(TAG, "onPause - 정리")
        // 백그라운드로 갈 때 음성 인식 리소스 해제
        if (voiceRecognitionManager.isCurrentlyListening()) {
            voiceRecognitionManager.stopListening()
        }
        // 진행 중인 네트워크 작업 취소 (불필요한 리소스 사용 방지)
        lifecycleScope.coroutineContext.cancelChildren()
        LogUtils.d(TAG, "진행 중인 코루틴 작업 취소됨")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "Destroy - 정리")
        // lifecycleScope는 자동으로 취소되므로 별도 cancel 불필요
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
    val recognizedText by voiceRecognitionManager.recognizedText.collectAsState(initial = "")
    val isListening by voiceRecognitionManager.isListening.collectAsState(initial = false)
    val errorMessage by voiceRecognitionManager.errorMessage.collectAsState(initial = "")
    val statusMessage by voiceRecognitionManager.statusMessage.collectAsState(initial = "음성 인식")
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
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = statusMessage,
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 말하기 버튼
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

                    // 도움말 버튼 (컴팩트)
                    Button(
                        onClick = { showHelp = true }
                    ) {
                        Text("?")
                    }
                }
            }
        }
    }
}