package com.trueedu.tong.model.dto.stockinfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * KIS(한국투자증권) inquire-price (FHKST01010100) 응답.
 * GET /uapi/domestic-stock/v1/quotations/inquire-price
 */
@Serializable
data class KisStockInfoResponse(
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg1") val msg1: String = "",
    val output: KisStockInfoOutput? = null,
)

@Serializable
data class KisStockInfoOutput(
    @SerialName("hts_kor_isnm") val name: String = "",
    @SerialName("stck_prpr") val currentPrice: String = "",
    @SerialName("prdy_vrss") val delta: String = "",
    @SerialName("prdy_ctrt") val rate: String = "",
    @SerialName("stck_oprc") val open: String = "",
    @SerialName("stck_hgpr") val high: String = "",
    @SerialName("stck_lwpr") val low: String = "",
    @SerialName("acml_vol") val volume: String = "",
    @SerialName("hts_avls") val marketCap: String = "",   // HTS 시가총액 (억원)
    @SerialName("per") val per: String = "",
    @SerialName("pbr") val pbr: String = "",
    @SerialName("eps") val eps: String = "",
    @SerialName("bps") val bps: String = "",
    @SerialName("lstn_stcn") val listedShares: String = "", // 상장주수
    @SerialName("stac_month") val settlementMonth: String = "", // 결산월
    @SerialName("w52_hgpr") val week52High: String = "",
    @SerialName("w52_lwpr") val week52Low: String = "",
    @SerialName("stck_mxpr") val upperLimit: String = "",  // 상한가
    @SerialName("stck_llam") val lowerLimit: String = "",  // 하한가
    @SerialName("hts_frgn_ehrt") val foreignExhaustionRate: String = "", // 외국인 소진률
)
