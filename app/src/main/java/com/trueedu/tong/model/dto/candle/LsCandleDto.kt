package com.trueedu.tong.model.dto.candle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * LS증권 t8410 (주식 기간별 주가 조회) 요청/응답.
 * POST /stock/chart
 */
@Serializable
data class LsCandleRequest(
    @SerialName("t8410InBlock") val inBlock: LsCandleInBlock,
)

@Serializable
data class LsCandleInBlock(
    @SerialName("shcode") val code: String,
    @SerialName("gubun") val period: String = "2",   // 2=일, 3=주, 4=월
    @SerialName("sdate") val startDate: String = "",
    @SerialName("edate") val endDate: String = "",
    @SerialName("cts_date") val ctsDate: String = "",
    @SerialName("comp_yn") val comp: String = "N",
    @SerialName("sujung") val adjusted: String = "Y",
)

// t8412 분봉 요청
@Serializable
data class LsMinuteCandleRequest(
    @SerialName("t8412InBlock") val inBlock: LsMinuteCandleInBlock,
)

@Serializable
data class LsMinuteCandleInBlock(
    @SerialName("shcode") val code: String,
    @SerialName("ncnt") val interval: String = "1",  // 분 단위: 1,3,5,10,30,60
    @SerialName("sdate") val startDate: String = "",
    @SerialName("edate") val endDate: String = "",
    @SerialName("stime") val startTime: String = "",
    @SerialName("etime") val endTime: String = "",
    @SerialName("cts_date") val ctsDate: String = "",
    @SerialName("cts_time") val ctsTime: String = "",
    @SerialName("comp_yn") val comp: String = "N",
)

@Serializable
data class LsMinuteCandleResponse(
    @SerialName("t8412OutBlock1") val candles: List<LsCandleItem> = emptyList(),
)

@Serializable
data class LsCandleResponse(
    @SerialName("t8410OutBlock1") val candles: List<LsCandleItem> = emptyList(),
)

@Serializable
data class LsCandleItem(
    @SerialName("date") val date: String = "",
    @SerialName("open") val open: String = "",
    @SerialName("high") val high: String = "",
    @SerialName("low") val low: String = "",
    @SerialName("close") val close: String = "",
    @SerialName("jdiff_vol") val volume: String = "",
)
