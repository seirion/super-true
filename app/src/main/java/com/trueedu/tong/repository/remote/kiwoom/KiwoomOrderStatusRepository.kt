package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.di.KiwoomRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.kiwoom.KiwoomFilledOrder
import com.trueedu.tong.model.dto.kiwoom.KiwoomUnfilledOrder
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KiwoomOrderStatusRepository @Inject constructor(
    @KiwoomRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val service by lazy { retrofit.create(KiwoomOrderStatusService::class.java) }

    private fun authHeaders(account: BrokerAccount, apiId: String, token: String) = mapOf(
        "authorization" to "Bearer $token",
        "api-id" to apiId,
        "cont-yn" to "N",
        "next-key" to "",
    )

    suspend fun getUnfilled(account: BrokerAccount): Result<List<KiwoomUnfilledOrder>> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val headers = authHeaders(account, "ka10075", token)
        val body = mapOf(
            "all_stk_tp" to "0",  // 0:전체 1:종목
            "trde_tp" to "0",     // 0:전체 1:매도 2:매수
            "stex_tp" to "0",     // 0:통합 1:KRX 2:NXT
        )
        val resp = service.getUnfilled(headers, body)
        val b = resp.body() ?: error("미체결 응답 없음")
        if (b.returnCode != 0) error("미체결 조회 오류: ${b.returnMsg}")
        b.orders
    }

    suspend fun getFilled(account: BrokerAccount): Result<List<KiwoomFilledOrder>> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val headers = authHeaders(account, "ka10076", token)
        val body = mapOf(
            "qry_tp" to "0",      // 0:전체 1:종목
            "sell_tp" to "0",     // 0:전체 1:매도 2:매수
            "stex_tp" to "0",     // 0:통합 1:KRX 2:NXT
        )
        val resp = service.getFilled(headers, body)
        val b = resp.body() ?: error("체결 응답 없음")
        if (b.returnCode != 0) error("체결 조회 오류: ${b.returnMsg}")
        b.orders
    }

    suspend fun cancel(account: BrokerAccount, ordNo: String, code: String, stexTp: String = "KRX"): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val headers = authHeaders(account, "kt10003", token)
        val body = mapOf(
            "dmst_stex_tp" to stexTp,        // 원주문과 동일한 거래소
            "orig_ord_no" to ordNo,
            "stk_cd" to code.removePrefix("A"),
            "cncl_qty" to "0",               // 0=전량 취소
        )
        val resp = service.modifyOrCancel(headers, body)
        val b = resp.body() ?: error("취소 응답 없음")
        if (b.returnCode != 0) error("취소 오류: ${b.returnMsg}")
        OrderResult(success = true, ordNo = b.ordNo, message = b.returnMsg)
    }

    suspend fun modify(account: BrokerAccount, ordNo: String, code: String, newPrice: Long, qty: Long, stexTp: String = "KRX"): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val headers = authHeaders(account, "kt10002", token)
        val body = mapOf(
            "dmst_stex_tp" to stexTp,        // 원주문과 동일한 거래소
            "orig_ord_no" to ordNo,
            "stk_cd" to code.removePrefix("A"),
            "mdfy_qty" to qty.toString(),    // 정정수량
            "mdfy_uv" to newPrice.toString(), // 정정단가
            "mdfy_cond_uv" to "",
        )
        val resp = service.modifyOrCancel(headers, body)
        val b = resp.body() ?: error("정정 응답 없음")
        if (b.returnCode != 0) error("정정 오류: ${b.returnMsg}")
        OrderResult(success = true, ordNo = b.ordNo, message = b.returnMsg)
    }
}
