package com.trueedu.tong.model.dto.toss

import kotlinx.serialization.Serializable

@Serializable
data class TossOrderCreateRequest(
    val symbol: String,
    val side: String,       // "BUY" | "SELL"
    val orderType: String,  // "LIMIT" | "MARKET"
    val price: String? = null,  // MARKET 은 전달 불가 (null 이면 직렬화에서 제외)
    val quantity: String,
    val clientOrderId: String? = null,
    val confirmHighValueOrder: Boolean = false,
)

@Serializable
data class TossOrderModifyRequest(
    val price: String = "",
    val quantity: Int = 0,
)

@Serializable
data class TossOrderResponse(
    val orderId: String = "",
    val clientOrderId: String = "",
    val symbol: String = "",
    val side: String = "",
    val orderType: String = "",
    val price: String = "",
    val quantity: Int = 0,
    val filledQuantity: Int = 0,
    val status: String = "",
    val createdAt: String = "",
)

// 에러 응답
@Serializable
data class TossErrorResponse(
    val error: TossError? = null,
)

@Serializable
data class TossError(
    val requestId: String = "",
    val code: String = "",
    val message: String = "",
)
