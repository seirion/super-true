package com.trueedu.tong.model.dto.kiwoom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// kt00018 응답 - 보유 종목
@Serializable
data class KiwoomBalanceResponse(
    @SerialName("acnt_evlt_remn_indv_tot") val summary: KiwoomBalanceSummary? = null,
    @SerialName("acnt_evlt_remn_indv") val holdings: List<KiwoomHolding> = emptyList(),
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String = "",
)

@Serializable
data class KiwoomBalanceSummary(
    @SerialName("tot_asse_amt") val totalAsset: String = "",         // 총자산금액
    @SerialName("scts_evlt_amt") val stockEvalAmount: String = "",   // 유가증권평가금액
    @SerialName("evlt_pfls_smtl") val profitLossTotal: String = "",  // 평가손익합계
    @SerialName("tot_buy_amt") val totalBuyAmount: String = "",      // 총매입금액
)

@Serializable
data class KiwoomHolding(
    @SerialName("stk_cd") val code: String = "",       // 종목코드
    @SerialName("stk_nm") val name: String = "",       // 종목명
    @SerialName("rmnd_qty") val quantity: String = "", // 잔고수량 (rmnd=remain)
    @SerialName("buy_uv") val avgPrice: String = "",   // 매입단가
    @SerialName("cur_prc") val currentPrice: String = "",  // 현재가
    @SerialName("evlt_amt") val evalAmount: String = "",   // 평가금액
    @SerialName("evlt_pfls_amt") val profitAmount: String = "", // 평가손익
    @SerialName("pfls_rt") val profitRate: String = "",    // 손익률
)

// kt00001 응답 - 예수금
@Serializable
data class KiwoomDepositResponse(
    @SerialName("pymn_alow_amt_entr") val deposit: String = "",  // 출금가능금액 (D+0 예수금)
    @SerialName("d1_entra") val depositD1: String = "",          // D+1 추정예수금
    @SerialName("d2_entra") val depositD2: String = "",          // D+2 추정예수금
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String = "",
)
