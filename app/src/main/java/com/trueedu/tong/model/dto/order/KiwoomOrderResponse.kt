package com.trueedu.tong.model.dto.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 키움증권 주식 주문 (kt10000 매수 / kt10001 매도) 응답.
 */
@Serializable
data class KiwoomOrderResponse(
    @SerialName("return_code") val returnCode: Int = -1,
    @SerialName("return_msg") val returnMsg: String = "",
    @SerialName("ord_no") val ordNo: String = "",
)
