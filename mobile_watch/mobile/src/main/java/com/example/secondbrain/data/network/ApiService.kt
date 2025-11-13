package com.example.secondbrain.data.network

import com.example.secondbrain.data.model.ApiResponse
import com.example.secondbrain.data.model.GoogleAuthRequest
import com.example.secondbrain.data.model.Note
import com.example.secondbrain.data.model.TokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Retrofit API 인터페이스 정의
interface ApiService {

    // Google ID Token을 사용한 인증 (모바일 전용 경로)
    @POST("/api/mobile/auth/google")
    suspend fun authenticateWithGoogle(
        @Body request: GoogleAuthRequest
    ): ApiResponse<TokenResponse>

    // 노트 목록 조회 (모바일 전용 경로)
    @GET("/api/mobile/notes")
    suspend fun getNotes(): ApiResponse<List<Note>>

    // 노트 상세 조회 (JWT 인증 필요)
    @GET("/api/notes/{noteId}")
    suspend fun getNote(@retrofit2.http.Path("noteId") noteId: Long): ApiResponse<Note>
}
