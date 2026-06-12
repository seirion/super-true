package com.trueedu.tong.model.dto.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// KIS 주문 응답
@Serializable
data class KisOrderResponse(
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg_cd") val msgCd: String = "",
    @SerialName("msg1") val msg1: String = "",
    val output: KisOrderOutput? = null,
)

@Serializable
data class KisOrderOutput(
    @SerialName("KRX_FWDG_ORD_ORGNO") val orgNo: String = "",
    @SerialName("ODNO") val ordNo: String = "",
    @SerialName("ORD_TMD") val ordTime: String = "",
)

// 키움 주문 응답
@Serializable
data class KiwoomOrderResponse(
    @SerialName("return_code") val returnCode: Int = -1,
    @SerialName("return_msg") val returnMsg: String = "",
    @SerialName("ord_no") val ordNo: String = "",
)

// LS 주문 응답
@Serializable
data class LsOrderResponse(
    @SerialName("rsp_cd") val rspCd: String = "",
    @SerialName("rsp_msg") val rspMsg: String = "",
    @SerialName("CSPAT00601OutBlock1") val output: LsOrderOutput? = null,
)

@Serializable
data class LsOrderOutput(
    @SerialName("OrdNo") val ordNo: String = "",
)
