package com.example.secondbrain.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.secondbrain.communication.WearableConstants
import com.example.secondbrain.utils.LogUtils
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 모바일 앱에서 전송된 메시지를 수신하는 서비스
 *
 * Google Wearable Data Layer API를 사용하여
 * 모바일 앱에서 전송한 백엔드 응답을 수신하고
 * 워치에 알림(Notification)을 표시합니다.
 */
class WearWearableListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearableListener"

        // 알림 채널 ID 및 설정
        private const val NOTIFICATION_CHANNEL_ID = "backend_response_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "백엔드 응답 알림"
        private const val NOTIFICATION_ID = 1001

        // BroadcastReceiver Action
        private const val ACTION_OPEN_ON_PHONE = "com.example.secondbrain.OPEN_ON_PHONE"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        LogUtils.i(TAG, "WearWearableListenerService onCreate() 호출됨!")
        LogUtils.i(TAG, "WearableListenerService 시작 - 모바일 메시지 수신 대기")
    }

    /**
     * 모바일 앱에서 메시지를 수신했을 때 호출됨
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        LogUtils.d(TAG, "메시지 수신: ${messageEvent.path}")

        when (messageEvent.path) {
            WearableConstants.PATH_BACKEND_RESPONSE -> {
                val response = String(messageEvent.data, Charsets.UTF_8)
                LogUtils.i(TAG, "백엔드 응답 수신: '$response' (${messageEvent.data.size} bytes)")

                // TODO: UI 업데이트 또는 Notification 표시
                // 예: Notification 표시, UI 상태 업데이트 등
                handleBackendResponse(response)
            }
            WearableConstants.PATH_STATUS_REQUEST -> {
                val statusRequest = String(messageEvent.data, Charsets.UTF_8)
                LogUtils.i(TAG, "상태 요청 수신: '$statusRequest'")

                // TODO: 상태 정보 전송
                handleStatusRequest(statusRequest)
            }
            else -> {
                LogUtils.w(TAG, "알 수 없는 경로: ${messageEvent.path}")
            }
        }
    }

    /**
     * 백엔드 응답 처리
     *
     * 모바일 앱에서 받은 백엔드 응답을 워치 알림으로 표시합니다.
     * 사용자가 질문한 후 답변이 도착하면 워치에 알림이 표시됩니다.
     */
    private fun handleBackendResponse(response: String) {
        try {
            LogUtils.d(TAG, "백엔드 응답 처리: '$response'")

            // 알림 채널 생성 (Android O 이상 필수)
            createNotificationChannel()

            // 워치 알림 표시
            showNotification(response)

            LogUtils.i(TAG, "워치 알림 표시 완료")

        } catch (e: Exception) {
            LogUtils.e(TAG, "백엔드 응답 처리 실패", e)
        }
    }

    /**
     * 알림 채널 생성 (Android O 이상)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // 높은 우선순위로 즉시 알림 표시
            ).apply {
                description = "백엔드 서버로부터 받은 질문 응답 알림"
                enableVibration(true) // 진동 활성화
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 워치 알림 표시
     *
     * 백엔드 응답을 워치 알림으로 표시합니다.
     * 사용자가 "폰에서 보기" 버튼을 탭하면 모바일로 메시지를 전송합니다.
     */
    private fun showNotification(responseText: String) {
        // "폰에서 보기" 버튼을 클릭했을 때 실행할 Service Intent
        val openOnPhoneIntent = Intent(this, OpenOnPhoneService::class.java).apply {
            putExtra("response_text", responseText)
        }

        val openOnPhonePendingIntent = PendingIntent.getService(
            this,
            0,
            openOnPhoneIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 워치 알림 빌드
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("질문 응답 도착")
            .setContentText(responseText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(responseText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .addAction(
                android.R.drawable.ic_menu_view,
                "폰에서 보기",
                openOnPhonePendingIntent
            )
            .build()

        // 알림 표시
        val notificationManager = NotificationManagerCompat.from(this)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            LogUtils.i(TAG, "알림 표시 성공 - 폰에서 보기 버튼 추가됨")
        } catch (e: SecurityException) {
            LogUtils.e(TAG, "알림 권한 없음", e)
        }
    }

    /**
     * 폰에서 열기 요청을 모바일로 전송
     */
    private suspend fun sendOpenOnPhoneRequest(responseText: String) {
        try {
            LogUtils.d(TAG, "폰에서 열기 요청 전송 시작: '$responseText'")

            // 연결된 모바일 기기 확인
            val nodes = Wearable.getNodeClient(applicationContext)
                .connectedNodes
                .await()

            if (nodes.isEmpty()) {
                LogUtils.w(TAG, "폰에서 열기 요청 실패: 연결된 기기 없음")
                return
            }

            val data = responseText.toByteArray(Charsets.UTF_8)

            // 연결된 모든 모바일 기기에 요청 전송
            for (node in nodes) {
                Wearable.getMessageClient(applicationContext)
                    .sendMessage(node.id, WearableConstants.PATH_OPEN_ON_PHONE, data)
                    .await()

                LogUtils.i(TAG, "✅ 폰에서 열기 요청 전송 완료!")
                LogUtils.i(TAG, "  - 노드: ${node.displayName}")
                LogUtils.i(TAG, "  - 경로: ${WearableConstants.PATH_OPEN_ON_PHONE}")
                LogUtils.i(TAG, "  - 메시지: $responseText")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "❌ 폰에서 열기 요청 전송 실패", e)
        }
    }

    /**
     * 상태 요청 처리
     */
    private fun handleStatusRequest(request: String) {
        try {
            LogUtils.d(TAG, "상태 요청 처리: '$request'")

            // TODO: 워치 상태 정보를 모바일로 전송
            // 예: 배터리 수준, 센서 상태, 앱 상태 등

        } catch (e: Exception) {
            LogUtils.e(TAG, "상태 요청 처리 실패", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "WearableListenerService 종료")
    }
}

/**
 * "폰에서 보기" 버튼 클릭 시 실행되는 Service
 */
class OpenOnPhoneService : android.app.Service() {
    companion object {
        private const val TAG = "OpenOnPhoneService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.i(TAG, "OpenOnPhoneService 시작됨")

        val responseText = intent?.getStringExtra("response_text")

        if (responseText != null) {
            LogUtils.i(TAG, "폰에서 보기 버튼 클릭: '$responseText'")

            // 코루틴으로 모바일에 메시지 전송 + RemoteIntent로 Activity 직접 실행
            scope.launch {
                try {
                    val nodes = Wearable.getNodeClient(applicationContext)
                        .connectedNodes
                        .await()

                    if (nodes.isEmpty()) {
                        LogUtils.w(TAG, "연결된 모바일 기기 없음")
                        stopSelf(startId)
                        return@launch
                    }

                    // Wearable Message API로 모바일에 메시지 전송
                    val data = responseText.toByteArray(Charsets.UTF_8)
                    for (node in nodes) {
                        try {
                            Wearable.getMessageClient(applicationContext)
                                .sendMessage(node.id, WearableConstants.PATH_OPEN_ON_PHONE, data)
                                .await()

                            LogUtils.i(TAG, "✅ 폰에서 열기 메시지 전송 완료!")
                            LogUtils.i(TAG, "  - 노드: ${node.displayName}")
                            LogUtils.i(TAG, "  - 메시지: $responseText")
                        } catch (e: Exception) {
                            LogUtils.e(TAG, "❌ 메시지 전송 실패", e)
                        }
                    }
                } catch (e: Exception) {
                    LogUtils.e(TAG, "❌ 폰에서 열기 요청 실패", e)
                } finally {
                    stopSelf(startId)
                }
            }
        } else {
            LogUtils.w(TAG, "response_text가 null입니다")
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "OpenOnPhoneService 종료")
    }
}
