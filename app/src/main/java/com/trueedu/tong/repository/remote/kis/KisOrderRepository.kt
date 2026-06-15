package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.order.KisOrderRequest
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.utils.MarketHours
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisOrderRepository @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val service: KisOrderService by lazy {
        retrofit.create(KisOrderService::class.java)
    }

    /**
     * KIS 주식 현금 매수/매도 주문 (실전투자).
     * 성공 시 주문번호(또는 메시지)를 반환한다.
     */
    suspend fun placeOrder(
        account: BrokerAccount,
        request: OrderRequest,
    ): Result<String> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        // 정규장(09:00~15:30): SOR → TTTC0802U/TTTC0801U
        // NXT 시간(08:00~09:00, 15:30~20:00): NXT → TTTC0012U/TTTC0011U
        val trId = if (request.isBuy) MarketHours.kisBuyTrId() else MarketHours.kisSellTrId()
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to trId,
            "custtype" to "P",
        )
        val body = KisOrderRequest(
            cano = account.accountNum.take(8),
            acntPrdtCd = account.accountNum.drop(8),
            pdno = request.code.removePrefix("A"),
            ordDvsn = if (request.isMarket) "01" else "00",
            ordQty = request.quantity.toString(),
            ordUnpr = if (request.isMarket) "0" else request.price.toString(),
        )

        val response = service.orderCash(headers, body)
        val result = response.body() ?: error("KIS 주문 응답 없음")
        if (result.rtCd != "0") error(result.msg1.ifBlank { "주문 실패 (${result.msgCd})" })
        result.output?.odno?.takeIf { it.isNotBlank() }
            ?.let { "주문번호 $it" }
            ?: result.msg1
    }
}
