package com.trueedu.tong.model.dto.kis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TTTC8036R 미체결 주문 조회 응답
@Serializable
data class KisUnfilledOrderResponse(
    @SerialName("output") val orders: List<KisUnfilledOrder> = emptyList(),
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg1") val msg1: String = "",
    @SerialName("ctx_area_fk100") val fk100: String = "",
    @SerialName("ctx_area_nk100") val nk100: String = "",
)

@Serializable
data class KisUnfilledOrder(
    @SerialName("ord_gno_brno") val orgNo: String = "",    // 주문채번지점번호
    @SerialName("odno") val ordNo: String = "",            // 주문번호
    @SerialName("orgn_odno") val orgOrdNo: String = "",    // 원주문번호
    @SerialName("ord_dvsn_name") val ordTypeName: String = "", // 주문구분명
    @SerialName("pdno") val code: String = "",             // 종목코드
    @SerialName("prdt_name") val name: String = "",        // 종목명
    @SerialName("rvse_cncl_dvsn_name") val reviseCancelName: String = "", // 정정취소구분명
    @SerialName("ord_qty") val ordQty: String = "",        // 주문수량
    @SerialName("ord_unpr") val ordPrice: String = "",     // 주문단가
    @SerialName("ord_tmd") val ordTime: String = "",       // 주문시각
    @SerialName("tot_ccld_qty") val filledQty: String = "", // 총체결수량
    @SerialName("tot_ccld_amt") val filledAmt: String = "", // 총체결금액
    @SerialName("psbl_qty") val remainQty: String = "",    // 주문가능수량(미체결)
    @SerialName("sll_buy_dvsn_cd") val sellBuyCode: String = "", // 매도매수구분코드 01:매도 02:매수
    @SerialName("ord_dvsn_cd") val ordDvsnCd: String = "", // 주문구분코드
)

// TTTC8001R 체결 내역 조회
@Serializable
data class KisFilledOrderResponse(
    @SerialName("output1") val orders: List<KisFilledOrder> = emptyList(),
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg1") val msg1: String = "",
)

@Serializable
data class KisFilledOrder(
    @SerialName("pdno") val code: String = "",
    @SerialName("prdt_name") val name: String = "",
    @SerialName("sll_buy_dvsn_cd") val sellBuyCode: String = "", // 01:매도 02:매수
    @SerialName("ord_qty") val ordQty: String = "",
    @SerialName("tot_ccld_qty") val filledQty: String = "",
    @SerialName("avg_prvs") val avgPrice: String = "",      // 체결평균가
    @SerialName("ord_tmd") val ordTime: String = "",
)

// TTTC8715R 일별 실현손익 조회 응답
@Serializable
data class KisRealizedPnlResponse(
    @SerialName("output1") val items: List<KisRealizedPnlItem> = emptyList(),
    @SerialName("output2") val summary: KisRealizedPnlSummary? = null,
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg1") val msg1: String = "",
)

@Serializable
data class KisRealizedPnlItem(
    @SerialName("pdno") val code: String = "",
    @SerialName("prdt_name") val name: String = "",
    @SerialName("sll_qty") val sellQty: String = "",        // 매도수량
    @SerialName("sll_pric") val sellPrice: String = "",     // 매도단가
    @SerialName("sll_amt") val sellAmt: String = "",        // 매도총액
    @SerialName("fee") val fee: String = "",                // 수수료
    @SerialName("tl_tax") val tax: String = "",             // 세금(제세금 합계)
    @SerialName("rlzt_pfls") val pnlBeforeCost: String = "", // 실현손익(비용전)
    // 비용후 = rlzt_pfls - fee - tl_tax (별도 필드 없음)
)

@Serializable
data class KisRealizedPnlSummary(
    @SerialName("tot_rlzt_pfls") val totalPnlBeforeCost: String = "", // 총실현손익(비용전)
    @SerialName("tot_ncls_pfls") val totalPnlAfterCost: String = "",  // 총실현손익(비용후)
    @SerialName("tot_bfee") val totalFee: String = "",               // 총수수료
    @SerialName("tot_tax") val totalTax: String = "",                // 총세금
)
