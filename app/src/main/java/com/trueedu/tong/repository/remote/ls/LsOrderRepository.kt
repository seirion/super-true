package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.di.LsRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.ls.LsOrderInBlock1
import com.trueedu.tong.model.dto.ls.LsOrderRequest
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LsOrderRepository @Inject constructor(
    @LsRetrofitQualifier private val retrofit: Retrofit,
    private val tokenManager: TokenManager,
) {
    private val service by lazy { retrofit.create(LsOrderService::class.java) }

    suspend fun placeOrder(account: BrokerAccount, request: OrderRequest): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "tr_cd" to "CSPAT00601",
            "tr_cont" to "N",
            "tr_cont_key" to "",
            "mac_address" to "",
        )
        val body = LsOrderRequest(
            block = LsOrderInBlock1(
                acntNo = account.accountNum,
                isuNo = request.code,
                ordQty = request.quantity.toLong(),
                ordPrc = if (request.isMarket) 0.0 else request.price.toDouble(),
                bnsTpCode = if (request.isBuy) "2" else "1",   // 2:매수 1:매도
                ordprcPtnCode = if (request.isMarket) "03" else "00",
            )
        )
        val resp = service.order(headers, body)
        val body2 = resp.body() ?: error("LS 주문 응답 없음")
        // LS 성공 코드: "00000"
        if (body2.rspCd != "00000") error("LS 주문 오류: ${body2.rspMsg}")
        OrderResult(success = true, ordNo = body2.output?.ordNo ?: "", message = body2.rspMsg)
    }
}
