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

    // 사용 가능한 서비스를 lazy하게 체크
    private val availableServices: List<String> by lazy {
        checkAvailableRecognitionServices()
    }

    init {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "음성 인식을 사용할 수 없습니다")
        } else {
            Log.d(TAG, "음성 인식 사용 가능")
        }
    }

    private fun checkAvailableRecognitionServices(): List<String> {
        return try {
            val pm = context.packageManager
            val activities = pm.queryIntentActivities(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                0
            )

            if (activities.isEmpty()) {
                Log.w(TAG, "음성 인식 서비스가 설치되어 있지 않습니다")
                _errorMessage.value = "음성 인식 서비스를 찾을 수 없습니다"
                emptyList()
            } else {
                Log.d(TAG, "사용 가능한 음성 인식 서비스: ${activities.size}개")
                activities.map { it.activityInfo.packageName }
            }
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 서비스 체크 실패", e)
            emptyList()
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
            // 이전 메시지 초기화
            _errorMessage.value = ""
            _recognizedText.value = ""

            // Wear OS에서 사용 가능한 음성 인식 서비스 찾기
            val recognizerComponent = findBestRecognizer()

            speechRecognizer = if (recognizerComponent != null) {
                Log.d(TAG, "음성 인식 서비스 사용: ${recognizerComponent.packageName}")
                SpeechRecognizer.createSpeechRecognizer(context, recognizerComponent)
            } else {
                Log.d(TAG, "기본 음성 인식 서비스 사용")
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            if (speechRecognizer == null) {
                Log.e(TAG, "SpeechRecognizer 생성 실패")
                _isListening.value = false
                _errorMessage.value = "음성 인식 서비스가 없습니다.\nGoogle 앱을 설치해주세요."
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

            speechRecognizer?.startListening(intent)
            _isListening.value = true
            Log.d(TAG, "음성 인식 시작")
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 시작 실패", e)
            _isListening.value = false
            _errorMessage.value = "음성 인식을 시작할 수 없습니다"
        }
    }

    /**
     * 음성 인식 중지
     */
    fun stopListening() {
        if (!_isListening.value) {
            return
        }

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            _isListening.value = false
            Log.d(TAG, "음성 인식 중지")
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 중지 실패", e)
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "음성 입력 준비 완료")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "음성 입력 시작")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // 음성 볼륨 모니터링 (필요시 활성화)
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // 오디오 버퍼 수신 (필요시 활성화)
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "음성 입력 종료")
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "오디오 장치에 문제가 있습니다"
                SpeechRecognizer.ERROR_CLIENT -> "음성 인식 서비스를 사용할 수 없습니다"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다"
                SpeechRecognizer.ERROR_NETWORK -> "네트워크에 연결할 수 없습니다"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 연결이 느립니다"
                SpeechRecognizer.ERROR_NO_MATCH -> "다시 말씀해 주세요"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "잠시 후 다시 시도해 주세요"
                SpeechRecognizer.ERROR_SERVER -> "서버 연결에 실패했습니다"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성이 감지되지 않았습니다"
                else -> "음성 인식에 실패했습니다"
            }
            Log.w(TAG, "음성 인식 에러 ($error): $errorMessage")

            _isListening.value = false
            _errorMessage.value = errorMessage

            // 정리
            speechRecognizer?.destroy()
            speechRecognizer = null
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    _recognizedText.value = recognizedText
                    Log.i(TAG, "인식 완료: '$recognizedText'")
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
    }

    fun setRecognizedText(text: String) {
        _recognizedText.value = text
        Log.d(TAG, "인식 결과: $text")
    }

    fun setError(error: String) {
        _errorMessage.value = error
        Log.w(TAG, "에러: $error")
    }

    fun clearMessages() {
        _recognizedText.value = ""
        _errorMessage.value = ""
    }
}