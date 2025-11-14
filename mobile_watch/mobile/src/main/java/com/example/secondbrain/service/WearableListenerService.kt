package com.example.secondbrain.service

import android.util.Log
import com.example.secondbrain.communication.WearableConstants
import com.example.secondbrain.data.local.TokenManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Wear OS에서 전송된 메시지를 수신하는 서비스
 *
 * Google Wearable Data Layer API를 사용하여
 * 워치 앱에서 전송한 음성 텍스트를 수신하고
 * 백엔드 서버로 전달
 */
class WearableListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearableListener"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WearableListenerService 시작 - 워치 메시지 수신 대기")
    }

    /**
     * 워치에서 메시지를 수신했을 때 호출됨
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.i(TAG, "워치 메시지 수신 - 경로: ${messageEvent.path}, 크기: ${messageEvent.data.size}B")

        when (messageEvent.path) {
            WearableConstants.PATH_VOICE_TEXT -> {
                val recognizedText = String(messageEvent.data, Charsets.UTF_8)
                Log.i(TAG, "음성 텍스트: '$recognizedText'")
                scope.launch {
                    sendToBackend(recognizedText)
                }
            }
            WearableConstants.PATH_VOICE_REQUEST -> {
                val requestText = String(messageEvent.data, Charsets.UTF_8)
                Log.i(TAG, "음성 요청: '$requestText'")
                scope.launch {
                    handleVoiceRequest(requestText)
                }
            }
            WearableConstants.PATH_STATUS_REQUEST -> {
                val statusResponse = String(messageEvent.data, Charsets.UTF_8)
                Log.i(TAG, "워치 상태: '$statusResponse'")
                handleStatusResponse(statusResponse)
            }
            else -> {
                Log.w(TAG, "알 수 없는 경로: ${messageEvent.path}")
            }
        }
    }

    /**
     * 백엔드 서버로 음성 텍스트 전송
     *
     * 플로우:
     * 1. 워치에서 STT로 변환된 텍스트 수신
     * 2. 사용자 인증 토큰 확인
     * 3. 백엔드 API에 질문 전송
     * 4. 백엔드 응답을 워치로 전송 → 워치에서 알림 표시
     */
    private suspend fun sendToBackend(text: String) {
        try {
            Log.d(TAG, "백엔드 전송 시작: '$text'")

            // TokenManager를 통해 액세스 토큰 확인
            val tokenManager = TokenManager(applicationContext)
            val accessToken = tokenManager.getAccessToken()

            if (accessToken == null) {
                Log.w(TAG, "액세스 토큰이 없음 - 로그인 필요")
                sendResponseToWear("로그인이 필요합니다.")
                return
            }

            // 임시 응답: 실제 백엔드 API 연동 시 아래 코드로 대체하세요
            // val apiService = RetrofitClient.instance.create(VoiceApiService::class.java)
            // val response = apiService.sendVoiceQuestion(VoiceQuestionRequest(text))
            // if (response.isSuccessful) {
            //     sendResponseToWear(response.body()?.answer ?: "응답 없음")
            // } else {
            //     sendResponseToWear("백엔드 오류: ${response.code()}")
            // }

            val responseText = "질문 받음: $text\n답변: 임시 응답입니다."
            Log.i(TAG, "임시 응답 전송")
            sendResponseToWear(responseText)

        } catch (e: Exception) {
            Log.e(TAG, "백엔드 전송 실패", e)
            sendResponseToWear("오류 발생: ${e.message}")
        }
    }

    /**
     * 음성 요청 처리
     */
    private suspend fun handleVoiceRequest(requestText: String) {
        try {
            Log.d(TAG, "음성 요청 처리: '$requestText'")

            val responseText = "요청을 처리했습니다: $requestText"
            sendResponseToWear(responseText)

        } catch (e: Exception) {
            Log.e(TAG, "음성 요청 처리 실패", e)
            sendResponseToWear("요청 처리 중 오류 발생: ${e.message}")
        }
    }

    /**
     * 워치 상태 응답 처리
     */
    private fun handleStatusResponse(statusResponse: String) {
        Log.i(TAG, "워치 상태: $statusResponse")
        // 필요 시 워치 상태 정보를 SharedPreferences나 Room DB에 저장하여 UI에 표시 가능
    }

    /**
     * 워치로 상태 요청 전송
     *
     * 워치의 현재 상태를 확인하기 위해 상태 요청을 보냅니다.
     * 워치에서는 이 요청을 받아 자신의 상태 정보를 응답으로 전송합니다.
     */
    suspend fun requestWearableStatus() {
        try {
            // 연결된 워치 기기 확인
            val nodes = Wearable.getNodeClient(applicationContext)
                .connectedNodes
                .await()

            if (nodes.isEmpty()) {
                Log.w(TAG, "상태 요청 실패: 연결된 워치 기기 없음")
                return
            }

            val requestData = "status".toByteArray(Charsets.UTF_8)

            // 연결된 모든 워치 기기에 상태 요청 전송
            for (node in nodes) {
                Wearable.getMessageClient(applicationContext)
                    .sendMessage(node.id, WearableConstants.PATH_STATUS_REQUEST, requestData)
                    .await()

                Log.i(TAG, "워치 상태 요청 전송 완료: ${node.displayName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "워치 상태 요청 실패", e)
        }
    }

    /**
     * 워치로 응답 전송
     *
     * 백엔드에서 받은 응답을 워치로 전송합니다.
     * 워치에서는 이 메시지를 받아 알림(Notification)을 표시합니다.
     */
    private suspend fun sendResponseToWear(response: String) {
        try {
            // 연결된 워치 기기 확인
            val nodes = Wearable.getNodeClient(applicationContext)
                .connectedNodes
                .await()

            if (nodes.isEmpty()) {
                Log.w(TAG, "워치 응답 전송 실패: 연결된 기기 없음")
                return
            }

            val data = response.toByteArray(Charsets.UTF_8)

            // 연결된 모든 워치 기기에 응답 전송
            for (node in nodes) {
                Wearable.getMessageClient(applicationContext)
                    .sendMessage(node.id, WearableConstants.PATH_BACKEND_RESPONSE, data)
                    .await()

                Log.i(TAG, "워치로 응답 전송 완료: ${node.displayName} - 워치에서 알림 표시 예정")
            }
        } catch (e: Exception) {
            Log.e(TAG, "워치로 응답 전송 실패", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WearableListenerService 종료")
    }
}