package com.example.secondbrain.communication

import android.content.Context
import com.example.secondbrain.utils.LogUtils
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
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

        // Wearable Data Layer 메시지 크기 제한 (공식 문서 권장: 100KB)
        private const val MAX_MESSAGE_SIZE = 100 * 1024 // 100KB in bytes

        // 재시도 설정
        private const val MAX_RETRY_COUNT = 2 // 최대 재시도 횟수
        private const val RETRY_DELAY_MS = 300L // 재시도 간 대기 시간
    }

    private val dataClient: DataClient = Wearable.getDataClient(context)

    /**
     * 음성 인식 결과를 모바일 앱으로 전송
     *
     * @param recognizedText 음성 인식된 텍스트
     * @return 전송 성공한 노드 수 (0이면 모두 실패)
     */
    suspend fun sendVoiceText(recognizedText: String): Int {
        return try {
            LogUtils.i(TAG, "음성 텍스트 전송 시작")

            // 입력 검증: 빈 문자열 체크
            if (recognizedText.isBlank()) {
                LogUtils.w(TAG, "빈 텍스트는 전송하지 않음")
                return 0
            }

            // 입력 검증: 메시지 크기 체크
            val data = recognizedText.toByteArray(Charsets.UTF_8)
            if (data.size > MAX_MESSAGE_SIZE) {
                LogUtils.w(TAG, "텍스트가 너무 큼: ${data.size}B (최대: ${MAX_MESSAGE_SIZE}B)")
                return 0
            }

            LogUtils.i(TAG, "전송 내용: '$recognizedText' (${data.size}B)")

            // 연결된 모바일 기기 찾기
            val nodes = Wearable.getNodeClient(context)
                .connectedNodes
                .await()

            if (nodes.isEmpty()) {
                LogUtils.w(TAG, "연결된 모바일 기기 없음")
                return 0
            }

            LogUtils.d(TAG, "연결된 노드: ${nodes.size}개")

            // DataItem으로 전송 (백그라운드에서도 동작)
            // 타임스탬프를 포함하여 매번 새로운 데이터로 인식되도록 함
            val putDataReq = PutDataMapRequest.create(WearableConstants.PATH_VOICE_TEXT).apply {
                dataMap.putString("text", recognizedText)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }

            val putDataTask = dataClient.putDataItem(putDataReq.asPutDataRequest().setUrgent())
            putDataTask.await()

            LogUtils.i(TAG, "DataItem 전송 성공: '$recognizedText'")
            1 // 성공
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

            if (nodes.isEmpty()) {
                LogUtils.d(TAG, "연결된 기기 없음")
            } else {
                LogUtils.d(TAG, "연결된 기기: ${nodes.size}개")
                nodes.forEach { node ->
                    LogUtils.d(TAG, "- ${node.displayName} (${node.id})")
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "노드 정보 조회 실패", e)
        }
    }
}