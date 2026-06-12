package com.trueedu.tong.model.ws

/**
 * KIS 실시간 호가 (H0STASP0)
 * "^" 구분자로 분리된 raw 데이터를 파싱한다.
 */
class KisRealTimeQuote(val data: List<String>) {
    companion object {
        fun from(rawData: String) = KisRealTimeQuote(rawData.split("^"))
    }

    val code = data[0]
    val totalSellQty = data.getOrElse(43) { "0" }
    val totalBuyQty = data.getOrElse(44) { "0" }

    // 매도 10호가 (가격, 잔량) - 높은가격→낮은가격 순
    fun sells(): List<Pair<Double, Double>> {
        val prices = 3..12
        val counts = 23..32
        return prices.zip(counts).map { (p, c) ->
            data.getOrElse(p) { "0" }.toDoubleOrNull().orZero() to
                data.getOrElse(c) { "0" }.toDoubleOrNull().orZero()
        }.reversed()
    }

    // 매수 10호가 (가격, 잔량) - 높은가격→낮은가격 순
    fun buys(): List<Pair<Double, Double>> {
        val prices = 13..22
        val counts = 33..42
        return prices.zip(counts).map { (p, c) ->
            data.getOrElse(p) { "0" }.toDoubleOrNull().orZero() to
                data.getOrElse(c) { "0" }.toDoubleOrNull().orZero()
        }
    }
}

private fun Double?.orZero() = this ?: 0.0
