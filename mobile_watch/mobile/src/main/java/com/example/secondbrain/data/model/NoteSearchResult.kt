package com.example.secondbrain.data.model

import com.google.gson.annotations.SerializedName

// 검색 결과 개별 노트 데이터 모델
data class NoteSearchResult(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String,

    @SerializedName("remindCount")
    val remindCount: Int?
)
