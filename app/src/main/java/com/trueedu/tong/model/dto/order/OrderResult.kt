package com.trueedu.tong.model.dto.order

// 공통 주문 결과
data class OrderResult(
    val success: Boolean,
    val ordNo: String = "",
    val message: String = "",
)
