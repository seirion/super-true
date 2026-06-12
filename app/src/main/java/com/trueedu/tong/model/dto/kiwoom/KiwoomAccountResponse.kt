package com.trueedu.tong.model.dto.kiwoom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// kt00018 응답 - 요약 정보는 최상위, 종목 목록은 acnt_evlt_remn_indv_tot 배열
@Serializable
data class KiwoomBalanceResponse(
    @SerialName("tot_pur_amt") val totalBuyAmount: String = "",      // 총매입금액
    @SerialName("tot_evlt_amt") val totalEvalAmount: String = "",    // 총평가금액
    @SerialName("tot_evlt_pl") val totalProfitLoss: String = "",     // 총평가손익
    @SerialName("tot_prft_rt") val totalProfitRate: String = "",     // 총수익률(%)
    @SerialName("prsm_dpst_aset_amt") val estimatedAsset: String = "", // 추정예탁자산
    @SerialName("acnt_evlt_remn_indv_tot") val holdings: List<KiwoomHolding> = emptyList(),
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String = "",
)

@Serializable
data class KiwoomHolding(
    @SerialName("stk_cd") val code: String = "",          // 종목코드
    @SerialName("stk_nm") val name: String = "",          // 종목명
    @SerialName("rmnd_qty") val quantity: String = "",    // 잔고수량
    @SerialName("pur_pric") val avgPrice: String = "",    // 매입단가 (평단가)
    @SerialName("cur_prc") val currentPrice: String = "", // 현재가
    @SerialName("evlt_amt") val evalAmount: String = "",  // 평가금액
    @SerialName("evltv_prft") val profitAmount: String = "", // 평가손익
    @SerialName("prft_rt") val profitRate: String = "",   // 수익률(%)
)

// kt00001 응답 - 예수금
@Serializable
data class KiwoomDepositResponse(
    @SerialName("pymn_alow_amt") val deposit: String = "",  // 출금가능금액 (D+0 예수금)
    @SerialName("d1_entra") val depositD1: String = "",     // D+1 추정예수금
    @SerialName("d2_entra") val depositD2: String = "",     // D+2 추정예수금
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String = "",
)
