/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.secondbrain.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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

    private lateinit var wakeWordDetector: WakeWordDetector

    // 홈 버튼 더블 클릭 감지를 위한 변수
    private var lastHomeButtonPressTime = 0L
    private val DOUBLE_CLICK_TIME_DELTA = 500L // 500ms 이내 더블 클릭

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("MainActivity", "권한 결과: $isGranted")
        if (isGranted) {
            android.util.Log.d("MainActivity", "권한 승인됨 - 음성 인식 시작")
            // 권한 승인되면 즉시 음성 인식 Activity 실행
            startVoiceRecognitionActivity()
        } else {
            android.util.Log.e("MainActivity", "권한 거부됨 - 음성 인식 불가")
            wakeWordDetector.setError("마이크 권한이 필요합니다")
        }
    }

    // 음성 인식 결과를 받기 위한 launcher
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("MainActivity", "음성 인식 결과 코드: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            matches?.let {
                if (it.isNotEmpty()) {
                    val recognizedText = it[0]
                    android.util.Log.i("MainActivity", "✓ 인식 완료 (Activity): '$recognizedText'")
                    wakeWordDetector.setRecognizedText(recognizedText)
                }
            }
        } else {
            android.util.Log.w("MainActivity", "음성 인식 취소 또는 실패")
            wakeWordDetector.setError("음성 인식 취소됨")
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
        android.util.Log.d("MainActivity", "권한 체크 시작...")
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                android.util.Log.d("MainActivity", "권한 있음 - 음성 인식 Activity 실행")
                startVoiceRecognitionActivity()
            }
            else -> {
                android.util.Log.d("MainActivity", "권한 없음 - 권한 요청")
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

            android.util.Log.d("MainActivity", "음성 인식 Activity 실행 시도...")
            speechRecognitionLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "음성 인식 Activity 실행 실패", e)
            wakeWordDetector.setError("음성 인식을 시작할 수 없습니다: ${e.message}")
            wakeWordDetector.setListening(false)
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        android.util.Log.d("MainActivity", "onKeyDown - keyCode: $keyCode")

        // 홈 버튼(크라운 버튼) 더블 클릭 감지
        if (keyCode == android.view.KeyEvent.KEYCODE_STEM_PRIMARY ||
            keyCode == android.view.KeyEvent.KEYCODE_HOME) {

            android.util.Log.d("MainActivity", "홈 버튼 눌림 감지")
            val currentTime = System.currentTimeMillis()
            val timeDelta = currentTime - lastHomeButtonPressTime

            android.util.Log.d("MainActivity", "시간 차이: ${timeDelta}ms (허용: ${DOUBLE_CLICK_TIME_DELTA}ms)")

            if (timeDelta < DOUBLE_CLICK_TIME_DELTA && lastHomeButtonPressTime != 0L) {
                // 더블 클릭 감지됨!
                android.util.Log.d("MainActivity", "✓ 홈 버튼 더블 클릭 감지!")
                lastHomeButtonPressTime = 0L // 리셋
                onHomeButtonDoubleClick()
                return true // 이벤트 소비
            }

            android.util.Log.d("MainActivity", "첫 번째 클릭 또는 시간 초과")
            lastHomeButtonPressTime = currentTime
            return super.onKeyDown(keyCode, event) // 시스템이 처리하도록
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun onHomeButtonDoubleClick() {
        android.util.Log.d("MainActivity", "음성 인식 시작 (홈 버튼 더블 클릭)")
        checkAndRequestPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
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