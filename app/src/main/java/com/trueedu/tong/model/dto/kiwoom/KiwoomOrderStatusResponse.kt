package com.trueedu.tong.model.dto.kiwoom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ka10075 미체결 응답 — 리스트 키: "oso"
@Serializable
data class KiwoomUnfilledOrderResponse(
    @SerialName("oso") val orders: List<KiwoomUnfilledOrder> = emptyList(),
    @SerialName("return_code") val returnCode: Int = -1,
    @SerialName("return_msg") val returnMsg: String = "",
)

@Serializable
data class KiwoomUnfilledOrder(
    @SerialName("stk_cd") val code: String = "",
    @SerialName("stk_nm") val name: String = "",
    @SerialName("ord_no") val ordNo: String = "",
    @SerialName("orig_ord_no") val orgOrdNo: String = "",  // 원주문번호
    @SerialName("ord_qty") val ordQty: String = "",
    @SerialName("ord_pric") val ordPrice: String = "",
    @SerialName("cntr_qty") val filledQty: String = "",    // 체결수량
    @SerialName("oso_qty") val remainQty: String = "",     // 미체결수량
    @SerialName("trde_tp") val tradeType: String = "",     // 매매구분 1:매도 2:매수
    @SerialName("io_tp_nm") val ordTypeName: String = "",  // 주문구분명
    @SerialName("tm") val ordTime: String = "",
    @SerialName("stex_tp") val stexTp: String = "1",       // 거래소구분 숫자 (1:KRX, 2:NXT)
    @SerialName("stex_tp_txt") val stexTpTxt: String = "KRX", // 거래소구분 문자 (KRX/NXT)
)

// ka10076 체결 응답 — 리스트 키: "cntr"
@Serializable
data class KiwoomFilledOrderResponse(
    @SerialName("cntr") val orders: List<KiwoomFilledOrder> = emptyList(),
    @SerialName("return_code") val returnCode: Int = -1,
    @SerialName("return_msg") val returnMsg: String = "",
)

@Serializable
data class KiwoomFilledOrder(
    @SerialName("stk_cd") val code: String = "",
    @SerialName("stk_nm") val name: String = "",
    @SerialName("ord_no") val ordNo: String = "",
    @SerialName("cntr_pric") val filledPrice: String = "",
    @SerialName("cntr_qty") val filledQty: String = "",
    @SerialName("trde_tp") val tradeType: String = "",     // 매매구분 1:매도 2:매수
    @SerialName("ord_tm") val filledTime: String = "",
)

// kt10002/kt10003 주문 응답
@Serializable
data class KiwoomModifyCancelResponse(
    @SerialName("ord_no") val ordNo: String = "",
    @SerialName("return_code") val returnCode: Int = -1,
    @SerialName("return_msg") val returnMsg: String = "",
)

// kt00015 일별 실현손익 응답
@Serializable
data class KiwoomRealizedPnlResponse(
    @SerialName("reali_pnl_list") val items: List<KiwoomRealizedPnlItem> = emptyList(),
    @SerialName("tot_reali_pnl") val totalPnlBeforeCost: String = "",  // 총실현손익(비용전)
    @SerialName("tot_net_reali_pnl") val totalPnlAfterCost: String = "", // 총실현손익(비용후)
    @SerialName("tot_fee") val totalFee: String = "",                  // 총수수료
    @SerialName("tot_tax") val totalTax: String = "",                  // 총세금
    @SerialName("return_code") val returnCode: Int = -1,
    @SerialName("return_msg") val returnMsg: String = "",
)

@Serializable
data class KiwoomRealizedPnlItem(
    @SerialName("stk_cd") val code: String = "",
    @SerialName("stk_nm") val name: String = "",
    @SerialName("sell_qty") val sellQty: String = "",       // 매도수량
    @SerialName("sell_pric") val sellPrice: String = "",    // 매도단가
    @SerialName("fee") val fee: String = "",                // 수수료
    @SerialName("tax") val tax: String = "",                // 세금
    @SerialName("reali_pnl") val pnlBeforeCost: String = "", // 실현손익(비용전)
    @SerialName("net_reali_pnl") val pnlAfterCost: String = "", // 실현손익(비용후)
)
