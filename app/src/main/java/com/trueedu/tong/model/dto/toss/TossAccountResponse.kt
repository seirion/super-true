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
    val result: TossHoldingsResult? = null,
) {
    val overview: TossHoldingsOverview? get() = result?.let {
        TossHoldingsOverview(
            totalMarketValue = it.marketValue?.amount?.usd ?: "",
            totalProfitLoss = it.profitLoss?.amount?.usd ?: "",
            totalProfitLossRate = it.profitLoss?.rate ?: "",
        )
    }
    val holdings: List<TossHolding> get() = result?.items ?: emptyList()
}

@Serializable
data class TossHoldingsResult(
    val totalPurchaseAmount: TossCurrencyAmount? = null,
    val marketValue: TossMarketValue? = null,
    val profitLoss: TossProfitLoss? = null,
    val items: List<TossHolding> = emptyList(),
)

@Serializable
data class TossCurrencyAmount(
    val krw: String = "",
    val usd: String = "",
)

@Serializable
data class TossMarketValue(
    val amount: TossCurrencyAmount? = null,
    val amountAfterCost: TossCurrencyAmount? = null,
)

@Serializable
data class TossProfitLoss(
    val amount: TossCurrencyAmount? = null,
    val amountAfterCost: TossCurrencyAmount? = null,
    val rate: String = "",
    val rateAfterCost: String = "",
)

@Serializable
data class TossDailyProfitLoss(
    val amount: String = "",
    val rate: String = "",
)

@Serializable
data class TossItemMarketValue(
    val purchaseAmount: String = "",
    val amount: String = "",
    val amountAfterCost: String = "",
)

@Serializable
data class TossItemProfitLoss(
    val amount: String = "",
    val amountAfterCost: String = "",
    val rate: String = "",
    val rateAfterCost: String = "",
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
    val marketCountry: String = "",
    val currency: String = "",
    val quantity: String = "",
    val lastPrice: String = "",
    val averagePurchasePrice: String = "",
    val marketValue: TossItemMarketValue? = null,
    val profitLoss: TossItemProfitLoss? = null,
    val dailyProfitLoss: TossDailyProfitLoss? = null,
)
