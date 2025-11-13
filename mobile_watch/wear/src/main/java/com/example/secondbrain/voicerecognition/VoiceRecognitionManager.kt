package com.example.secondbrain.voicerecognition

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 버튼 기반 음성 인식 매니저
 *
 * 버튼을 누르면 음성 인식을 시작하고, 사용자 음성을 텍스트로 변환합니다.
 * Wear OS의 제약사항을 고려하여 백그라운드 웨이크워드 방식 대신
 * 버튼 기반 방식으로 구현되었습니다.
 */
class VoiceRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceRecognitionMgr"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    init {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "음성 인식 사용 불가")
            _errorMessage.value = "음성 인식 사용 불가"
        } else {
            Log.d(TAG, "음성 인식 사용 가능")
        }
    }

    /**
     * 최적의 음성 인식 서비스를 찾습니다.
     * 우선순위: Google 서비스 > 기타 서비스
     */
    private fun findBestRecognizer(): android.content.ComponentName? {
        return try {
            val pm = context.packageManager
            val services = pm.queryIntentServices(
                Intent("android.speech.RecognitionService"),
                0
            )

            val googleService = services.find {
                it.serviceInfo.packageName.contains("google", ignoreCase = true)
            }

            when {
                googleService != null -> android.content.ComponentName(
                    googleService.serviceInfo.packageName,
                    googleService.serviceInfo.name
                )
                services.isNotEmpty() -> android.content.ComponentName(
                    services[0].serviceInfo.packageName,
                    services[0].serviceInfo.name
                )
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "서비스 찾기 실패", e)
            null
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
            cleanupRecognizer()
            _isListening.value = false
            Log.d(TAG, "음성 인식 중지")
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 중지 실패", e)
        }
    }

    /**
     * 리소스 정리
     */
    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "리소스 정리 실패", e)
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
                SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                SpeechRecognizer.ERROR_CLIENT -> "서비스 에러"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 필요"
                SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "연결 시간초과"
                SpeechRecognizer.ERROR_NO_MATCH -> "다시 말해주세요"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "잠시 후 재시도"
                SpeechRecognizer.ERROR_SERVER -> "서버 에러"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 없음"
                else -> "인식 실패"
            }
            Log.w(TAG, "에러($error): $errorMessage")

            _isListening.value = false
            _errorMessage.value = errorMessage
            cleanupRecognizer()
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    if (recognizedText.isNotBlank()) {
                        _recognizedText.value = recognizedText
                        Log.i(TAG, "완료: '$recognizedText'")
                    } else {
                        Log.w(TAG, "빈 결과")
                        _errorMessage.value = "인식 실패"
                    }
                } else {
                    Log.w(TAG, "결과 없음")
                    _errorMessage.value = "인식 실패"
                }
            }

            _isListening.value = false
            cleanupRecognizer()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // 부분 결과 처리 (실시간 피드백)
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val partialText = matches[0]
                    if (partialText.isNotBlank()) {
                        _recognizedText.value = "인식 중: $partialText"
                    }
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
        if (text.isNotBlank()) {
            _recognizedText.value = text
            Log.d(TAG, "인식 결과: $text")
        }
    }

    fun setError(error: String) {
        _errorMessage.value = error
        Log.w(TAG, "에러: $error")
    }

    fun clearMessages() {
        _recognizedText.value = ""
        _errorMessage.value = ""
    }

    /**
     * 리소스 정리 - Activity onDestroy에서 호출
     */
    fun cleanup() {
        stopListening()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
