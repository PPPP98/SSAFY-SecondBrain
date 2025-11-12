/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.secondbrain.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.secondbrain.R
import com.example.secondbrain.presentation.theme.SecondBrainTheme
import com.example.secondbrain.wakeword.WakeWordDetector

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val DOUBLE_CLICK_TIME_DELTA = 500L
        private const val REQUEST_CODE_SPEECH = 100
    }

    private lateinit var wakeWordDetector: WakeWordDetector

    // 홈 버튼 더블 클릭 감지를 위한 변수
    private var homeButtonClickCount = 0
    private val homeButtonHandler = Handler(Looper.getMainLooper())
    private val homeButtonRunnable = Runnable {
        homeButtonClickCount = 0
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d(TAG, "권한 결과: $isGranted")
        if (isGranted) {
            android.util.Log.d(TAG, "권한 승인됨 - 음성 인식 시작")
            startVoiceRecognitionActivity()
        } else {
            android.util.Log.e(TAG, "권한 거부됨 - 음성 인식 불가")
            wakeWordDetector.setError("마이크 권한이 필요합니다")
        }
    }

    // 음성 인식 결과를 받기 위한 launcher
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d(TAG, "음성 인식 결과 코드: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            matches?.let {
                if (it.isNotEmpty()) {
                    val recognizedText = it[0]
                    android.util.Log.i(TAG, "✓ 인식 완료 (Activity): '$recognizedText'")
                    wakeWordDetector.setRecognizedText(recognizedText)
                }
            }
        } else {
            android.util.Log.w(TAG, "음성 인식 취소 또는 실패")
            wakeWordDetector.setError("음성 인식이 취소되었습니다")
        }
        wakeWordDetector.setListening(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        wakeWordDetector = WakeWordDetector(this)

        setContent {
            WearApp(wakeWordDetector) {
                checkAndRequestPermission()
            }
        }
    }

    private fun checkAndRequestPermission() {
        android.util.Log.d(TAG, "권한 체크 시작...")
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                android.util.Log.d(TAG, "권한 있음 - 음성 인식 Activity 실행")
                startVoiceRecognitionActivity()
            }
            else -> {
                android.util.Log.d(TAG, "권한 없음 - 권한 요청")
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
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

            wakeWordDetector.setListening(true)
            wakeWordDetector.clearMessages()

            android.util.Log.d(TAG, "음성 인식 Activity 실행 시도...")
            speechRecognitionLauncher.launch(intent)
        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "권한 부족으로 음성 인식 실패", e)
            wakeWordDetector.setError("마이크 권한이 필요합니다")
            wakeWordDetector.setListening(false)
        } catch (e: android.content.ActivityNotFoundException) {
            android.util.Log.e(TAG, "음성 인식 서비스를 찾을 수 없습니다", e)
            wakeWordDetector.setError("음성 인식 서비스가 설치되어 있지 않습니다")
            wakeWordDetector.setListening(false)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "음성 인식 Activity 실행 실패", e)
            wakeWordDetector.setError("음성 인식을 시작할 수 없습니다")
            wakeWordDetector.setListening(false)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isHomeButton(keyCode)) {
            android.util.Log.d(TAG, "홈 버튼 눌림 감지 - clickCount: $homeButtonClickCount")

            homeButtonClickCount++

            if (homeButtonClickCount == 1) {
                // 첫 번째 클릭 - 타이머 시작
                homeButtonHandler.postDelayed(homeButtonRunnable, DOUBLE_CLICK_TIME_DELTA)
                return super.onKeyDown(keyCode, event)
            } else if (homeButtonClickCount == 2) {
                // 두 번째 클릭 - 더블 클릭 감지!
                android.util.Log.d(TAG, "✓ 홈 버튼 더블 클릭 감지!")
                homeButtonHandler.removeCallbacks(homeButtonRunnable)
                homeButtonClickCount = 0
                onHomeButtonDoubleClick()
                return true // 이벤트 소비
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    /**
     * 홈 버튼 키 코드 체크
     */
    private fun isHomeButton(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_STEM_PRIMARY || keyCode == KeyEvent.KEYCODE_HOME
    }

    private fun onHomeButtonDoubleClick() {
        android.util.Log.d(TAG, "음성 인식 시작 (홈 버튼 더블 클릭)")
        checkAndRequestPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d(TAG, "Activity 종료 - 리소스 정리 중")

        // Handler 콜백 제거
        homeButtonHandler.removeCallbacks(homeButtonRunnable)

        // 음성 인식 종료
        wakeWordDetector.stopListening()
    }
}

@Composable
fun WearApp(wakeWordDetector: WakeWordDetector, onStartListening: () -> Unit) {
    val recognizedText by wakeWordDetector.recognizedText.collectAsState()
    val isListening by wakeWordDetector.isListening.collectAsState()
    val errorMessage by wakeWordDetector.errorMessage.collectAsState()

    SecondBrainTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()

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
                        android.util.Log.d("WearApp", "버튼 클릭! 현재 isListening=$isListening")
                        if (isListening) {
                            android.util.Log.d("WearApp", "중지 호출")
                            wakeWordDetector.stopListening()
                        } else {
                            android.util.Log.d("WearApp", "음성 인식 시작 호출")
                            onStartListening()
                        }
                    }
                ) {
                    Text(if (isListening) "중지" else "말하기")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isListening) "🎤 음성 인식 중" else "홈 버튼 2번 클릭\n또는 '말하기' 버튼",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}