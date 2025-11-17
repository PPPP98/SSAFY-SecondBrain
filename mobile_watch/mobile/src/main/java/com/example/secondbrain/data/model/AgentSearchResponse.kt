package com.example.secondbrain.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// AI 에이전트 검색 응답 모델
data class AgentSearchResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("response")
    val response: String,

    @SerializedName("documents")
    val documents: List<AgentNoteResult>?
) : Serializable

// 에이전트가 추천하는 개별 노트 데이터
data class AgentNoteResult(
    @SerializedName("note_id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("similarity_score")
    val similarityScore: Double? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Serializable
