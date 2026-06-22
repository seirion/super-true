package com.trueedu.tong.model.dto.toss

import kotlinx.serialization.Serializable

// GET /api/v1/prices 응답
@Serializable
data class TossPricesResponse(
    val data: List<TossPrice> = emptyList(),
)

@Serializable
data class TossPrice(
    val symbol: String = "",
    val price: String = "",
    val change: String = "",
    val changeRate: String = "",
    val volume: String = "",
    val marketCountry: String = "KR",
)

// GET /api/v1/stocks 응답
@Serializable
data class TossStocksResponse(
    val data: List<TossStock> = emptyList(),
)

@Serializable
data class TossStock(
    val symbol: String = "",
    val name: String = "",
    val market: String = "",
    val currency: String = "",
)
