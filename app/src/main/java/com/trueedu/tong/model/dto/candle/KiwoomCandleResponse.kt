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
    @SerialName("stk_dt_pole_chart_qry") val candles: List<KiwoomCandleItem> = emptyList(),
)

@Serializable
data class KiwoomCandleItem(
    @SerialName("dt") val date: String = "",           // YYYYMMDD
    @SerialName("open_pric") val open: String = "",    // 시가
    @SerialName("high_pric") val high: String = "",    // 고가
    @SerialName("low_pric") val low: String = "",      // 저가
    @SerialName("cur_prc") val close: String = "",     // 현재가(종가)
    @SerialName("trde_qty") val volume: String = "",   // 거래량
)
