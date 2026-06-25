package com.trueedu.tong.model.dto.order

/**
 * 증권사 공통 주문 요청 (도메인 모델).
 */
data class OrderRequest(
    val code: String,
    val quantity: Int,
    val price: Long,
    val isBuy: Boolean,
    val isMarket: Boolean,
    val exchangeId: String = "SOR", // KRX / NXT / SOR
)
