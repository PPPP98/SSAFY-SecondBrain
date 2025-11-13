package com.example.secondbrain.data.model

import com.google.gson.annotations.SerializedName

// 백엔드에서 반환하는 JWT 토큰 응답 모델
data class TokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("tokenType")
    val tokenType: String,

    @SerializedName("expiresIn")
    val expiresIn: Long
)
