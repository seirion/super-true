package com.trueedu.tong.model.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KiwoomTokenRequest(
    @SerialName("grant_type") val grantType: String = "client_credentials",
    @SerialName("appkey") val appKey: String,
    @SerialName("secretkey") val secretKey: String,  // 키움은 "secretkey"
)

@Serializable
data class KiwoomTokenResponse(
    @SerialName("token") val accessToken: String,          // 키움은 "token" 필드
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_dt") val expiresAt: String,       // "yyyyMMddHHmmss" 형식
    @SerialName("return_code") val returnCode: Int,
    @SerialName("return_msg") val returnMsg: String,
)
