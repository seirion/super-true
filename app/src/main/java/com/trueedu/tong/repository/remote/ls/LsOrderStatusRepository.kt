package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.di.LsRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.ls.LsCancelInBlock1
import com.trueedu.tong.model.dto.ls.LsCancelRequest
import com.trueedu.tong.model.dto.ls.LsModifyInBlock1
import com.trueedu.tong.model.dto.ls.LsModifyRequest
import com.trueedu.tong.model.dto.ls.LsOrderStatusInBlock
import com.trueedu.tong.model.dto.ls.LsOrderStatusItem
import com.trueedu.tong.model.dto.ls.LsOrderStatusRequest
import com.trueedu.tong.model.dto.order.FilledOrderItem
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.model.dto.order.UnfilledOrderItem
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LsOrderStatusRepository @Inject constructor(
    @LsRetrofitQualifier private val retrofit: Retrofit,
    private val tokenManager: TokenManager,
) {
    private val service by lazy { retrofit.create(LsOrderStatusService::class.java) }

    private fun commonHeaders(token: String, trCd: String) = mapOf(
        "authorization" to "Bearer $token",
        "tr_cd" to trCd,
        "tr_cont" to "N",
        "tr_cont_key" to "",
        "mac_address" to "",
    )

    suspend fun getUnfilled(account: BrokerAccount): Result<List<UnfilledOrderItem>> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val resp = service.getOrders(
            commonHeaders(token, "t0425"),
            LsOrderStatusRequest(LsOrderStatusInBlock(chegb = "2")),
        )
        val body = resp.body() ?: error("LS 미체결 응답 없음")
        if (body.rspCd != "00000") error("LS 미체결 오류: ${body.rspMsg}")
        body.orders.map { it.toUnfilled() }
    }

    suspend fun getFilled(account: BrokerAccount): Result<List<FilledOrderItem>> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val resp = service.getOrders(
            commonHeaders(token, "t0425"),
            LsOrderStatusRequest(LsOrderStatusInBlock(chegb = "1")),
        )
        val body = resp.body() ?: error("LS 체결 응답 없음")
        if (body.rspCd != "00000") error("LS 체결 오류: ${body.rspMsg}")
        body.orders.map { it.toFilled() }
    }

    suspend fun cancel(account: BrokerAccount, ordNo: String, code: String): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        // 미체결 조회해서 잔량 파악
        val unfilledList = getUnfilled(account).getOrDefault(emptyList())
        val item = unfilledList.firstOrNull { it.ordNo == ordNo }
        val qty = item?.remainQty ?: 0L
        val resp = service.cancel(
            commonHeaders(token, "CSPAT00801"),
            LsCancelRequest(LsCancelInBlock1(
                orgOrdNo = ordNo.toLongOrNull() ?: 0L,
                isuNo = "A$code",
                ordQty = qty,
            )),
        )
        val body = resp.body() ?: error("LS 취소 응답 없음")
        if (body.rspCd != "00000") error("LS 취소 오류: ${body.rspMsg}")
        OrderResult(success = true, message = body.rspMsg)
    }

    suspend fun modify(account: BrokerAccount, ordNo: String, code: String, newPrice: Long, qty: Long): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val resp = service.modify(
            commonHeaders(token, "CSPAT00701"),
            LsModifyRequest(LsModifyInBlock1(
                orgOrdNo = ordNo.toLongOrNull() ?: 0L,
                isuNo = "A$code",
                ordQty = qty,
                ordPrc = newPrice.toDouble(),
            )),
        )
        val body = resp.body() ?: error("LS 정정 응답 없음")
        if (body.rspCd != "00000") error("LS 정정 오류: ${body.rspMsg}")
        OrderResult(success = true, message = body.rspMsg)
    }
}

private fun LsOrderStatusItem.toUnfilled() = UnfilledOrderItem(
    ordNo = ordNo.toString(),
    code = code,
    name = name,
    isBuy = medosu == "2",
    ordPrice = price,
    ordQty = qty,
    filledQty = filledQty,
    remainQty = remainQty,
    ordTime = ordTime,
    orgNo = orgOrdNo.toString(),
)

private fun LsOrderStatusItem.toFilled() = FilledOrderItem(
    code = code,
    name = name,
    isBuy = medosu == "2",
    filledPrice = currentPrice,
    filledQty = filledQty,
    filledTime = ordTime,
)
