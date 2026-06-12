package com.trueedu.tong.model.dto.order

/** 체결/미체결 공통 모델 */
data class UnfilledOrderItem(
    val ordNo: String,
    val code: String,
    val name: String,
    val isBuy: Boolean,       // true=매수, false=매도
    val ordPrice: Long,
    val ordQty: Long,
    val filledQty: Long,
    val remainQty: Long,
    val ordTime: String,
    // 취소/정정용 원주문번호 (KIS: orgNo+ordNo, 키움: ordNo)
    val orgNo: String = "",
    val stexTp: String = "KRX",   // 거래소구분 (키움 취소/정정 시 필요)
)

data class FilledOrderItem(
    val code: String,
    val name: String,
    val isBuy: Boolean,
    val filledPrice: Long,
    val filledQty: Long,
    val filledTime: String,
)
