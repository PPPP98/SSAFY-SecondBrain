package com.example.secondbrain.data.model

import com.google.gson.annotations.SerializedName

// 노트 검색 응답 데이터 모델
data class NoteSearchResponse(
    @SerializedName("results")
    val results: List<NoteSearchResult>,

    @SerializedName("totalCount")
    val totalCount: Long,

    @SerializedName("currentPage")
    val currentPage: Int,

    @SerializedName("totalPages")
    val totalPages: Int,

    @SerializedName("pageSize")
    val pageSize: Int
)
