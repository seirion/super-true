package com.trueedu.tong.model.dto.toss

import kotlinx.serialization.Serializable

// GET /api/v1/accounts 응답
@Serializable
data class TossAccountListResponse(
    val result: List<TossAccount> = emptyList(),
) {
    val data: List<TossAccount> get() = result
}

@Serializable
data class TossAccount(
    val accountSeq: Int = 0,
    val accountNo: String = "",
    val accountType: String = "",
) {
    val accountNumber: String get() = accountNo
    val accountSeqStr: String get() = accountSeq.toString()
}

// GET /api/v1/holdings 응답
@Serializable
data class TossHoldingsResponse(
    val overview: TossHoldingsOverview? = null,
    val holdings: List<TossHolding> = emptyList(),
)

@Serializable
data class TossHoldingsOverview(
    val totalMarketValue: String = "",
    val totalProfitLoss: String = "",
    val totalProfitLossRate: String = "",
)

@Serializable
data class TossHolding(
    val symbol: String = "",
    val name: String = "",
    val quantity: String = "",
    val averagePrice: String = "",
    val currentPrice: String = "",
    val marketValue: String = "",
    val profitLoss: String = "",
    val profitLossRate: String = "",
)
