package com.trueedu.tong.model.dto.kis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 국내주식 예약주문 조회 (CTSC0004R) */
@Serializable
data class KisScheduleOrderListResponse(
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg_cd") val msgCd: String = "",
    @SerialName("msg1") val msg1: String = "",
    @SerialName("output") val orders: List<KisScheduleOrder> = emptyList(),
    @SerialName("ctx_area_fk200") val fk200: String = "",
    @SerialName("ctx_area_nk200") val nk200: String = "",
)

@Serializable
data class KisScheduleOrder(
    @SerialName("rsvn_ord_seq") val seq: String = "",
    @SerialName("rsvn_ord_ord_dt") val orderDate: String = "",
    @SerialName("rsvn_ord_rcit_dt") val receivedDate: String = "",
    @SerialName("pdno") val code: String = "",
    @SerialName("kor_item_shtn_name") val name: String = "",
    @SerialName("ord_dvsn_cd") val orderDivisionCode: String = "",
    @SerialName("ord_dvsn_name") val orderDivisionName: String = "",
    @SerialName("ord_rsvn_qty") val quantity: String = "",
    @SerialName("ord_rsvn_unpr") val price: String = "",
    @SerialName("tot_ccld_qty") val filledQuantity: String = "",
    @SerialName("tot_ccld_amt") val filledAmount: String = "",
    @SerialName("sll_buy_dvsn_cd") val sellBuyDivisionCode: String = "",
    @SerialName("odno") val orderNumber: String = "",
    @SerialName("ord_tmd") val orderTime: String = "",
    @SerialName("cncl_ord_dt") val cancelOrderDate: String = "",
    @SerialName("prcs_rslt") val processResult: String = "",
    @SerialName("rjct_rson2") val rejectReason: String = "",
    @SerialName("rsvn_end_dt") val endDate: String = "",
)

/** 예약주문 등록 (CTSC0008U) */
@Serializable
data class KisScheduleOrderResponse(
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg_cd") val msgCd: String = "",
    @SerialName("msg1") val msg1: String? = null,
    val output: KisScheduleOrderSeq? = null,
)

@Serializable
data class KisScheduleOrderSeq(
    @SerialName("RSVN_ORD_SEQ") val seq: String = "",
)

/**
 * 예약주문 정정/취소 (CTSC0013U / CTSC0009U)
 *
 * 문서와 달리 실제 응답은 msg 가 아닌 msg1 로 내려오고,
 * output 의 처리여부 키도 소문자가 아닌 대문자(NRML_PRCS_YN)로 온다.
 */
@Serializable
data class KisScheduleOrderCancelResponse(
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg_cd") val msgCd: String = "",
    val msg: String? = null,
    val msg1: String? = null,
    val output: KisScheduleOrderCancelOutput? = null,
) {
    val message: String get() = msg1 ?: msg ?: ""
}

@Serializable
data class KisScheduleOrderCancelOutput(
    @SerialName("NRML_PRCS_YN") val normalProcess: String = "",
)
