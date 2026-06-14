package com.trueedu.tong.model.dto.candle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * KIS(한국투자증권) FHKST03010100 (주식현재가 일자별) 응답.
 * GET /uapi/domestic-stock/v1/quotations/inquire-daily-price
 */
@Serializable
data class KisCandleResponse(
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg1") val msg: String = "",
    @SerialName("output2") val candles: List<KisCandleItem> = emptyList(),
)

@Serializable
data class KisCandleItem(
    @SerialName("stck_bsop_date") val date: String = "",
    @SerialName("stck_oprc") val open: String = "",
    @SerialName("stck_hgpr") val high: String = "",
    @SerialName("stck_lwpr") val low: String = "",
    @SerialName("stck_clpr") val close: String = "",
    @SerialName("acml_vol") val volume: String = "",
)
