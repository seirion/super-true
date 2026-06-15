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

// ka10073 일자별종목별 실현손익 (거래건별 상세)
// ka10074 기간별 실현손익 합계
@Serializable
data class KiwoomRealizedPnlResponse(
    // ka10073 응답: 거래별 상세
    @SerialName("dt_stk_rlzt_pl") val items: List<KiwoomRealizedPnlItem> = emptyList(),
    // ka10074 응답: 합계
    @SerialName("rlzt_pl") val totalPnlBeforeCost: String = "",   // 총 실현손익(비용전)
    @SerialName("trde_cmsn") val totalFee: String = "",           // 총 수수료
    @SerialName("trde_tax") val totalTax: String = "",            // 총 세금
    @SerialName("return_code") val returnCode: Int = -1,
    @SerialName("return_msg") val returnMsg: String = "",
)

@Serializable
data class KiwoomRealizedPnlItem(
    @SerialName("stk_cd") val code: String = "",
    @SerialName("stk_nm") val name: String = "",
    @SerialName("cntr_qty") val sellQty: String = "",         // 체결량(매도수량)
    @SerialName("cntr_pric") val sellPrice: String = "",      // 체결가(매도단가)
    @SerialName("tdy_trde_cmsn") val fee: String = "",        // 당일매매수수료
    @SerialName("tdy_trde_tax") val tax: String = "",         // 당일매매세금
    @SerialName("tdy_sel_pl") val pnlBeforeCost: String = "", // 당일매도손익(비용전)
    // 비용후 = pnlBeforeCost - fee - tax
)
