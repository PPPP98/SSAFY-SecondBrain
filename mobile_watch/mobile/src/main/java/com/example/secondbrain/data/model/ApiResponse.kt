package com.example.secondbrain.data.model

import com.google.gson.annotations.SerializedName

// 백엔드의 표준 응답 포맷 (BaseResponse)
data class ApiResponse<T>(
    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: T?
)
