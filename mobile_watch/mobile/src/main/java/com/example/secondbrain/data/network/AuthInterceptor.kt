package com.example.secondbrain.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

// JWT 토큰을 자동으로 Authorization 헤더에 추가하는 인터셉터
class AuthInterceptor(
    private val tokenProvider: suspend () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 토큰이 필요 없는 엔드포인트는 그대로 진행
        if (originalRequest.url.encodedPath.contains("/auth/")) {
            android.util.Log.d("AuthInterceptor", "인증 불필요 경로: ${originalRequest.url.encodedPath}")
            return chain.proceed(originalRequest)
        }

        // 토큰 가져오기 (suspend 함수를 동기적으로 실행)
        val token = runBlocking { tokenProvider() }

        // 토큰이 없으면 그대로 진행
        if (token.isNullOrEmpty()) {
            android.util.Log.e("AuthInterceptor", "⚠️ 토큰 없음! 경로: ${originalRequest.url.encodedPath}")
            return chain.proceed(originalRequest)
        }

        // Authorization 헤더에 Bearer 토큰 추가
        android.util.Log.d("AuthInterceptor", "✅ 토큰 추가됨: ${token.take(20)}... (경로: ${originalRequest.url.encodedPath})")
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
