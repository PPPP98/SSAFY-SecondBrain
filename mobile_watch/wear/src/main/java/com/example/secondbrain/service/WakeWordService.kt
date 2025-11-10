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
import com.example.secondbrain.presentation.MainActivity
import com.example.secondbrain.wakeword.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 백그라운드에서 웨이크워드를 감지하는 Foreground Service (Wear OS)
 */
class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"
        private const val CHANNEL_ID = "wakeword_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var wakeWordDetector: WakeWordDetector
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "워치 서비스 생성됨")

        // 웨이크워드 감지기 초기화
        wakeWordDetector = WakeWordDetector(this)

        // 웨이크워드 감지 상태 관찰
        serviceScope.launch {
            wakeWordDetector.wakeWordDetected.collect { detected ->
                if (detected) {
                    Log.d(TAG, "웨이크워드 감지됨! 앱 실행")
                    onWakeWordDetected()
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
                "웨이크워드 감지",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "헤이스비 감지 중"
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
            .setContentTitle("헤이스비")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun onWakeWordDetected() {
        // 알림 업데이트
        val notification = createNotification("감지됨!")
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)

        // 앱 실행
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("wake_word_detected", true)
        }
        startActivity(intent)

        // 짧은 딜레이 후 알림 원래대로
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val normalNotification = createNotification("웨이크워드 감지 중...")
            notificationManager.notify(NOTIFICATION_ID, normalNotification)
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