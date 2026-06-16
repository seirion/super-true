package com.trueedu.tong.model.dto.kis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisIndexPriceResponse(
    val output1: KisIndexPriceDetail? = null,
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg1") val msg1: String = "",
)

@Serializable
data class KisIndexPriceDetail(
    @SerialName("bstp_nmix_prpr") val price: String = "",         // 업종 지수 현재가
    @SerialName("bstp_nmix_prdy_vrss") val delta: String = "",    // 전일 대비
    @SerialName("bstp_nmix_prdy_ctrt") val rate: String = "",     // 전일 대비율(%)
)
