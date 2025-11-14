package com.example.secondbrain.data.network

import com.example.secondbrain.data.model.NeighborNodeResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// FastAPI 인터페이스 정의
interface FastApiService {

    // 노트의 1depth 연결 노트 조회
    @GET("api/v1/graph/neighbors/{note_id}")
    suspend fun getNeighborNodes(
        @Path("note_id") noteId: Long,
        @Query("depth") depth: Int = 1,
        @Header("X-User-ID") userId: Long
    ): NeighborNodeResponse
}
