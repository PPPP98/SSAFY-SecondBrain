package com.example.secondbrain.data.network

import com.example.secondbrain.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Retrofit 클라이언트 싱글톤 객체
object RetrofitClient {

    // 백엔드 서버 URL (빌드 타입에 따라 자동 선택)
    // Debug: http://10.0.2.2:8080/ (localhost 직접 연결)
    // Release: https://api.brainsecond.site/ (Traefik 통과)
    private val BASE_URL = BuildConfig.BASE_URL

    // FastAPI 서버 URL (Knowledge Graph Service)
    // 배포 서버 사용: Traefik /ai 경로로 프록시
    private val FASTAPI_BASE_URL = "https://api.brainsecond.site/ai/"

    // HTTP 로깅 인터셉터 (디버그용)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 응답 본문 로깅 인터셉터
    private val responseLoggingInterceptor = okhttp3.Interceptor { chain ->
        val response = chain.proceed(chain.request())
        val responseBody = response.body
        val bodyString = responseBody?.string() ?: ""

        android.util.Log.e("RetrofitClient", "=== 응답 상태 코드: ${response.code}")
        android.util.Log.e("RetrofitClient", "=== 응답 본문: $bodyString")

        // 응답 본문을 다시 만들어야 함 (한 번 읽으면 소진됨)
        val newResponseBody = okhttp3.ResponseBody.create(
            responseBody?.contentType(),
            bodyString
        )
        response.newBuilder().body(newResponseBody).build()
    }

    // OkHttpClient 생성 함수 (토큰 제공자를 받음)
    private fun createOkHttpClient(tokenProvider: suspend () -> String?): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(responseLoggingInterceptor) // 응답 본문 로깅 (가장 먼저)
            .addInterceptor(loggingInterceptor) // HTTP 로그 출력
            .addInterceptor(AuthInterceptor(tokenProvider)) // JWT 자동 추가
            .connectTimeout(30, TimeUnit.SECONDS) // 연결 타임아웃
            .readTimeout(30, TimeUnit.SECONDS) // 읽기 타임아웃
            .writeTimeout(30, TimeUnit.SECONDS) // 쓰기 타임아웃
            .build()
    }

    // Retrofit 인스턴스 생성 함수
    fun createApiService(tokenProvider: suspend () -> String?): ApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient(tokenProvider))
            .addConverterFactory(GsonConverterFactory.create()) // JSON 변환
            .build()
            .create(ApiService::class.java)
    }

    // FastAPI 서비스 생성 함수
    fun createFastApiService(tokenProvider: suspend () -> String?): FastApiService {
        // FastAPI는 포트 8000 (Debug) 또는 Traefik /ai 경로 (Release)
        return Retrofit.Builder()
            .baseUrl(FASTAPI_BASE_URL)
            .client(createOkHttpClient(tokenProvider))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FastApiService::class.java)
    }
}
