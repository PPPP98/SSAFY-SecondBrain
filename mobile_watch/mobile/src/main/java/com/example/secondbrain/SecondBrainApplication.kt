package com.example.secondbrain

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.secondbrain.communication.WearableConstants
import com.example.secondbrain.data.local.TokenManager
import com.example.secondbrain.data.model.AgentSearchResponse
import com.example.secondbrain.data.network.RetrofitClient
import com.example.secondbrain.ui.search.SearchActivity
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Application 클래스
 * 앱 시작 시 WearableListenerService 바인딩 트리거
 */
class SecondBrainApplication : Application() {

    companion object {
        private const val TAG = "SecondBrainApp"
        private const val NOTIFICATION_CHANNEL_ID = "wearable_search_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 최근 검색 결과를 저장 (워치 "폰에서 보기" 버튼용)
    private var lastSearchResponse: AgentSearchResponse? = null
    private var lastSearchQuery: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application onCreate - 앱 시작")

        // WearableListenerService 바인딩 트리거
        initializeWearableService()
    }

    private fun initializeWearableService() {
        applicationScope.launch {
            try {
                Log.i(TAG, "=========================================")
                Log.i(TAG, "WearableListenerService 초기화 시작")
                Log.i(TAG, "=========================================")

                // Google Play Services 버전 확인
                try {
                    val gmsVersion = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                        .isGooglePlayServicesAvailable(this@SecondBrainApplication)
                    Log.i(TAG, "Google Play Services 상태: $gmsVersion (0이면 정상)")
                } catch (e: Exception) {
                    Log.e(TAG, "Google Play Services 확인 실패", e)
                }

                // DataClient 생성 및 수동 리스너 등록
                Log.i(TAG, "DataClient 생성 및 리스너 등록 중...")
                val dataClient = Wearable.getDataClient(this@SecondBrainApplication)

                // 수동으로 DataListener 등록
                val dataListener = com.google.android.gms.wearable.DataClient.OnDataChangedListener { dataEvents ->
                    Log.i(TAG, "🔥 DataListener 호출됨! ${dataEvents.count}개 데이터 수신")

                    for (event in dataEvents) {
                        if (event.type == com.google.android.gms.wearable.DataEvent.TYPE_CHANGED) {
                            val dataItem = event.dataItem
                            Log.i(TAG, "DataItem 경로: ${dataItem.uri.path}")

                            when (dataItem.uri.path) {
                                "/voice_text" -> {
                                    val dataMap = com.google.android.gms.wearable.DataMapItem.fromDataItem(dataItem).dataMap
                                    val recognizedText = dataMap.getString("text") ?: ""
                                    val timestamp = dataMap.getLong("timestamp", 0L)

                                    Log.i(TAG, "✓ 음성 텍스트 수신: '$recognizedText' (timestamp: $timestamp)")

                                    // FastAPI Agent 검색 API 호출
                                    applicationScope.launch(Dispatchers.IO) {
                                        searchWithAgent(recognizedText)
                                    }
                                }
                                else -> {
                                    Log.w(TAG, "알 수 없는 경로: ${dataItem.uri.path}")
                                }
                            }
                        }
                    }
                }

                dataClient.addListener(dataListener)
                Log.i(TAG, "✓ DataListener 수동 등록 완료!")

                // MessageClient 수동 리스너 등록 (워치 알림의 "폰에서 보기" 버튼용)
                Log.i(TAG, "MessageClient 리스너 등록 중...")
                val messageClient = Wearable.getMessageClient(this@SecondBrainApplication)

                val messageListener = com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener { messageEvent ->
                    Log.i(TAG, "🔥 MessageListener 호출됨! 경로: ${messageEvent.path}")

                    when (messageEvent.path) {
                        WearableConstants.PATH_OPEN_ON_PHONE -> {
                            val responseText = String(messageEvent.data, Charsets.UTF_8)
                            Log.i(TAG, "✓ 폰에서 열기 요청 수신: '$responseText'")

                            // Full-Screen 알림 표시
                            applicationScope.launch(Dispatchers.Main) {
                                showFullScreenNotification(responseText)
                            }
                        }
                        else -> {
                            Log.w(TAG, "알 수 없는 메시지 경로: ${messageEvent.path}")
                        }
                    }
                }

                messageClient.addListener(messageListener)
                Log.i(TAG, "✓ MessageListener 수동 등록 완료!")

                Log.i(TAG, "=========================================")
                Log.i(TAG, "Data Layer + Message 리스너 등록 완료!")
                Log.i(TAG, "이제 워치에서 데이터/메시지를 전송하면 여기서 수신합니다")
                Log.i(TAG, "=========================================")

            } catch (e: Exception) {
                Log.e(TAG, "❌ WearableListenerService 초기화 실패", e)
                Log.e(TAG, "스택 트레이스:", e)
            }
        }
    }

    /**
     * FastAPI Agent 검색 API 호출
     */
    private suspend fun searchWithAgent(query: String) {
        try {
            Log.d(TAG, "FastAPI 검색 시작: '$query'")

            // TokenManager를 통해 액세스 토큰 및 사용자 ID 확인
            val tokenManager = TokenManager(this)
            val accessToken = tokenManager.getAccessToken()
            val userId = tokenManager.getUserId()

            if (accessToken == null || userId == null) {
                Log.w(TAG, "액세스 토큰 또는 사용자 ID가 없음 - 로그인 필요")
                sendNotificationToPhone(query, "로그인이 필요합니다.", null)
                sendResponseToWear("로그인이 필요합니다.")
                return
            }

            // FastAPI Agent 검색 API 호출
            val fastApiService = RetrofitClient.createFastApiService { accessToken }
            val searchResponse = fastApiService.searchWithAgent(query, userId)

            Log.i(TAG, "AI 검색 완료: ${searchResponse.response}")
            Log.i(TAG, "검색된 노트 수: ${searchResponse.documents?.size ?: 0}")

            // 검색 결과 저장 (나중에 "폰에서 보기" 버튼용)
            lastSearchQuery = query
            lastSearchResponse = searchResponse

            // 폰에 알림 전송 (검색어, 응답 메시지, 검색 결과)
            sendNotificationToPhone(query, searchResponse.response, searchResponse)

            // 워치에 알림 전송
            sendResponseToWear(searchResponse.response)

        } catch (e: Exception) {
            Log.e(TAG, "FastAPI 검색 실패", e)
            sendNotificationToPhone(query, "검색 중 오류가 발생했습니다: ${e.message}", null)
            sendResponseToWear("검색 중 오류가 발생했습니다.")
        }
    }

    /**
     * 폰에 알림 표시
     */
    private fun sendNotificationToPhone(
        query: String,
        responseMessage: String?,
        searchResponse: AgentSearchResponse?
    ) {
        try {
            // 알림 채널 생성 (Android 8.0 이상)
            createNotificationChannel()

            // SearchActivity로 이동하는 Intent 생성
            val intent = Intent(this, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("FROM_WEARABLE", true)
                putExtra("SEARCH_QUERY", query)
                putExtra("SEARCH_RESPONSE", responseMessage)
                if (searchResponse != null) {
                    putExtra("SEARCH_RESULT", searchResponse)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알림 생성
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("워치 검색 완료: $query")
                .setContentText(responseMessage ?: "검색 결과를 확인하세요")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    "폰으로 보기",
                    pendingIntent
                )
                .build()

            // 알림 표시
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            Log.i(TAG, "폰에 알림 표시 완료")

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
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Full-Screen Intent 알림 표시 (워치 "폰에서 보기" 버튼용)
     */
    private fun showFullScreenNotification(responseText: String) {
        try {
            Log.d(TAG, "Full-Screen 알림 표시: '$responseText'")

            // 알림 채널 생성
            createFullScreenNotificationChannel()

            // SearchActivity로 이동하는 Intent 생성
            val intent = Intent(this, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("FROM_WEARABLE", true)
                putExtra("SEARCH_QUERY", lastSearchQuery ?: "")
                putExtra("SEARCH_RESPONSE", responseText)
                // 저장된 검색 결과 전달
                if (lastSearchResponse != null) {
                    putExtra("SEARCH_RESULT", lastSearchResponse)
                }
            }

            Log.d(TAG, "Intent 생성 완료 - 검색어: $lastSearchQuery, 노트 수: ${lastSearchResponse?.documents?.size ?: 0}")

            val pendingIntent = PendingIntent.getActivity(
                this,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Full-Screen Intent 생성
            val fullScreenIntent = PendingIntent.getActivity(
                this,
                3,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 알림 생성
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("📱 워치 검색 결과")
                .setContentText(responseText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(responseText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(fullScreenIntent, true) // 폰 화면을 자동으로 켬
                .setVibrate(longArrayOf(0, 500, 200, 500))
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

    /**
     * Full-Screen 알림 채널 생성
     */
    private fun createFullScreenNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "워치 검색 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "워치에서 보낸 검색 결과 알림"
                setBypassDnd(true)
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 워치로 응답 전송
     */
    private suspend fun sendResponseToWear(response: String) {
        try {
            Log.d(TAG, "워치로 응답 전송 시작: '$response'")

            // 연결된 워치 기기 확인
            val nodes = Wearable.getNodeClient(this)
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

                val result = Wearable.getMessageClient(this)
                    .sendMessage(node.id, WearableConstants.PATH_BACKEND_RESPONSE, data)
                    .await()

                Log.i(TAG, "✅ 워치로 응답 전송 완료!")
                Log.i(TAG, "  - 노드: ${node.displayName}")
                Log.i(TAG, "  - 경로: ${WearableConstants.PATH_BACKEND_RESPONSE}")
                Log.i(TAG, "  - 요청 ID: $result")
                Log.i(TAG, "  - 메시지: $response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 워치로 응답 전송 실패", e)
            Log.e(TAG, "에러 상세: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }
}
