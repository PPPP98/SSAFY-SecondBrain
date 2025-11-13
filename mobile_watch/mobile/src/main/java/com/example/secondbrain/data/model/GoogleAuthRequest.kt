package com.example.secondbrain.data.model

import com.google.gson.annotations.SerializedName

// Google ID Token을 백엔드로 전송하기 위한 요청 모델
data class GoogleAuthRequest(
    @SerializedName("idToken")
    val idToken: String
)
