package com.trueedu.tong.repository.remote.kb

import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.model.dto.order.OrderResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KbOrderRepository @Inject constructor(
    private val client: KbApiClient,
) {
    /** SSAM1802 매수주문 / SSAM1801 매도주문 */
    suspend fun placeOrder(account: BrokerAccount, request: OrderRequest): Result<OrderResult> = runCatching {
        val tr = if (request.isBuy) "SSAM1802" else "SSAM1801"
        val data = client.call(
            account,
            tr,
            mapOf(
                "mkt_tm_clsf" to "1", // 정규장
                "is_cd" to request.code,
                "ordr_q" to request.quantity.toString(),
                "ordr_uprc" to if (request.isMarket) "0" else request.price.toString(),
                "ordr_ccd" to if (request.isMarket) "03" else "00",
                "sor_ordr_ccd" to KbExchange.toCode(request.exchangeId),
            ),
        ).getOrThrow()
        OrderResult(success = true, ordNo = data.str("ordr_no"), message = data.str("o_msg"))
    }
}

/** 앱의 거래소 구분(KRX/NXT/SOR)과 KB sor_ordr_ccd(K/N/S) 변환 */
object KbExchange {
    fun toCode(exchangeId: String): String = when (exchangeId) {
        "KRX" -> "K"
        "NXT" -> "N"
        else -> "S"
    }

    fun fromCode(code: String): String = when (code) {
        "K" -> "KRX"
        "N" -> "NXT"
        else -> "SOR"
    }
}
