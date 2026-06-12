package com.trueedu.tong.model.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisApprovalKeyRequest(
    @SerialName("grant_type") val grantType: String = "client_credentials",
    @SerialName("appkey") val appKey: String,
    @SerialName("secretkey") val secretKey: String,  // appsecret 아닌 secretkey
)

@Serializable
data class KisApprovalKeyResponse(
    @SerialName("approval_key") val approvalKey: String,
)
