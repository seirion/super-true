package com.trueedu.tong.model.dto.ls

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// t0424 요청
@Serializable
data class LsBalanceRequest(
    @SerialName("t0424InBlock") val block: LsBalanceInBlock = LsBalanceInBlock()
)

@Serializable
data class LsBalanceInBlock(
    @SerialName("prcgb") val prcgb: String = "1",       // 단가구분 1:평균단가
    @SerialName("chegb") val chegb: String = "0",       // 체결기준 0:결제기준
    @SerialName("sortgb") val sortgb: String = "1",     // 정렬구분 1:종목명
    @SerialName("cts_expcode") val ctsExpcode: String = "",
)

// CSPAQ12200 요청
@Serializable
data class LsDepositRequest(
    @SerialName("CSPAQ12200InBlock1") val block: LsDepositInBlock
)

@Serializable
data class LsDepositInBlock(
    @SerialName("RecCnt") val recCnt: String = "1",
    @SerialName("AcntNo") val acntNo: String,
    @SerialName("Pwd") val pwd: String = "",
    @SerialName("BalCreTp") val balCreTp: String = "0",
)
