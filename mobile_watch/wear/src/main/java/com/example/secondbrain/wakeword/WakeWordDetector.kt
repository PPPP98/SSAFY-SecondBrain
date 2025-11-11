package com.example.secondbrain.wakeword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 버튼 기반 음성 인식기
 * 버튼을 누르면 음성 인식을 시작하고, 사용자 음성을 텍스트로 변환합니다.
 */
class WakeWordDetector(private val context: Context) {

    companion object {
        private const val TAG = "WakeWordDetector"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    init {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "음성 인식을 사용할 수 없습니다")
        } else {
            Log.d(TAG, "음성 인식 사용 가능")
        }

        // 사용 가능한 음성 인식 서비스 확인
        checkAvailableRecognitionServices()
    }

    private fun checkAvailableRecognitionServices() {
        try {
            val pm = context.packageManager
            val activities = pm.queryIntentActivities(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                0
            )

            if (activities.isEmpty()) {
                Log.e(TAG, "❌ 음성 인식 서비스가 설치되어 있지 않습니다")
                Log.e(TAG, "Wear OS에서는 Google 앱이나 음성 인식 서비스가 필요합니다")
                _errorMessage.value = "음성 인식 서비스를 찾을 수 없습니다"
            } else {
                Log.d(TAG, "✓ 사용 가능한 음성 인식 서비스:")
                activities.forEach { info ->
                    Log.d(TAG, "  - ${info.activityInfo.packageName}")
                }
            }

            // RecognitionService도 체크
            val services = pm.queryIntentServices(
                Intent("android.speech.RecognitionService"),
                0
            )
            if (services.isNotEmpty()) {
                Log.d(TAG, "✓ 사용 가능한 RecognitionService:")
                services.forEach { info ->
                    Log.d(TAG, "  - ${info.serviceInfo.packageName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 서비스 체크 실패", e)
        }
    }

    private fun findBestRecognizer(): android.content.ComponentName? {
        try {
            val pm = context.packageManager
            val services = pm.queryIntentServices(
                Intent("android.speech.RecognitionService"),
                0
            )

            // 우선순위: Google 서비스 > 기타 서비스
            val googleService = services.find {
                it.serviceInfo.packageName.contains("google", ignoreCase = true)
            }

            return if (googleService != null) {
                android.content.ComponentName(
                    googleService.serviceInfo.packageName,
                    googleService.serviceInfo.name
                )
            } else if (services.isNotEmpty()) {
                android.content.ComponentName(
                    services[0].serviceInfo.packageName,
                    services[0].serviceInfo.name
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 서비스 찾기 실패", e)
            return null
        }
    }

    /**
     * 음성 인식 시작 (버튼을 누르면 호출)
     */
    fun startListening() {
        if (_isListening.value) {
            Log.d(TAG, "이미 음성 인식 중입니다")
            return
        }

        try {
            Log.d(TAG, "음성 인식 초기화 시작...")

            // 이전 에러 메시지 초기화
            _errorMessage.value = ""
            _recognizedText.value = ""

            // Wear OS에서 사용 가능한 음성 인식 서비스 찾기
            val recognizerComponent = findBestRecognizer()

            speechRecognizer = if (recognizerComponent != null) {
                Log.d(TAG, "음성 인식 서비스 사용: ${recognizerComponent.packageName}")
                SpeechRecognizer.createSpeechRecognizer(context, recognizerComponent)
            } else {
                Log.w(TAG, "기본 음성 인식 서비스 사용 시도")
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            if (speechRecognizer == null) {
                val errorMsg = "음성 인식 서비스가 없습니다.\nGoogle 앱을 설치해주세요."
                Log.e(TAG, "❌ SpeechRecognizer 생성 실패")
                _isListening.value = false
                _errorMessage.value = errorMsg
                return
            }

            speechRecognizer?.setRecognitionListener(recognitionListener)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }

            Log.d(TAG, "음성 인식 시작...")
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            Log.d(TAG, "✓ 음성 인식 시작됨 (isListening=${_isListening.value})")
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 시작 실패", e)
            _isListening.value = false
            _errorMessage.value = "음성 인식 시작 실패: ${e.message}"
        }
    }

    /**
     * 음성 인식 중지
     */
    fun stopListening() {
        if (!_isListening.value) {
            Log.d(TAG, "이미 중지된 상태입니다")
            return
        }

        try {
            Log.d(TAG, "음성 인식 중지 중...")
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            _isListening.value = false
            Log.d(TAG, "✓ 음성 인식 중지됨 (isListening=${_isListening.value})")
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 중지 실패", e)
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "✓ 음성 입력 준비 완료 - 말씀하세요")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "음성 입력 시작됨")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // 음성 볼륨 모니터링 (디버깅용)
            // Log.v(TAG, "RMS: $rmsdB dB")
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // 오디오 버퍼 수신
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "음성 입력 종료 - 처리 중...")
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러 - 음성 인식 서비스 없음"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
                SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                SpeechRecognizer.ERROR_NO_MATCH -> "음성을 인식하지 못했습니다"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "인식기 사용 중"
                SpeechRecognizer.ERROR_SERVER -> "서버 에러"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 타임아웃"
                else -> "알 수 없는 에러: $error"
            }
            Log.w(TAG, "음성 인식 에러: $errorMessage")

            _isListening.value = false
            _errorMessage.value = errorMessage

            // 정리
            speechRecognizer?.destroy()
            speechRecognizer = null
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val recognizedText = matches[0] // 가장 신뢰도 높은 결과
                    _recognizedText.value = recognizedText
                    Log.i(TAG, "✓ 인식 완료: '$recognizedText'")

                    // 모든 후보 로깅
                    matches.forEachIndexed { index, text ->
                        Log.d(TAG, "  후보 #$index: $text")
                    }
                } else {
                    Log.w(TAG, "인식 결과 없음")
                    _errorMessage.value = "음성을 인식하지 못했습니다"
                }
            }

            // 인식 완료 후 종료
            _isListening.value = false
            speechRecognizer?.destroy()
            speechRecognizer = null
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // 부분 결과 처리 (실시간 피드백)
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val partialText = matches[0]
                    _recognizedText.value = "인식 중: $partialText"
                    Log.d(TAG, "부분 인식: $partialText")
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // 기타 이벤트
        }
    }

    fun isCurrentlyListening(): Boolean = _isListening.value

    /**
     * Activity 기반 음성 인식을 위한 헬퍼 메서드
     */
    fun setListening(listening: Boolean) {
        _isListening.value = listening
        Log.d(TAG, "상태 변경: isListening=$listening")
    }

    fun setRecognizedText(text: String) {
        _recognizedText.value = text
        Log.d(TAG, "텍스트 설정: $text")
    }

    fun setError(error: String) {
        _errorMessage.value = error
        Log.w(TAG, "에러 설정: $error")
    }

    fun clearMessages() {
        _recognizedText.value = ""
        _errorMessage.value = ""
        Log.d(TAG, "메시지 초기화")
    }
}