package com.trueedu.tong.model.dto.toss

import kotlinx.serialization.Serializable

// GET /api/v1/buying-power 응답
@Serializable
data class TossBuyingPowerResponse(
    val result: TossBuyingPower? = null,
)

@Serializable
data class TossBuyingPower(
    val currency: String = "",
    // 현금 기반 매수 가능 금액 (미수 미발생 기준). KRW 정수, USD 소수점 포함
    val cashBuyingPower: String = "",
)
