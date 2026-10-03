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
            marketValueKrw = it.marketValue?.amount?.krw,
            marketValueUsd = it.marketValue?.amount?.usd,
            profitLossKrw = it.profitLoss?.amount?.krw,
            profitLossUsd = it.profitLoss?.amount?.usd,
            profitLossRate = it.profitLoss?.rate,
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
    val krw: String? = null,   // 국내 종목이 없으면 "0"
    val usd: String? = null,   // 해외 종목이 없으면 null
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
    val marketValueKrw: String? = null,
    val marketValueUsd: String? = null,
    val profitLossKrw: String? = null,
    val profitLossUsd: String? = null,
    val profitLossRate: String? = null,  // 전체를 현재 환율로 원화 환산한 소수비율
)

// GET /api/v1/exchange-rate 응답
@Serializable
data class TossExchangeRateResponse(
    val result: TossExchangeRate? = null,
)

@Serializable
data class TossExchangeRate(
    val baseCurrency: String = "",
    val quoteCurrency: String = "",
    val rate: String = "",       // 매수 환율
    val midRate: String = "",    // 매매기준율
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
