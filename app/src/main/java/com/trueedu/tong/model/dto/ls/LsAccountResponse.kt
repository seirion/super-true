package com.trueedu.tong.model.dto.ls

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// t0424 응답
@Serializable
data class LsBalanceResponse(
    @SerialName("t0424OutBlock") val summary: LsBalanceSummary? = null,
    @SerialName("t0424OutBlock1") val holdings: List<LsHolding> = emptyList(),
    @SerialName("rsp_cd") val rspCd: String = "",
    @SerialName("rsp_msg") val rspMsg: String = "",
)

@Serializable
data class LsBalanceSummary(
    @SerialName("sunamt") val deposit: Long = 0L,         // 예수금 (D+0)
    @SerialName("dtsunik") val profitTotal: Long = 0L,    // 평가손익합계
    @SerialName("mnyuse") val usedCash: Long = 0L,        // 사용현금
    @SerialName("tappamt") val totalEvalAmount: Long = 0L, // 총평가금액
    @SerialName("tdtamt") val totalBuyAmount: Long = 0L,   // 총매입금액
)

@Serializable
data class LsHolding(
    @SerialName("expcode") val code: String = "",         // 종목코드
    @SerialName("hname") val name: String = "",           // 종목명
    @SerialName("janqty") val quantity: Long = 0L,        // 잔고수량
    @SerialName("price") val avgPrice: Long = 0L,         // 평단가 (매입단가)
    @SerialName("appamt") val evalAmount: Long = 0L,      // 평가금액
    @SerialName("dtsunik") val profitAmount: Long = 0L,   // 손익금액
    @SerialName("sunikrt") val profitRate: Double = 0.0,  // 손익률
)

// CSPAQ12200 응답
@Serializable
data class LsDepositResponse(
    @SerialName("CSPAQ12200OutBlock2") val deposit: LsDepositDetail? = null,
    @SerialName("rsp_cd") val rspCd: String = "",
    @SerialName("rsp_msg") val rspMsg: String = "",
)

@Serializable
data class LsDepositDetail(
    @SerialName("MnyOrdAbleAmt") val depositD0: Long = 0L,   // 주문가능금액 (D+0)
    @SerialName("D1Amt") val depositD1: Long = 0L,            // D+1 예수금
    @SerialName("D2Amt") val depositD2: Long = 0L,            // D+2 예수금
    @SerialName("EvalAmt") val totalEvalAmount: Long = 0L,    // 총평가금액
    @SerialName("PchsAmt") val totalBuyAmount: Long = 0L,     // 총매입금액
)
