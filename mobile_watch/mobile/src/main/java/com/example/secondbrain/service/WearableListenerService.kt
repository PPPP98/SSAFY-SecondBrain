package com.example.secondbrain.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.secondbrain.R
import com.example.secondbrain.communication.WearableConstants
import com.example.secondbrain.data.local.TokenManager
import com.example.secondbrain.data.model.AgentSearchResponse
import com.example.secondbrain.data.network.RetrofitClient
import com.example.secondbrain.ui.search.SearchActivity
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
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
class MobileWearableListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearableListener"
        private const val NOTIFICATION_CHANNEL_ID = "wearable_search_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MobileWearableListenerService onCreate() 호출됨!")
        Log.i(TAG, "WearableListenerService 시작 - 워치 데이터 수신 대기 (백그라운드 동작)")
    }

    /**
     * 워치에서 DataItem을 수신했을 때 호출됨 (백그라운드에서도 동작!)
     */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        Log.i(TAG, "DataItem 수신됨: ${dataEvents.count}개")

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                Log.i(TAG, "DataItem 경로: ${dataItem.uri.path}")

                when (dataItem.uri.path) {
                    WearableConstants.PATH_VOICE_TEXT -> {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        val recognizedText = dataMap.getString("text") ?: ""
                        val timestamp = dataMap.getLong("timestamp", 0L)

                        Log.i(TAG, "음성 텍스트 수신: '$recognizedText' (timestamp: $timestamp)")

                        scope.launch {
                            sendToBackend(recognizedText)
                        }
                    }
                    else -> {
                        Log.w(TAG, "알 수 없는 경로: ${dataItem.uri.path}")
                    }
                }
            }
        }
    }

    /**
     * 워치에서 메시지를 수신했을 때 호출됨 (백엔드 응답용)
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.i(TAG, "워치 메시지 수신 - 경로: ${messageEvent.path}, 크기: ${messageEvent.data.size}B")

        when (messageEvent.path) {
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
            WearableConstants.PATH_OPEN_ON_PHONE -> {
                val responseText = String(messageEvent.data, Charsets.UTF_8)
                Log.i(TAG, "폰에서 열기 요청 수신: '$responseText'")
                scope.launch {
                    showFullScreenNotification(responseText)
                }
            }
            else -> {
                Log.w(TAG, "알 수 없는 경로: ${messageEvent.path}")
            }
        }
    }

    /**
     * 백엔드 서버로 음성 텍스트 전송 (FastAPI AI Agent 검색)
     *
     * 플로우:
     * 1. 워치에서 STT로 변환된 텍스트 수신
     * 2. 사용자 인증 토큰 확인
     * 3. FastAPI Agent 검색 API 호출
     * 4. 검색 결과를 폰에 알림으로 표시
     */
    private suspend fun sendToBackend(text: String) {
        try {
            Log.d(TAG, "FastAPI 검색 시작: '$text'")

            // TokenManager를 통해 액세스 토큰 및 사용자 ID 확인
            val tokenManager = TokenManager(applicationContext)
            val accessToken = tokenManager.getAccessToken()
            val userId = tokenManager.getUserId()

            if (accessToken == null || userId == null) {
                Log.w(TAG, "액세스 토큰 또는 사용자 ID가 없음 - 로그인 필요")
                sendNotificationToPhone("로그인이 필요합니다.", null, null)
                return
            }

            // FastAPI Agent 검색 API 호출
            val fastApiService = RetrofitClient.createFastApiService { accessToken }
            val searchResponse = fastApiService.searchWithAgent(text, userId)

            Log.i(TAG, "AI 검색 완료: ${searchResponse.response}")
            Log.i(TAG, "검색된 노트 수: ${searchResponse.documents?.size ?: 0}")

            // 폰에 알림 전송 (검색어, 응답 메시지, 검색 결과)
            sendNotificationToPhone(text, searchResponse.response, searchResponse)

        } catch (e: Exception) {
            Log.e(TAG, "FastAPI 검색 실패", e)
            sendNotificationToPhone(text, "검색 중 오류가 발생했습니다: ${e.message}", null)
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
     * 폰에 알림 표시 및 워치에 알림 전송
     *
     * @param query 검색어
     * @param responseMessage AI 응답 메시지
     * @param searchResponse 검색 결과 (AgentSearchResponse)
     */
    private suspend fun sendNotificationToPhone(
        query: String,
        responseMessage: String?,
        searchResponse: AgentSearchResponse?
    ) {
        try {
            // 알림 채널 생성 (Android 8.0 이상)
            createNotificationChannel()

            // SearchActivity로 이동하는 Intent 생성
            val intent = Intent(applicationContext, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("FROM_WEARABLE", true)
                putExtra("SEARCH_QUERY", query)
                putExtra("SEARCH_RESPONSE", responseMessage)
                if (searchResponse != null) {
                    putExtra("SEARCH_RESULT", searchResponse)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알림 생성
            val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 적절한 아이콘으로 변경 필요
                .setContentTitle("워치 검색 완료: $query")
                .setContentText(responseMessage ?: "검색 결과를 확인하세요")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_launcher_foreground, // 적절한 아이콘으로 변경 필요
                    "폰으로 보기",
                    pendingIntent
                )
                .build()

            // 알림 표시
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            Log.i(TAG, "폰에 알림 표시 완료")

            // 워치에도 알림 전송
            sendResponseToWear(responseMessage ?: "검색 완료")

        } catch (e: Exception) {
            Log.e(TAG, "폰 알림 표시 실패", e)
        }
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "워치 검색 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "워치에서 보낸 검색 결과 알림"
                // Full-Screen Intent를 위한 설정
                setBypassDnd(true) // 방해 금지 모드 우회
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "알림 채널 생성 완료 - Full-Screen Intent 지원")
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
            Log.d(TAG, "워치로 응답 전송 시작: '$response'")

            // 연결된 워치 기기 확인
            val nodes = Wearable.getNodeClient(applicationContext)
                .connectedNodes
                .await()

            Log.d(TAG, "연결된 워치 기기 수: ${nodes.size}")

            if (nodes.isEmpty()) {
                Log.w(TAG, "⚠️ 워치 응답 전송 실패: 연결된 기기 없음")
                return
            }

            val data = response.toByteArray(Charsets.UTF_8)
            Log.d(TAG, "전송할 데이터 크기: ${data.size} bytes")

            // 연결된 모든 워치 기기에 응답 전송
            for (node in nodes) {
                Log.d(TAG, "워치 기기 정보 - ID: ${node.id}, 이름: ${node.displayName}, 근처: ${node.isNearby}")

                val result = Wearable.getMessageClient(applicationContext)
                    .sendMessage(node.id, WearableConstants.PATH_BACKEND_RESPONSE, data)
                    .await()

                Log.i(TAG, "✅ 워치로 응답 전송 완료!")
                Log.i(TAG, "  - 노드: ${node.displayName}")
                Log.i(TAG, "  - 경로: ${WearableConstants.PATH_BACKEND_RESPONSE}")
                Log.i(TAG, "  - 요청 ID: $result")
                Log.i(TAG, "  - 워치에서 알림 표시 예정")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 워치로 응답 전송 실패", e)
            Log.e(TAG, "에러 상세: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Full-Screen Intent 알림 표시
     *
     * 워치의 "폰에서 보기" 버튼 클릭 시 폰 화면을 켜고 SearchActivity를 표시합니다.
     */
    private fun showFullScreenNotification(responseText: String) {
        try {
            Log.d(TAG, "Full-Screen 알림 표시: '$responseText'")

            // 알림 채널 생성
            createNotificationChannel()

            // SearchActivity로 이동하는 Intent 생성
            val intent = Intent(applicationContext, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("FROM_WEARABLE", true)
                putExtra("SEARCH_RESPONSE", responseText)
            }

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Full-Screen Intent 생성 (다른 request code 사용)
            val fullScreenIntent = PendingIntent.getActivity(
                applicationContext,
                3,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알림 생성
            val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("📱 워치 검색 결과")
                .setContentText(responseText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(responseText))
                .setPriority(NotificationCompat.PRIORITY_MAX) // MAX로 변경
                .setCategory(NotificationCompat.CATEGORY_CALL) // CALL로 변경 (더 높은 우선순위)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(fullScreenIntent, true) // 폰 화면을 자동으로 켬
                .setVibrate(longArrayOf(0, 500, 200, 500)) // 진동 패턴 추가
                .setLights(0xFF0000FF.toInt(), 1000, 1000) // LED 설정
                .build()

            // 알림 표시
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID + 1, notification)

            Log.i(TAG, "✅ Full-Screen 알림 표시 완료")
            Log.i(TAG, "   - Category: CALL (높은 우선순위)")
            Log.i(TAG, "   - Priority: MAX")
            Log.i(TAG, "   - Full-Screen Intent: 활성화")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Full-Screen 알림 표시 실패", e)
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WearableListenerService 종료")
    }
}