package com.trueedu.tong.model.dto.candle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 키움증권 ka10081 (주식 일봉차트 조회) 응답.
 * POST /api/dostk/chart
 */
@Serializable
data class KiwoomCandleResponse(
    @SerialName("stk_cd") val code: String = "",
    @SerialName("output") val candles: List<KiwoomCandleItem> = emptyList(),
)

@Serializable
data class KiwoomCandleItem(
    @SerialName("dt") val date: String = "",          // YYYYMMDD
    @SerialName("opnprc") val open: String = "",
    @SerialName("hgprc") val high: String = "",
    @SerialName("lwprc") val low: String = "",
    @SerialName("clsprc") val close: String = "",
    @SerialName("acml_vol") val volume: String = "",
)
