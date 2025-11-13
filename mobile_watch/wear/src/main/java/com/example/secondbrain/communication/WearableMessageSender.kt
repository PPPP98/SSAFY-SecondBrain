package com.example.secondbrain.communication

import android.content.Context
import com.example.secondbrain.utils.LogUtils
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * Wear OS에서 모바일 앱으로 메시지를 전송하는 클래스
 *
 * Google Wearable Data Layer API를 사용하여
 * 워치 앱과 모바일 앱 간의 양방향 통신을 지원합니다.
 */
class WearableMessageSender(private val context: Context) {

    companion object {
        private const val TAG = "WearableMessageSender"

        // 메시지 경로 (모바일 앱과 동일한 경로를 사용해야 함)
        const val PATH_VOICE_TEXT = "/voice_text"
        const val PATH_VOICE_REQUEST = "/voice_request"

        // Wearable Data Layer 메시지 크기 제한 (공식 문서 권장: 100KB)
        private const val MAX_MESSAGE_SIZE = 100 * 1024 // 100KB in bytes
    }

    private val messageClient: MessageClient = Wearable.getMessageClient(context)

    /**
     * 음성 인식 결과를 모바일 앱으로 전송
     *
     * @param recognizedText 음성 인식된 텍스트
     * @return 전송 성공한 노드 수 (0이면 모두 실패)
     */
    suspend fun sendVoiceText(recognizedText: String): Int {
        return try {
            // 입력 검증: 빈 문자열 체크
            if (recognizedText.isBlank()) {
                LogUtils.w(TAG, "빈 텍스트는 전송하지 않음")
                return 0
            }

            // 입력 검증: 메시지 크기 체크
            val data = recognizedText.toByteArray(Charsets.UTF_8)
            if (data.size > MAX_MESSAGE_SIZE) {
                LogUtils.w(TAG, "텍스트가 너무 큼: ${data.size} bytes (최대: $MAX_MESSAGE_SIZE bytes)")
                return 0
            }

            LogUtils.d(TAG, "음성 텍스트 전송 시작: '$recognizedText' (${data.size} bytes)")

            // 연결된 노드(모바일 기기) 찾기
            val nodes = Wearable.getNodeClient(context)
                .connectedNodes
                .await()

            if (nodes.isEmpty()) {
                LogUtils.w(TAG, "연결된 모바일 기기 없음")
                return 0
            }

            // 모든 연결된 노드에 메시지 전송 및 성공 카운트 추적
            var successCount = 0
            for (node in nodes) {
                try {
                    messageClient.sendMessage(
                        node.id,
                        PATH_VOICE_TEXT,
                        data
                    ).await()

                    LogUtils.i(TAG, "전송 성공: ${node.displayName} (${node.id})")
                    successCount++
                } catch (e: Exception) {
                    LogUtils.e(TAG, "노드 전송 실패: ${node.displayName}", e)
                }
            }

            LogUtils.i(TAG, "전송 결과: ${successCount}/${nodes.size}")
            successCount
        } catch (e: Exception) {
            LogUtils.e(TAG, "메시지 전송 실패", e)
            0
        }
    }

    /**
     * 연결된 모바일 기기 확인
     *
     * @return 연결된 기기가 있으면 true
     */
    suspend fun isConnectedToMobile(): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context)
                .connectedNodes
                .await()

            val isConnected = nodes.isNotEmpty()
            LogUtils.d(TAG, "연결 상태: $isConnected (${nodes.size}개 기기)")
            isConnected
        } catch (e: Exception) {
            LogUtils.e(TAG, "연결 확인 실패", e)
            false
        }
    }

    /**
     * 연결된 모든 노드 정보 로깅 (디버깅용)
     */
    suspend fun logConnectedNodes() {
        try {
            val nodes = Wearable.getNodeClient(context)
                .connectedNodes
                .await()

            LogUtils.d(TAG, "=== 연결된 기기 목록 ===")
            if (nodes.isEmpty()) {
                LogUtils.d(TAG, "연결된 기기 없음")
            } else {
                nodes.forEach { node ->
                    LogUtils.d(TAG, "- ${node.displayName} (ID: ${node.id}, Nearby: ${node.isNearby})")
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "노드 정보 조회 실패", e)
        }
    }
}