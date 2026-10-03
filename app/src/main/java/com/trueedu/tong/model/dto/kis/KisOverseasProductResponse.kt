package com.trueedu.tong.model.dto.kis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 해외주식 상품기본정보 (CTPF1702R) */
@Serializable
data class KisOverseasProductResponse(
    val output: KisOverseasProduct? = null,
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg1") val msg1: String = "",
)

@Serializable
data class KisOverseasProduct(
    @SerialName("std_pdno") val stdPdno: String = "",           // 표준상품번호
    @SerialName("ovrs_excg_cd") val exchangeCode: String = "",  // 해외거래소코드
    @SerialName("tr_crcy_cd") val currency: String = "",        // 거래통화코드
)
