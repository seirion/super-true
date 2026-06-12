package com.trueedu.tong.model.dto.order

// 공통 주문 파라미터 (내부 도메인 모델)
data class OrderRequest(
    val code: String,        // 종목코드 (KIS: 6자리, 키움: A제외 6자리, LS: 6자리)
    val quantity: Int,       // 주문수량
    val price: Long,         // 주문가격 (시장가 = 0)
    val isBuy: Boolean,      // true=매수, false=매도
    val isMarket: Boolean,   // true=시장가, false=지정가
)
