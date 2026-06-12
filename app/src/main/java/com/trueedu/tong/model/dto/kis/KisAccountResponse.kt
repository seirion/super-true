package com.trueedu.tong.model.dto.kis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisAccountResponse(
    @SerialName("output1") val holdings: List<KisHolding>,
    @SerialName("output2") val summary: List<KisAccountDetail>,
    @SerialName("rt_cd") val rtCd: String,
    @SerialName("msg_cd") val msgCd: String,
    @SerialName("msg1") val msg1: String,
    @SerialName("ctx_area_fk100") val fk100: String = "",
    @SerialName("ctx_area_nk100") val nk100: String = "",
)

@Serializable
data class KisHolding(
    @SerialName("pdno") val code: String,
    @SerialName("prdt_name") val name: String,
    @SerialName("hldg_qty") val holdingQty: String,
    @SerialName("pchs_avg_pric") val avgPrice: String,
    @SerialName("prpr") val currentPrice: String,
    @SerialName("evlu_amt") val evaluationAmount: String,
    @SerialName("evlu_pfls_amt") val profitLossAmount: String,
    @SerialName("evlu_pfls_rt") val profitLossRate: String,
    @SerialName("fltt_rt") val priceChangeRate: String = "",
    @SerialName("bfdy_cprs_icdc") val priceChange: String = "",
)

@Serializable
data class KisAccountDetail(
    @SerialName("dnca_tot_amt") val deposit: String,          // 예수금 D+0
    @SerialName("nxdy_excc_amt") val depositD1: String,       // D+1
    @SerialName("prvs_rcdl_excc_amt") val depositD2: String,  // D+2
    @SerialName("tot_evlu_amt") val totalAsset: String,       // 총평가금액
    @SerialName("pchs_amt_smtl_amt") val purchaseTotalAmount: String,   // 매입금액합계
    @SerialName("evlu_amt_smtl_amt") val evaluationTotalAmount: String, // 평가금액합계
    @SerialName("evlu_pfls_smtl_amt") val profitLossTotalAmount: String, // 평가손익합계
    @SerialName("asst_icdc_amt") val assetChangeAmount: String = "",
)
