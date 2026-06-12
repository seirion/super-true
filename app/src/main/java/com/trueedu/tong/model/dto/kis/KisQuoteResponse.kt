package com.trueedu.tong.model.dto.kis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * KIS 주식현재가 호가/예상체결 (inquire-asking-price-exp-ccn) 응답.
 * WebSocket 첫 호가 수신 전 초기값으로 사용한다.
 */
@Serializable
data class KisQuoteResponse(
    val output1: KisQuoteAsk? = null,
    val output2: KisQuotePrice? = null,
    @SerialName("rt_cd") val rtCd: String = "",
    @SerialName("msg1") val msg1: String = "",
)

@Serializable
data class KisQuoteAsk(
    // 매도 호가 1~10 (askp1~askp10)
    @SerialName("askp1") val askp1: String = "0",
    @SerialName("askp2") val askp2: String = "0",
    @SerialName("askp3") val askp3: String = "0",
    @SerialName("askp4") val askp4: String = "0",
    @SerialName("askp5") val askp5: String = "0",
    @SerialName("askp6") val askp6: String = "0",
    @SerialName("askp7") val askp7: String = "0",
    @SerialName("askp8") val askp8: String = "0",
    @SerialName("askp9") val askp9: String = "0",
    @SerialName("askp10") val askp10: String = "0",
    // 매도 잔량 1~10
    @SerialName("askp_rsqn1") val askRsqn1: String = "0",
    @SerialName("askp_rsqn2") val askRsqn2: String = "0",
    @SerialName("askp_rsqn3") val askRsqn3: String = "0",
    @SerialName("askp_rsqn4") val askRsqn4: String = "0",
    @SerialName("askp_rsqn5") val askRsqn5: String = "0",
    @SerialName("askp_rsqn6") val askRsqn6: String = "0",
    @SerialName("askp_rsqn7") val askRsqn7: String = "0",
    @SerialName("askp_rsqn8") val askRsqn8: String = "0",
    @SerialName("askp_rsqn9") val askRsqn9: String = "0",
    @SerialName("askp_rsqn10") val askRsqn10: String = "0",
    // 매수 호가 1~10 (bidp1~bidp10)
    @SerialName("bidp1") val bidp1: String = "0",
    @SerialName("bidp2") val bidp2: String = "0",
    @SerialName("bidp3") val bidp3: String = "0",
    @SerialName("bidp4") val bidp4: String = "0",
    @SerialName("bidp5") val bidp5: String = "0",
    @SerialName("bidp6") val bidp6: String = "0",
    @SerialName("bidp7") val bidp7: String = "0",
    @SerialName("bidp8") val bidp8: String = "0",
    @SerialName("bidp9") val bidp9: String = "0",
    @SerialName("bidp10") val bidp10: String = "0",
    // 매수 잔량 1~10
    @SerialName("bidp_rsqn1") val bidRsqn1: String = "0",
    @SerialName("bidp_rsqn2") val bidRsqn2: String = "0",
    @SerialName("bidp_rsqn3") val bidRsqn3: String = "0",
    @SerialName("bidp_rsqn4") val bidRsqn4: String = "0",
    @SerialName("bidp_rsqn5") val bidRsqn5: String = "0",
    @SerialName("bidp_rsqn6") val bidRsqn6: String = "0",
    @SerialName("bidp_rsqn7") val bidRsqn7: String = "0",
    @SerialName("bidp_rsqn8") val bidRsqn8: String = "0",
    @SerialName("bidp_rsqn9") val bidRsqn9: String = "0",
    @SerialName("bidp_rsqn10") val bidRsqn10: String = "0",
) {
    fun sells(): List<Pair<Double, Double>> = listOf(
        askp10 to askRsqn10, askp9 to askRsqn9, askp8 to askRsqn8,
        askp7 to askRsqn7, askp6 to askRsqn6, askp5 to askRsqn5,
        askp4 to askRsqn4, askp3 to askRsqn3, askp2 to askRsqn2,
        askp1 to askRsqn1,
    ).map { (p, q) -> p.toDoubleOrNull().orZero() to q.toDoubleOrNull().orZero() }

    fun buys(): List<Pair<Double, Double>> = listOf(
        bidp1 to bidRsqn1, bidp2 to bidRsqn2, bidp3 to bidRsqn3,
        bidp4 to bidRsqn4, bidp5 to bidRsqn5, bidp6 to bidRsqn6,
        bidp7 to bidRsqn7, bidp8 to bidRsqn8, bidp9 to bidRsqn9,
        bidp10 to bidRsqn10,
    ).map { (p, q) -> p.toDoubleOrNull().orZero() to q.toDoubleOrNull().orZero() }
}

@Serializable
data class KisQuotePrice(
    @SerialName("stck_prpr") val price: String = "0",       // 현재가
    @SerialName("prdy_vrss") val delta: String = "0",        // 전일대비
    @SerialName("prdy_ctrt") val rate: String = "0",         // 등락률
)

private fun Double?.orZero() = this ?: 0.0
