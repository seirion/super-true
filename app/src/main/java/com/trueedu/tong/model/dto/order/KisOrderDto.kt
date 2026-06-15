package com.trueedu.tong.model.dto.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * KIS 주식 현금 주문 (order-cash) 요청 본문.
 */
@Serializable
data class KisOrderRequest(
    @SerialName("CANO") val cano: String,                 // 계좌번호 앞 8자리
    @SerialName("ACNT_PRDT_CD") val acntPrdtCd: String,   // 계좌상품코드 (뒤 2자리)
    @SerialName("PDNO") val pdno: String,                 // 종목코드 (6자리)
    @SerialName("ORD_DVSN") val ordDvsn: String,          // 주문구분 00:지정가 01:시장가
    @SerialName("ORD_QTY") val ordQty: String,            // 주문수량
    @SerialName("ORD_UNPR") val ordUnpr: String,          // 주문단가 (시장가는 "0")
    @SerialName("EXCG_ID_DVSN_CD") val excgIdDvsnCd: String = "SOR", // 거래소구분 KRX/NXT/SOR
)

@Serializable
data class KisOrderResponse(
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg_cd") val msgCd: String = "",
    @SerialName("msg1") val msg1: String = "",
    val output: KisOrderOutput? = null,
)

@Serializable
data class KisOrderOutput(
    @SerialName("ODNO") val odno: String = "",       // 주문번호
    @SerialName("ORD_TMD") val ordTmd: String = "",  // 주문시각
)
