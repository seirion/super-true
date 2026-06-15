package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.di.KiwoomRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.kiwoom.KiwoomFilledOrder
import com.trueedu.tong.model.dto.kiwoom.KiwoomUnfilledOrder
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.model.dto.order.RealizedPnlItem
import com.trueedu.tong.model.dto.order.RealizedPnlSummary
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

    suspend fun getRealizedPnl(account: BrokerAccount, startDate: String, endDate: String): Result<RealizedPnlSummary> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val body = mapOf(
            "acnt_no" to account.accountNum,
            "strt_dt" to startDate,
            "end_dt" to endDate,
            "stk_cd" to "",
        )
        // ka10073: 거래건별 상세 실현손익
        val detailResp = service.getRealizedPnl(authHeaders(account, "ka10073", token), body)
        val detail = detailResp.body() ?: error("실현손익 상세 응답 없음")
        if (detail.returnCode != 0) error("실현손익 조회 오류: ${detail.returnMsg}")

        // ka10074: 기간별 합계 (tot_pnl, trde_cmsn, trde_tax)
        val summaryResp = service.getRealizedPnl(authHeaders(account, "ka10074", token), body)
        val summary = summaryResp.body()

        val items = detail.items.map { o ->
            val fee = o.fee.toLongOrNull() ?: 0L
            val tax = o.tax.toLongOrNull() ?: 0L
            val pnlBefore = o.pnlBeforeCost.replace(",", "").toDoubleOrNull()?.toLong() ?: 0L
            RealizedPnlItem(
                code = o.code.removePrefix("A"),
                name = o.name,
                sellQty = o.sellQty.toLongOrNull() ?: 0L,
                sellPrice = o.sellPrice.replace(",", "").toLongOrNull() ?: 0L,
                fee = fee,
                tax = tax,
                pnlBeforeCost = pnlBefore,
                pnlAfterCost = pnlBefore - fee - tax,
            )
        }
        val totalPnlBefore = summary?.totalPnlBeforeCost?.replace(",", "")?.toLongOrNull()
            ?: items.sumOf { it.pnlBeforeCost }
        val totalFee = summary?.totalFee?.replace(",", "")?.toLongOrNull()
            ?: items.sumOf { it.fee }
        val totalTax = summary?.totalTax?.replace(",", "")?.toLongOrNull()
            ?: items.sumOf { it.tax }
        RealizedPnlSummary(
            totalPnlBeforeCost = totalPnlBefore,
            totalPnlAfterCost = totalPnlBefore - totalFee - totalTax,
            totalFee = totalFee,
            totalTax = totalTax,
            items = items,
        )
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
