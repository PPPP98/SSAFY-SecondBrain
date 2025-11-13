package com.example.secondbrain.data.model

import com.google.gson.annotations.SerializedName

// 노트 데이터 모델 (백엔드 API 응답과 매핑)
data class Note(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)
