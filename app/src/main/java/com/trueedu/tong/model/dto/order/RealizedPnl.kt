package com.trueedu.tong.model.dto.order

data class RealizedPnlItem(
    val code: String,
    val name: String,
    val sellQty: Long,
    val sellPrice: Long,
    val fee: Long,
    val tax: Long,
    val pnlBeforeCost: Long,   // 비용전 실현손익
    val pnlAfterCost: Long,    // 비용후 실현손익
    val totalSellAmount: Long = sellQty * sellPrice, // 총 거래액 (합산 시 명시적으로 지정)
)

data class RealizedPnlSummary(
    val totalPnlBeforeCost: Long,
    val totalPnlAfterCost: Long,
    val totalFee: Long,
    val totalTax: Long,
    val items: List<RealizedPnlItem>,
)

enum class PnlDateRange {
    TODAY, THIS_MONTH, THIS_YEAR, CUSTOM
}
