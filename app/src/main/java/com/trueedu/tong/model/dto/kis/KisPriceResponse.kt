package com.trueedu.tong.model.dto.kis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisPriceResponse(
    val output: KisPriceDetail? = null,
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg1") val msg1: String = "",
)

@Serializable
data class KisPriceDetail(
    @SerialName("stck_prpr") val price: String = "",      // 현재가
    @SerialName("prdy_vrss") val delta: String = "",      // 전일대비
    @SerialName("prdy_ctrt") val rate: String = "",       // 등락률(%)
    @SerialName("stck_hgpr") val high: String = "",       // 고가
    @SerialName("stck_lwpr") val low: String = "",        // 저가
    @SerialName("stck_oprc") val open: String = "",       // 시가
    @SerialName("stck_clpr") val close: String = "",      // 전일종가
    @SerialName("acml_vol") val volume: String = "",      // 누적거래량
    @SerialName("acml_tr_pbmn") val tradeAmount: String = "", // 누적거래대금
)
