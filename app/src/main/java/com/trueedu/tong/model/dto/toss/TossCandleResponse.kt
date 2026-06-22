package com.trueedu.tong.model.dto.toss

import kotlinx.serialization.Serializable

// GET /api/v1/candles 응답
@Serializable
data class TossCandlesResponse(
    val data: List<TossCandle> = emptyList(),
)

@Serializable
data class TossCandle(
    val timestamp: String = "",
    val open: String = "",
    val high: String = "",
    val low: String = "",
    val close: String = "",
    val volume: String = "",
)
