package com.trueedu.tong.model.dto.stockinfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * LS증권 t1102 (주식 현재가(시세) 조회) 요청/응답.
 * POST /stock/market-data
 */
@Serializable
data class LsStockInfoRequest(
    @SerialName("t1102InBlock") val block: LsStockInfoInBlock,
)

@Serializable
data class LsStockInfoInBlock(
    @SerialName("shcode") val shcode: String,
)

@Serializable
data class LsStockInfoResponse(
    @SerialName("rsp_cd") val rspCd: String = "",
    @SerialName("rsp_msg") val rspMsg: String = "",
    @SerialName("t1102OutBlock") val output: LsStockInfoOutBlock? = null,
)

@Serializable
data class LsStockInfoOutBlock(
    @SerialName("hname") val name: String = "",       // 종목명
    @SerialName("price") val price: String = "",      // 현재가
    @SerialName("change") val change: String = "",    // 전일대비 (절대값)
    @SerialName("diff") val rate: String = "",        // 등락율
    @SerialName("open") val open: String = "",        // 시가
    @SerialName("high") val high: String = "",        // 고가
    @SerialName("low") val low: String = "",          // 저가
    @SerialName("volume") val volume: String = "",    // 거래량
    @SerialName("jnilclose") val prevClose: String = "", // 전일종가
    @SerialName("uplmtprice") val upperLimit: String = "", // 상한가
    @SerialName("dnlmtprice") val lowerLimit: String = "", // 하한가
    @SerialName("per") val per: String = "",          // PER
    @SerialName("total") val marketCap: String = "",  // 시가총액 (억원)
    @SerialName("t52ghigh") val week52High: String = "", // 52주 최고
    @SerialName("t52glow") val week52Low: String = "",   // 52주 최저
    @SerialName("eps") val eps: String = "",          // EPS
)
