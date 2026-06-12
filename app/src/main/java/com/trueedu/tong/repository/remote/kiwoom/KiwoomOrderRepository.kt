package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.di.KiwoomRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KiwoomOrderRepository @Inject constructor(
    @KiwoomRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val service by lazy { retrofit.create(KiwoomOrderService::class.java) }
    suspend fun placeOrder(account: BrokerAccount, request: OrderRequest): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val apiId = if (request.isBuy) "kt10000" else "kt10001"
        val headers = mapOf("authorization" to "Bearer $token", "api-id" to apiId, "cont-yn" to "N", "next-key" to "")
        val body = mapOf(
            "stk_cd" to request.code,
            "ord_qty" to request.quantity.toString(),
            "ord_uv" to request.price.toString(),          // 주문단가 (ord_unpr 아닌 ord_uv)
            "trde_tp" to if (request.isMarket) "3" else "0", // 0=보통(지정가), 3=시장가
            "dmst_stex_tp" to "SOR",                       // KRX/NXT/SOR
        )
        val resp = service.order(headers, body)
        val body2 = resp.body() ?: error("키움 주문 응답 없음")
        if (body2.returnCode != 0) error("키움 주문 오류: ${body2.returnMsg}")
        OrderResult(success = true, ordNo = body2.ordNo, message = body2.returnMsg)
    }
}
