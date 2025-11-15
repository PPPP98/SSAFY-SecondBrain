package com.example.secondbrain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.secondbrain.MainActivity
import com.example.secondbrain.R
import com.example.secondbrain.wakeword.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 백그라운드에서 웨이크워드를 감지하는 Foreground Service
 */
class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"
        private const val CHANNEL_ID = "wakeword_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val COOLDOWN_PERIOD = 5000L // 5초 쿨다운
    }

    private lateinit var wakeWordDetector: WakeWordDetector
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastDetectionTime = 0L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "서비스 생성됨")

        // 웨이크워드 감지기 초기화
        wakeWordDetector = WakeWordDetector(this)

        // 웨이크워드 감지 상태 관찰
        serviceScope.launch {
            wakeWordDetector.wakeWordDetected.collect { detected ->
                if (detected) {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastDetection = currentTime - lastDetectionTime

                    // 쿨다운 기간 확인
                    if (timeSinceLastDetection >= COOLDOWN_PERIOD) {
                        Log.d(TAG, "웨이크워드 감지됨! 앱 실행")
                        lastDetectionTime = currentTime
                        onWakeWordDetected()
                    } else {
                        Log.d(TAG, "웨이크워드 감지 무시 (쿨다운: ${COOLDOWN_PERIOD - timeSinceLastDetection}ms 남음)")
                    }
                }
            }
        }

        // Foreground 서비스로 시작
        startForeground()

        // 웨이크워드 감지 시작
        wakeWordDetector.startListening()
    }

    private fun startForeground() {
        createNotificationChannel()

        val notification = createNotification("웨이크워드 감지 중...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "웨이크워드 감지 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "헤이스비 웨이크워드를 감지합니다"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("헤이스비 대기 중")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun onWakeWordDetected() {
        Log.i(TAG, "웨이크워드 처리 시작")

        // 1. SearchActivity 직접 시작 (백그라운드에서 foreground로 가져오기)
        val activityIntent = Intent(this, com.example.secondbrain.ui.search.SearchActivity::class.java).apply {
            // 새 태스크로 시작하고, 기존 인스턴스가 있으면 재사용
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("auto_start_stt", true)  // STT 자동 시작 플래그
        }

        // 액티비티 직접 시작
        try {
            startActivity(activityIntent)
            Log.i(TAG, "✅ SearchActivity 시작 성공 (STT 자동 시작)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 액티비티 시작 실패", e)
        }

        // 2. Full-Screen Intent 알림도 함께 표시 (화면 꺼져있을 때를 위해)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            100,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("헤이스비 감지됨!")
            .setContentText("웨이크워드가 감지되었습니다")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)

        // 3초 후 알림 원래대로
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val normalNotification = createNotification("웨이크워드 감지 중...")
            notificationManager.notify(NOTIFICATION_ID, normalNotification)
            // Full-Screen 알림 제거
            notificationManager.cancel(NOTIFICATION_ID + 1)
        }, 3000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "서비스 시작 명령 수신")
        return START_STICKY // 시스템이 종료해도 재시작
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "서비스 종료됨")
        wakeWordDetector.stopListening()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}