package com.trueedu.tong.model.dto.ls

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// CSPAT00601 주식주문 요청
@Serializable
data class LsOrderRequest(
    @SerialName("CSPAT00601InBlock1") val block: LsOrderInBlock1
)

@Serializable
data class LsOrderInBlock1(
    @SerialName("AcntNo") val acntNo: String,           // 계좌번호
    @SerialName("InptPwd") val inptPwd: String = "",    // 입력비밀번호
    @SerialName("IsuNo") val isuNo: String,             // 종목번호
    @SerialName("OrdQty") val ordQty: Long,             // 주문수량
    @SerialName("OrdPrc") val ordPrc: Double,           // 주문가격 (시장가=0)
    @SerialName("BnsTpCode") val bnsTpCode: String,     // 매매구분 1:매도 2:매수
    @SerialName("OrdprcPtnCode") val ordprcPtnCode: String, // 호가유형코드 00:지정가 03:시장가
    @SerialName("MgntrnCode") val mgntrnCode: String = "000", // 신용거래코드 000:보통
    @SerialName("LoanDt") val loanDt: String = "",      // 대출일
    @SerialName("OrdCndiTpCode") val ordCndiTpCode: String = "0", // 주문조건구분 0:없음
)

// CSPAT00601 응답
@Serializable
data class LsOrderResponse(
    @SerialName("CSPAT00601OutBlock1") val output: LsOrderOutBlock1? = null,
    @SerialName("rsp_cd") val rspCd: String = "",
    @SerialName("rsp_msg") val rspMsg: String = "",
)

@Serializable
data class LsOrderOutBlock1(
    @SerialName("OrdNo") val ordNo: String = "",        // 주문번호
    @SerialName("OrdTime") val ordTime: String = "",    // 주문시각
    @SerialName("OrdMktCode") val ordMktCode: String = "", // 주문시장코드
)
