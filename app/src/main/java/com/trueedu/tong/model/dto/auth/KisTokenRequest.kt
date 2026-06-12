package com.trueedu.tong.model.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisTokenRequest(
    @SerialName("grant_type") val grantType: String = "client_credentials",
    @SerialName("appkey") val appKey: String,
    @SerialName("appsecret") val appSecret: String,
)

@Serializable
data class KisTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,              // 초 단위 유효기간
    @SerialName("access_token_token_expired") val expiredAt: String, // "yyyy-MM-dd HH:mm:ss"
)
