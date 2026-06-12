package com.trueedu.tong.model.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisWsRequest(
    val header: KisWsHeader,
    val body: KisWsBody,
)

@Serializable
data class KisWsHeader(
    @SerialName("approval_key") val approvalKey: String,
    @SerialName("custtype") val customerType: String = "P",
    @SerialName("tr_type") val transactionType: String,  // "1"=구독, "2"=해제
    @SerialName("content-type") val contentType: String = "utf-8",
)

@Serializable
data class KisWsBody(
    val input: KisWsBodyInput,
)

@Serializable
data class KisWsBodyInput(
    @SerialName("tr_id") val transactionId: String,   // "H0STCNT0" 실시간 체결
    @SerialName("tr_key") val transactionKey: String, // 종목코드
)
