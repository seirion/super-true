package com.trueedu.tong.model.dto.ls

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// t0425 미체결/체결 응답
@Serializable
data class LsOrderStatusResponse(
    @SerialName("t0425OutBlock1") val orders: List<LsOrderStatusItem> = emptyList(),
    @SerialName("rsp_cd") val rspCd: String = "",
    @SerialName("rsp_msg") val rspMsg: String = "",
)

@Serializable
data class LsOrderStatusItem(
    @SerialName("ordno") val ordNo: Long = 0L,          // 주문번호
    @SerialName("expcode") val code: String = "",        // 종목코드
    @SerialName("hname") val name: String = "",          // 종목명
    @SerialName("medosu") val medosu: String = "",       // 매도수구분 1:매도 2:매수
    @SerialName("qty") val qty: Long = 0L,               // 주문수량
    @SerialName("price") val price: Long = 0L,           // 주문가격
    @SerialName("ordrem") val remainQty: Long = 0L,      // 미체결잔량
    @SerialName("cgqty") val filledQty: Long = 0L,       // 체결수량
    @SerialName("orgordno") val orgOrdNo: Long = 0L,     // 원주문번호
    @SerialName("ordtime") val ordTime: String = "",     // 주문시간
    @SerialName("price1") val currentPrice: Long = 0L,  // 현재가
)

// CSPAT00701 정정 요청
@Serializable
data class LsModifyRequest(
    @SerialName("CSPAT00701InBlock1") val block: LsModifyInBlock1
)

@Serializable
data class LsModifyInBlock1(
    @SerialName("OrgOrdNo") val orgOrdNo: Long,
    @SerialName("IsuNo") val isuNo: String,
    @SerialName("OrdQty") val ordQty: Long,
    @SerialName("OrdprcPtnCode") val ordprcPtnCode: String = "00",
    @SerialName("OrdCndiTpCode") val ordCndiTpCode: String = "0",
    @SerialName("OrdPrc") val ordPrc: Double,
)

// CSPAT00801 취소 요청
@Serializable
data class LsCancelRequest(
    @SerialName("CSPAT00801InBlock1") val block: LsCancelInBlock1
)

@Serializable
data class LsCancelInBlock1(
    @SerialName("OrgOrdNo") val orgOrdNo: Long,
    @SerialName("IsuNo") val isuNo: String,
    @SerialName("OrdQty") val ordQty: Long,
)

// 정정/취소 공통 응답
@Serializable
data class LsModifyCancelResponse(
    @SerialName("rsp_cd") val rspCd: String = "",
    @SerialName("rsp_msg") val rspMsg: String = "",
)

// t0425 요청
@Serializable
data class LsOrderStatusRequest(
    @SerialName("t0425InBlock") val block: LsOrderStatusInBlock
)

@Serializable
data class LsOrderStatusInBlock(
    @SerialName("expcode") val expcode: String = "",
    @SerialName("chegb") val chegb: String,      // 2:미체결, 1:체결, 0:전체
    @SerialName("medosu") val medosu: String = "0", // 0:전체, 1:매도, 2:매수
    @SerialName("sortgb") val sortgb: String = "1",
    @SerialName("cts_ordno") val ctsOrdno: String = "",
)
