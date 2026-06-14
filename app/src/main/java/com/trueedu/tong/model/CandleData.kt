package com.trueedu.tong.model

/** 캔들 주기 */
enum class CandlePeriod { MINUTE, DAY, WEEK, MONTH }

/**
 * 증권사 캔들(봉) 데이터 공통 모델.
 * 증권사 별 차트 API 응답을 이 모델로 변환한다.
 */
data class CandleData(
    val datetime: String,   // YYYYMMDD or YYYYMMDDHHmm
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)
