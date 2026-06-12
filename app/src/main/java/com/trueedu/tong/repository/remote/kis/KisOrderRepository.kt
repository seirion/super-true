package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisOrderRepository @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val service: KisOrderService by lazy { retrofit.create(KisOrderService::class.java) }

    suspend fun placeOrder(account: BrokerAccount, request: OrderRequest): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val trId = if (request.isBuy) "TTTC0802U" else "TTTC0801U"
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to trId,
            "custtype" to "P",
        )
        val ordDvsn = if (request.isMarket) "01" else "00"
        val body = mapOf(
            "CANO" to account.accountNum.take(8),
            "ACNT_PRDT_CD" to account.accountNum.drop(8),
            "PDNO" to request.code,
            "ORD_DVSN" to ordDvsn,
            "ORD_QTY" to request.quantity.toString(),
            "ORD_UNPR" to request.price.toString(),
            "EXCG_ID_DVSN_CD" to "KRX",
        )
        val resp = service.order(headers, body)
        val body2 = resp.body() ?: error("KIS 주문 응답 없음")
        if (body2.rtCd != "0") error("KIS 주문 오류: ${body2.msg1}")
        OrderResult(success = true, ordNo = body2.output?.ordNo ?: "", message = body2.msg1)
    }
}
