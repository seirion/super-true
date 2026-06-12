package com.trueedu.tong.model.dto.order

/**
 * 증권사 공통 주문 결과 (도메인 모델).
 */
data class OrderResult(
    val success: Boolean,
    val ordNo: String,
    val message: String,
)
