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
import com.example.secondbrain.presentation.MainActivity
import com.example.secondbrain.utils.LogUtils
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * 모바일 앱에서 전송된 메시지를 수신하는 서비스
 *
 * Google Wearable Data Layer API를 사용하여
 * 모바일 앱에서 전송한 백엔드 응답을 수신하고
 * 워치에 알림(Notification)을 표시합니다.
 */
class WearableListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearableListener"

        // 알림 채널 ID 및 설정
        private const val NOTIFICATION_CHANNEL_ID = "backend_response_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "백엔드 응답 알림"
        private const val NOTIFICATION_ID = 1001
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
     * 사용자가 알림을 탭하면 앱이 열립니다.
     */
    private fun showNotification(responseText: String) {
        // 알림을 탭했을 때 앱을 여는 인텐트
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 워치 알림 빌드
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: 앱 아이콘으로 변경
            .setContentTitle("질문 응답 도착") // 알림 제목
            .setContentText(responseText) // 알림 내용 (백엔드 응답)
            .setStyle(NotificationCompat.BigTextStyle().bigText(responseText)) // 긴 텍스트 표시
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 높은 우선순위
            .setAutoCancel(true) // 탭하면 자동으로 알림 제거
            .setContentIntent(pendingIntent) // 탭 시 실행할 인텐트
            .setVibrate(longArrayOf(0, 200, 100, 200)) // 진동 패턴: 대기-진동-대기-진동
            .build()

        // 알림 표시
        val notificationManager = NotificationManagerCompat.from(this)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            LogUtils.i(TAG, "알림 표시 성공")
        } catch (e: SecurityException) {
            LogUtils.e(TAG, "알림 권한 없음", e)
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
