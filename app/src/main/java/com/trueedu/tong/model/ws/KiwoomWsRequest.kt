package com.trueedu.tong.model.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 키움 실시간 WebSocket 구독/해제 요청.
 */
@Serializable
data class KiwoomWsRequest(
    val header: KiwoomWsHeader,
    val body: KiwoomWsBody,
)

@Serializable
data class KiwoomWsHeader(
    val token: String,
    @SerialName("content-type") val contentType: String = "utf-8",
)

@Serializable
data class KiwoomWsBody(
    @SerialName("tr_type") val transactionType: String,  // "1"=구독, "2"=해제
    @SerialName("tr_cd") val transactionCode: String,     // "OPK20001" 업종현재가
    @SerialName("tr_key") val transactionKey: String,     // 종목코드
)
