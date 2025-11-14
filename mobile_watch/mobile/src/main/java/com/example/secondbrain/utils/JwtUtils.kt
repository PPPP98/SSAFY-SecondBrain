package com.example.secondbrain.utils

import android.util.Base64
import org.json.JSONObject

// JWT 토큰 파싱 유틸리티
object JwtUtils {

    /**
     * JWT 토큰에서 userId 추출
     * @param token JWT 토큰 (Bearer 제외)
     * @return userId (Long) 또는 null
     */
    fun getUserIdFromToken(token: String?): Long? {
        if (token.isNullOrEmpty()) {
            return null
        }

        try {
            // JWT는 "header.payload.signature" 형식
            val parts = token.split(".")
            if (parts.size != 3) {
                android.util.Log.e("JwtUtils", "Invalid JWT format")
                return null
            }

            // payload 부분 (두 번째 파트) 디코딩
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            val decodedString = String(decodedBytes, Charsets.UTF_8)

            // JSON 파싱
            val jsonObject = JSONObject(decodedString)

            // userId 추출 (필드명은 백엔드에 따라 다를 수 있음)
            // 일반적으로 "sub", "userId", "id" 등의 필드명 사용
            return when {
                jsonObject.has("userId") -> jsonObject.getLong("userId")
                jsonObject.has("id") -> jsonObject.getLong("id")
                jsonObject.has("sub") -> {
                    val sub = jsonObject.getString("sub")
                    // sub가 숫자 문자열인 경우 Long으로 변환
                    sub.toLongOrNull()
                }
                else -> {
                    android.util.Log.e("JwtUtils", "No userId field found in token")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("JwtUtils", "Failed to parse JWT token", e)
            return null
        }
    }

    /**
     * JWT 토큰의 payload 전체를 JSON으로 반환 (디버깅용)
     */
    fun getPayloadAsJson(token: String?): String? {
        if (token.isNullOrEmpty()) {
            return null
        }

        try {
            val parts = token.split(".")
            if (parts.size != 3) {
                return null
            }

            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            return String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("JwtUtils", "Failed to decode JWT payload", e)
            return null
        }
    }
}
