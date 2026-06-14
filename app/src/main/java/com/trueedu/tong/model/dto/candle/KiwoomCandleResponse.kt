package com.trueedu.tong.model.dto.candle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── 일봉 ka10081 ─────────────────────────────────────────────────────────────
@Serializable
data class KiwoomDayCandleResponse(
    @SerialName("stk_cd") val code: String = "",
    @SerialName("stk_dt_pole_chart_qry") val candles: List<KiwoomOhlcvItem> = emptyList(),
)

// ── 주봉 ka10082 ─────────────────────────────────────────────────────────────
@Serializable
data class KiwoomWeekCandleResponse(
    @SerialName("stk_cd") val code: String = "",
    @SerialName("stk_stk_pole_chart_qry") val candles: List<KiwoomOhlcvItem> = emptyList(),
)

// ── 월봉 ka10083 ─────────────────────────────────────────────────────────────
@Serializable
data class KiwoomMonthCandleResponse(
    @SerialName("stk_cd") val code: String = "",
    @SerialName("stk_mth_pole_chart_qry") val candles: List<KiwoomOhlcvItem> = emptyList(),
)

// ── 분봉 ka10080 ─────────────────────────────────────────────────────────────
@Serializable
data class KiwoomMinuteCandleResponse(
    @SerialName("stk_cd") val code: String = "",
    @SerialName("stk_min_pole_chart_qry") val candles: List<KiwoomMinuteItem> = emptyList(),
)

// ── 공통 OHLCV 아이템 (일/주/월봉) ────────────────────────────────────────────
@Serializable
data class KiwoomOhlcvItem(
    @SerialName("dt") val date: String = "",           // YYYYMMDD
    @SerialName("open_pric") val open: String = "",
    @SerialName("high_pric") val high: String = "",
    @SerialName("low_pric") val low: String = "",
    @SerialName("cur_prc") val close: String = "",     // 현재가(종가)
    @SerialName("trde_qty") val volume: String = "",
)

// ── 분봉 아이템 (날짜 필드가 다름) ────────────────────────────────────────────
@Serializable
data class KiwoomMinuteItem(
    @SerialName("cntr_tm") val datetime: String = "",  // YYYYMMDDHHmmss
    @SerialName("open_pric") val open: String = "",
    @SerialName("high_pric") val high: String = "",
    @SerialName("low_pric") val low: String = "",
    @SerialName("cur_prc") val close: String = "",
    @SerialName("trde_qty") val volume: String = "",
)

// 하위 호환용 typealias (기존 코드 참조 대비)
typealias KiwoomCandleResponse = KiwoomDayCandleResponse
typealias KiwoomCandleItem = KiwoomOhlcvItem
