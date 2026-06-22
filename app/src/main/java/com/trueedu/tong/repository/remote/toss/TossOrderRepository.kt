package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.di.TossRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.model.dto.toss.TossErrorResponse
import com.trueedu.tong.model.dto.toss.TossOrderCreateRequest
import com.trueedu.tong.model.dto.toss.TossOrderResponse
import com.trueedu.tong.repository.remote.auth.TokenManager
import kotlinx.serialization.json.Json
import retrofit2.Response
import retrofit2.Retrofit
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import com.trueedu.tong.utils.logW
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TossOrderRepository @Inject constructor(
    @TossRetrofitQualifier private val retrofit: Retrofit,
    private val tokenManager: TokenManager,
    private val accountRepo: TossAccountRepository,
    private val json: Json,
) {
    private val service by lazy { retrofit.create(TossOrderService::class.java) }

    suspend fun placeOrder(account: BrokerAccount, request: OrderRequest): Result<OrderResult> = runCatching {
        val side = if (request.isBuy) "BUY" else "SELL"
        val orderType = if (request.isMarket) "MARKET" else "LIMIT"
        val body = TossOrderCreateRequest(
            symbol = request.code,
            marketCountry = "KR",
            side = side,
            orderType = orderType,
            price = if (request.isMarket) "" else request.price.toString(),
            quantity = request.quantity,
        )

        val resp = withTokenRetry(account) { token ->
            val accountSeq = accountRepo.resolveAccountSeq(account, token)
            service.createOrder(TossAccountRepository.accountHeaders(token, accountSeq), body)
        }

        if (!resp.isSuccessful) {
            error("토스 주문 오류: ${parseError(resp)}")
        }
        val orderBody = resp.body() ?: error("토스 주문 응답 없음: ${resp.code()}")
        logD("Toss order: orderId=${orderBody.orderId}, status=${orderBody.status}")
        OrderResult(success = true, ordNo = orderBody.orderId, message = orderBody.status)
    }.also { r -> r.onFailure { logE("TossOrderRepository error: ${it.message}") } }

    /**
     * 토큰 오류(HTTP 401: invalid-token / expired-token) 시 토큰을 강제 갱신하고 1회 재시도한다.
     */
    private suspend fun <T> withTokenRetry(
        account: BrokerAccount,
        block: suspend (token: String) -> Response<T>,
    ): Response<T> {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val resp = block(token)
        if (resp.code() != 401) return resp

        logW("TossOrderRepository: 토큰 오류(401) 감지 - 강제 갱신 후 재시도 accountId=${account.id}")
        tokenManager.invalidateToken(account.id)
        val newToken = tokenManager.refreshToken(account).getOrThrow()
        return block(newToken)
    }

    private fun parseError(resp: Response<*>): String {
        val raw = runCatching { resp.errorBody()?.string() }.getOrNull().orEmpty()
        val parsed = runCatching {
            json.decodeFromString(TossErrorResponse.serializer(), raw).error
        }.getOrNull()
        return parsed?.let { "${it.code} ${it.message}" } ?: "HTTP ${resp.code()}"
    }
}
