package com.trueedu.tong.model.dto.stockinfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 키움증권 ka10001 (주식기본정보요청) 응답.
 * POST /api/dostk/stkinfo
 * 응답 필드는 flat 구조이며, 가격/대비 필드에는 +/- 부호가 붙을 수 있다.
 */
@Serializable
data class KiwoomStockInfoResponse(
    @SerialName("return_code") val returnCode: Int = 0,
    @SerialName("return_msg") val returnMsg: String = "",

    @SerialName("stk_cd") val code: String = "",
    @SerialName("stk_nm") val name: String = "",
    @SerialName("cur_prc") val currentPrice: String = "",
    @SerialName("pred_pre") val delta: String = "",
    @SerialName("flu_rt") val rate: String = "",
    @SerialName("open_pric") val open: String = "",
    @SerialName("high_pric") val high: String = "",
    @SerialName("low_pric") val low: String = "",
    @SerialName("trde_qty") val volume: String = "",

    @SerialName("mac") val marketCap: String = "",   // 시가총액 (억원)
    @SerialName("per") val per: String = "",
    @SerialName("pbr") val pbr: String = "",
    @SerialName("eps") val eps: String = "",
    @SerialName("bps") val bps: String = "",
    @SerialName("roe") val roe: String = "",
    @SerialName("ev") val ev: String = "",

    @SerialName("sale_amt") val salesAmount: String = "",     // 매출액 (억원)
    @SerialName("bus_pro") val operatingProfit: String = "",  // 영업이익 (억원)
    @SerialName("cup_nga") val netProfit: String = "",        // 당기순이익 (억원)

    @SerialName("fav") val parValue: String = "",       // 액면가
    @SerialName("cap") val capital: String = "",        // 자본금 (억원)
    @SerialName("flo_stk") val listedShares: String = "", // 상장주식수 (천주)
    @SerialName("setl_mm") val settlementMonth: String = "", // 결산월

    @SerialName("250hgst") val high250: String = "",    // 250일 최고
    @SerialName("250lwst") val low250: String = "",     // 250일 최저
    @SerialName("upl_pric") val upperLimit: String = "", // 상한가
    @SerialName("lst_pric") val lowerLimit: String = "", // 하한가

    @SerialName("for_exh_rt") val foreignExhaustionRate: String = "", // 외인소진률
    @SerialName("crd_rt") val creditRate: String = "",  // 신용비율
)
