package com.trueedu.tong.model.dto.order

/** 예약주문 등록/정정 요청 (증권사 공통) */
data class ScheduleOrderRequest(
    val code: String,
    val isBuy: Boolean,
    val price: Long,
    val quantity: Long,
)

/** 예약주문 목록 항목 (증권사 공통) */
data class ScheduleOrderItem(
    val seq: String,          // 예약주문 순번 (정정/취소 키)
    val code: String,
    val name: String,
    val isBuy: Boolean,
    val price: Long,
    val quantity: Long,
    val filledQuantity: Long,
    val orderDate: String,    // 예약주문 주문일자 yyyyMMdd
    val endDate: String,      // 예약 종료일자 yyyyMMdd
    val processResult: String,
    val rejectReason: String,
) {
    /** '처리' 상태인 예약은 정정/취소할 수 없다 */
    val disabled: Boolean get() = processResult == "처리"
}
