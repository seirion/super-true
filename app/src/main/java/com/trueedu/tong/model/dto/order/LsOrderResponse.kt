package com.trueedu.tong.model.dto.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * LS증권 주식 주문 (CSPAT00601) 응답.
 */
@Serializable
data class LsOrderResponse(
    @SerialName("rsp_cd") val rspCd: String = "",
    @SerialName("rsp_msg") val rspMsg: String = "",
    @SerialName("CSPAT00601OutBlock2") val output: LsOrderOutput? = null,
)

@Serializable
data class LsOrderOutput(
    @SerialName("OrdNo") val ordNo: String = "",
)
