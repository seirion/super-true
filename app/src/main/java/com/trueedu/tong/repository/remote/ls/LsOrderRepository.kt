package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.di.LsRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LsOrderRepository @Inject constructor(
    @LsRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val service: LsOrderService by lazy { retrofit.create(LsOrderService::class.java) }

    suspend fun placeOrder(account: BrokerAccount, request: OrderRequest): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val bnsTpCode = if (request.isBuy) "1" else "2"
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "tr_cd" to "CSPAT00601",
            "tr_cont" to "N",
            "tr_cont_key" to "",
            "mac_address" to "",
        )
        val ordprcPtnCode = if (request.isMarket) "03" else "00"
        // LS는 nested body이지만 flat map으로 처리 (실제 연동 시 @Serializable request body로 교체 필요)
        val body = mapOf(
            "AcntNo" to account.accountNum,
            "Pwd" to "",
            "IsuNo" to request.code,
            "OrdQty" to request.quantity.toString(),
            "OrdPrc" to request.price.toString(),
            "BnsTpCode" to bnsTpCode,
            "OrdprcPtnCode" to ordprcPtnCode,
            "MgntrnCode" to "000",
            "LoanDt" to "",
        )
        val resp = service.order(headers, body)
        val body2 = resp.body() ?: error("LS 주문 응답 없음")
        if (body2.rspCd != "0") error("LS 주문 오류: ${body2.rspMsg}")
        OrderResult(success = true, ordNo = body2.output?.ordNo ?: "", message = body2.rspMsg)
    }
}
