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
 * "헤이스비" 웨이크워드 감지기
 * 음성 인식을 통해 특정 웨이크워드를 감지합니다.
 */
class WakeWordDetector(private val context: Context) {

    companion object {
        private const val TAG = "WakeWordDetector"
        private const val WAKE_WORD = "헤이스비"
        private val WAKE_WORD_VARIATIONS = listOf(
            "헤이스비",
            "헤이 스비",
            "헤이에스비",
            "헤이 에스비",
            "헤스비",
            "에이스비",
            "페이스비",
            "hey sbi",
            "hey sb",
            "heysby",
            "heysb"
        )

        // 음성 감지 임계값
        private const val MIN_RMS_THRESHOLD = -2.0f // 최소 음성 볼륨
        private const val CONFIDENCE_THRESHOLD = 0.6f // 신뢰도 임계값
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _wakeWordDetected = MutableStateFlow(false)
    val wakeWordDetected: StateFlow<Boolean> = _wakeWordDetected

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    // 음성 레벨 모니터링
    private var currentRmsDb = 0f
    private var speechDetected = false

    init {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "음성 인식을 사용할 수 없습니다")
        }
    }

    /**
     * 웨이크워드 감지 시작
     */
    fun startListening() {
        if (isListening) {
            Log.d(TAG, "이미 감지 중입니다")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

                // 음성 감지 튜닝 - 더 세밀한 감지를 위한 설정
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10) // 더 많은 후보 결과

                // 침묵 감지 시간 조정 (밀리초)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L) // 완전 종료 전 침묵
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L) // 가능성 있는 침묵
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 800L) // 최소 음성 길이

                // 다국어 지원 및 신뢰도 개선
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ko-KR", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)

                // 음성 감지 민감도 향상 (일부 기기에서만 지원)
                putExtra("android.speech.extra.ENABLE_BIASING", true)
                putExtra("android.speech.extra.GET_AUDIO_FORMAT", true)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d(TAG, "웨이크워드 감지 시작: $WAKE_WORD")
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 시작 실패", e)
            isListening = false
        }
    }

    /**
     * 웨이크워드 감지 중지
     */
    fun stopListening() {
        if (!isListening) return

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            Log.d(TAG, "웨이크워드 감지 중지")
        } catch (e: Exception) {
            Log.e(TAG, "음성 인식 중지 실패", e)
        }
    }

    /**
     * 인식된 텍스트에서 웨이크워드 검사
     * 유사도 기반 매칭으로 오인식 개선
     */
    private fun checkForWakeWord(text: String): Boolean {
        val normalizedText = text.lowercase().replace(" ", "")

        return WAKE_WORD_VARIATIONS.any { variation ->
            val normalizedVariation = variation.lowercase().replace(" ", "")
            val found = normalizedText.contains(normalizedVariation) ||
                    calculateSimilarity(normalizedText, normalizedVariation) > 0.7

            if (found) {
                Log.d(TAG, "웨이크워드 감지됨: '$text' (매칭: $variation)")
            }
            found
        }
    }

    /**
     * 레벤슈타인 거리 기반 문자열 유사도 계산
     * 발음이 비슷하게 인식된 경우도 감지
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1

        if (longer.isEmpty()) return 1.0

        val distance = levenshteinDistance(longer, shorter)
        return (longer.length - distance).toDouble() / longer.length
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1) { it }

        for (i in 1..s1.length) {
            var lastValue = i
            for (j in 1..s2.length) {
                val newValue = if (s1[i - 1] == s2[j - 1]) {
                    costs[j - 1]
                } else {
                    minOf(costs[j - 1], costs[j], lastValue) + 1
                }
                costs[j - 1] = lastValue
                lastValue = newValue
            }
            costs[s2.length] = lastValue
        }
        return costs[s2.length]
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "음성 입력 준비 완료")
            speechDetected = false
            currentRmsDb = 0f
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "음성 입력 시작")
            speechDetected = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            // 음성 볼륨 변화 모니터링
            currentRmsDb = rmsdB
            if (rmsdB > MIN_RMS_THRESHOLD) {
                speechDetected = true
                Log.v(TAG, "음성 감지됨: RMS = $rmsdB dB")
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // 오디오 버퍼 수신
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "음성 입력 종료")
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
                SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                SpeechRecognizer.ERROR_NO_MATCH -> "매칭 없음 (재시작)"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "인식기 사용 중"
                SpeechRecognizer.ERROR_SERVER -> "서버 에러"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 타임아웃 (재시작)"
                else -> "알 수 없는 에러: $error"
            }
            Log.w(TAG, "음성 인식 에러: $errorMessage")

            // 타임아웃이나 매칭 없음 에러는 자동 재시작
            isListening = false
            if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                // 짧은 딜레이 후 재시작
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startListening()
                }, 500)
            }
        }

        override fun onResults(results: Bundle?) {
            // 신뢰도 점수 추출 (가능한 경우)
            val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    // 모든 후보 결과 검토 (신뢰도 순)
                    var wakeWordFound = false

                    for (i in matches.indices) {
                        val recognizedText = matches[i]
                        val confidence = confidenceScores?.getOrNull(i) ?: 1.0f

                        Log.d(TAG, "인식 후보 #$i: '$recognizedText' (신뢰도: $confidence)")

                        // 신뢰도가 임계값 이상이고 웨이크워드가 포함된 경우
                        if (confidence >= CONFIDENCE_THRESHOLD && checkForWakeWord(recognizedText)) {
                            _recognizedText.value = recognizedText
                            _wakeWordDetected.value = true
                            wakeWordFound = true

                            Log.i(TAG, "✓ 웨이크워드 최종 감지: '$recognizedText' (신뢰도: $confidence)")

                            // 감지 후 일정 시간 후 자동 리셋
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                _wakeWordDetected.value = false
                            }, 1000)
                            break
                        }
                    }

                    if (!wakeWordFound && matches.isNotEmpty()) {
                        Log.d(TAG, "웨이크워드 미감지 (최상위: '${matches[0]}')")
                    }
                }
            }

            // 결과 처리 후 다시 시작 (연속 감지)
            isListening = false
            startListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // 부분 결과 처리 (실시간 피드백)
            if (!speechDetected) {
                return // 음성이 실제로 감지되지 않은 경우 무시
            }

            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val partialText = matches[0]
                    Log.d(TAG, "부분 인식: $partialText (RMS: $currentRmsDb dB)")

                    // 부분 결과에서도 웨이크워드 체크 (빠른 반응)
                    if (currentRmsDb > MIN_RMS_THRESHOLD && checkForWakeWord(partialText)) {
                        _recognizedText.value = partialText
                        _wakeWordDetected.value = true

                        Log.i(TAG, "✓ 웨이크워드 부분 감지: '$partialText'")

                        // 즉시 중지하고 재시작
                        stopListening()

                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            _wakeWordDetected.value = false
                        }, 1000)
                    }
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // 기타 이벤트
        }
    }

    fun isCurrentlyListening(): Boolean = isListening
}