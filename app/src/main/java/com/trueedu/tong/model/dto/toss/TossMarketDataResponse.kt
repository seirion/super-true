package com.trueedu.tong.model.dto.toss

import kotlinx.serialization.Serializable

// GET /api/v1/stocks/all 응답
@Serializable
data class TossUniverseResponse(
    val result: List<TossUniverseStock> = emptyList(),
)

@Serializable
data class TossUniverseStock(
    val symbol: String = "",
    val name: String = "",              // 종목명 (한글)
    val securityType: String = "",      // STOCK / ETF / REIT ...
    val isCommonShare: Boolean = true,
    val isinCode: String? = null,
)

// GET /api/v1/prices 응답
@Serializable
data class TossLastPriceResponse(
    val result: List<TossLastPrice> = emptyList(),
)

@Serializable
data class TossLastPrice(
    val symbol: String = "",
    val timestamp: String? = null,
    val lastPrice: String = "",
    val currency: String = "",
)

// GET /api/v1/candles 응답 (최신순)
@Serializable
data class TossDailyCandleResponse(
    val result: TossDailyCandleResult? = null,
)

@Serializable
data class TossDailyCandleResult(
    val candles: List<TossDailyCandle> = emptyList(),
)

@Serializable
data class TossDailyCandle(
    val timestamp: String = "",         // 1d: 해당 거래일 (현지 자정)
    val closePrice: String = "",
)
