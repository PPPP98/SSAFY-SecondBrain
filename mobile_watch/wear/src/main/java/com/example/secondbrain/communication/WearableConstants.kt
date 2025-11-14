package com.example.secondbrain.communication

/**
 * Wear OS와 Mobile 앱 간 통신에 사용되는 공통 상수
 *
 * 워치와 모바일 양쪽에서 동일한 경로를 사용해야 통신이 가능합니다.
 */
object WearableConstants {
    /**
     * Wear → Mobile: 음성 인식된 텍스트 전송
     */
    const val PATH_VOICE_TEXT = "/voice_text"

    /**
     * Wear → Mobile: 음성 요청
     */
    const val PATH_VOICE_REQUEST = "/voice_request"

    /**
     * Mobile → Wear: 백엔드 응답 전송
     */
    const val PATH_BACKEND_RESPONSE = "/backend_response"

    /**
     * 상태 요청
     */
    const val PATH_STATUS_REQUEST = "/status_request"
}
