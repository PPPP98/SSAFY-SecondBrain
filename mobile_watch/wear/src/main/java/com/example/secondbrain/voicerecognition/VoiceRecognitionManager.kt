package com.example.secondbrain.voicerecognition

import android.content.Context
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 음성 인식 상태 관리 매니저
 *
 * Wear OS에서는 Activity 기반 음성 인식만 사용
 * 이 클래스는 UI 상태 관리 및 메시지 캡슐화를 담당
 */
class VoiceRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceRecognitionMgr"
        private const val PREFIX_RECOGNIZED = "인식: "
    }

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    // UI에 표시할 상태 메시지 (캡슐화)
    private val _statusMessage = MutableStateFlow("음성 인식")
    val statusMessage: StateFlow<String> = _statusMessage

    init {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "음성 인식 사용 불가")
            _errorMessage.value = "음성 인식 사용 불가"
        } else {
            Log.d(TAG, "음성 인식 사용 가능")
        }
    }

    fun isCurrentlyListening(): Boolean = _isListening.value

    fun setListening(listening: Boolean) {
        _isListening.value = listening
        _statusMessage.value = if (listening) "듣는 중..." else "음성 인식"
    }

    fun setRecognizedText(text: String) {
        if (text.isNotBlank()) {
            _recognizedText.value = "$PREFIX_RECOGNIZED$text"
            // 상태 메시지는 MainActivity에서 관리
            Log.d(TAG, "인식 결과: $text")
        }
    }

    fun setError(error: String) {
        _errorMessage.value = error
        _statusMessage.value = "음성 인식"
        Log.w(TAG, "에러: $error")
    }

    fun clearMessages() {
        _recognizedText.value = ""
        _errorMessage.value = ""
        _statusMessage.value = "음성 인식"
    }

    fun stopListening() {
        _isListening.value = false
        _statusMessage.value = "음성 인식"
    }

    fun cleanup() {
        stopListening()
    }
}
