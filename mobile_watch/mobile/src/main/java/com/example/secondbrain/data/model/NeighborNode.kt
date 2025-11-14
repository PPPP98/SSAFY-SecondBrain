package com.example.secondbrain.data.model

import com.google.gson.annotations.SerializedName

// FastAPI 이웃 노트 응답 데이터 모델
data class NeighborNodeResponse(
    @SerializedName("center_note_id")
    val centerNoteId: Long,

    @SerializedName("neighbors")
    val neighbors: List<NeighborNode>
)

// 이웃 노트 정보
data class NeighborNode(
    @SerializedName("center_id")
    val centerId: Long,

    @SerializedName("center_title")
    val centerTitle: String,

    @SerializedName("neighbor_id")
    val neighborId: Long,

    @SerializedName("neighbor_title")
    val neighborTitle: String,

    @SerializedName("distance")
    val distance: Int
)
