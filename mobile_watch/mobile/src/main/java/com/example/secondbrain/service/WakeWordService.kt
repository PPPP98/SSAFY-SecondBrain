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
        private const val ALERT_CHANNEL_ID = "wakeword_alert_channel"
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
            val notificationManager = getSystemService(NotificationManager::class.java)

            // 1. Foreground Service용 낮은 우선순위 채널
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "웨이크워드 감지 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "헤이스비 웨이크워드를 감지합니다"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // 2. 웨이크워드 감지 알림용 높은 우선순위 채널
            // Full-Screen Intent를 사용하려면 IMPORTANCE_HIGH 이상이 필요
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "웨이크워드 감지 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "웨이크워드가 감지되었을 때 알림을 표시합니다"
                setShowBadge(true)
                enableVibration(true)
                // 잠금 화면에서도 표시
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                // 방해 금지 모드 무시 (선택적)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
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

        // Android 15에서는 백그라운드에서 액티비티를 직접 시작할 수 없음
        // Full-Screen Intent를 통해서만 가능
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            // 새 태스크로 시작하고, 기존 인스턴스가 있으면 재사용
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("wake_word_detected", true)
            putExtra("auto_opened", true) // 자동으로 열렸음을 표시
        }

        // 알림을 탭해서 열 때 사용할 Intent (자동 종료하지 않음)
        val manualIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("wake_word_detected", true)
            putExtra("auto_opened", false) // 수동으로 열렸음을 표시
        }

        // Full-Screen Intent PendingIntent 생성
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            100,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 알림 클릭 시에도 앱이 열리도록 contentIntent 설정 (수동 Intent 사용)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            101,
            manualIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Full-Screen Intent를 가진 높은 우선순위 알림 생성
        // Android 15에서는 화면이 꺼져있을 때만 Full-Screen Intent가 자동으로 앱을 열고,
        // 화면이 켜져있을 때는 알림만 표시됨
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("헤이스비 감지됨!")
            .setContentText("탭하여 열기")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Heads-up 알림으로 표시 (화면 상단에 팝업)
            .setDefaults(android.app.Notification.DEFAULT_ALL)
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