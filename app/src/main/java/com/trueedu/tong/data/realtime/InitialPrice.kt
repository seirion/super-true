package com.trueedu.tong.data.realtime

/**
 * WebSocket 첫 체결 전 REST API로 1회 조회한 초기 현재가.
 * 실시간 데이터(KisRealTimeTrade)가 들어오기 전까지 fallback으로 사용된다.
 */
data class InitialPrice(
    val code: String,
    val price: Double,
    val delta: Double,
    val rate: Double,
)
