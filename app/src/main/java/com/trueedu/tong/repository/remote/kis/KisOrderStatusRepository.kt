package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.kis.KisFilledOrder
import com.trueedu.tong.model.dto.kis.KisUnfilledOrder
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.model.dto.order.RealizedPnlItem
import com.trueedu.tong.model.dto.order.RealizedPnlSummary
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Retrofit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisOrderStatusRepository @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val service by lazy { retrofit.create(KisOrderStatusService::class.java) }

    suspend fun getUnfilled(account: BrokerAccount): Result<List<KisUnfilledOrder>> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "TTTC8036R",
            "custtype" to "P",
        )
        val queries = mapOf(
            "CANO" to account.accountNum.take(8),
            "ACNT_PRDT_CD" to account.accountNum.drop(8),
            "CTX_AREA_FK100" to "",
            "CTX_AREA_NK100" to "",
            "INQR_DVSN_1" to "1",
            "INQR_DVSN_2" to "0",
        )
        val resp = service.getUnfilled(headers, queries)
        val body = resp.body() ?: error("미체결 조회 응답 없음")
        if (body.rtCd != "0") error("미체결 조회 오류: ${body.msg1}")
        body.orders
    }

    suspend fun getFilled(account: BrokerAccount): Result<List<KisFilledOrder>> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "TTTC8001R",
            "custtype" to "P",
        )
        val queries = mapOf(
            "CANO" to account.accountNum.take(8),
            "ACNT_PRDT_CD" to account.accountNum.drop(8),
            "INQR_STRT_DT" to today,
            "INQR_END_DT" to today,
            "SLL_BUY_DVSN_CD" to "00",
            "INQR_DVSN" to "00",
            "PDNO" to "",
            "ORD_GNO_BRNO" to "",
            "ODNO" to "",
            "INQR_DVSN_3" to "00",
            "INQR_DVSN_1" to "",
            "CTX_AREA_FK100" to "",
            "CTX_AREA_NK100" to "",
        )
        val resp = service.getFilled(headers, queries)
        val body = resp.body() ?: error("체결 조회 응답 없음")
        if (body.rtCd != "0") error("체결 조회 오류: ${body.msg1}")
        body.orders
    }

    suspend fun getRealizedPnl(account: BrokerAccount, startDate: String, endDate: String): Result<RealizedPnlSummary> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val baseHeaders = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "TTTC8715R",
            "custtype" to "P",
        )
        val allItems = mutableListOf<RealizedPnlItem>()
        var fk100 = ""
        var nk100 = ""
        var isFirstCall = true

        // KIS 연속조회 스펙:
        // - 1차: 요청 헤더 tr_cont="" (빈값)
        // - 응답 헤더 tr_cont="M"이면 다음 페이지 있음
        // - 2차~: 요청 헤더 tr_cont="N" + CTX_AREA_FK100/NK100에 응답값 전달
        while (true) {
            val headers = baseHeaders.toMutableMap().apply {
                this["tr_cont"] = if (isFirstCall) "" else "N"
            }
            val queries = mapOf(
                "CANO" to account.accountNum.take(8),
                "ACNT_PRDT_CD" to account.accountNum.drop(8),
                "INQR_STRT_DT" to startDate,
                "INQR_END_DT" to endDate,
                "PDNO" to "",
                "INQR_DVSN" to "00",
                "SORT_DVSN" to "00",
                "CBLC_DVSN" to "00",
                "EXCG_ID_DVSN_CD" to "",
                "CTX_AREA_FK100" to fk100,
                "CTX_AREA_NK100" to nk100,
            )
            val resp = service.getRealizedPnl(headers, queries)
            val body = resp.body() ?: break
            if (body.rtCd != "0") error("실현손익 조회 오류: ${body.msg1}")

            body.items.mapTo(allItems) { o ->
                val fee = o.fee.toLongOrNull() ?: 0L
                val tax = o.tax.toLongOrNull() ?: 0L
                val pnlBefore = o.pnlBeforeCost.toLongOrNull() ?: 0L
                val sellQty = o.sellQty.toLongOrNull() ?: 0L
                val sellAmt = o.sellAmt.toLongOrNull() ?: 0L
                val sellPrice = if (sellQty > 0) sellAmt / sellQty else o.sellPrice.toLongOrNull() ?: 0L
                RealizedPnlItem(
                    code = o.code,
                    name = o.name,
                    sellQty = sellQty,
                    sellPrice = sellPrice,
                    fee = fee,
                    tax = tax,
                    pnlBeforeCost = pnlBefore,
                    pnlAfterCost = pnlBefore - fee - tax,
                )
            }

            // 응답 헤더 tr_cont: "M"=다음 페이지, 그 외=마지막
            val trCont = resp.headers()["tr_cont"]?.trim() ?: ""
            if (trCont != "M") break

            fk100 = body.fk100
            nk100 = body.nk100
            isFirstCall = false
            kotlinx.coroutines.delay(300L) // 초당 3건 이하로 안전하게
        }

        RealizedPnlSummary(
            totalPnlBeforeCost = allItems.sumOf { it.pnlBeforeCost },
            totalPnlAfterCost = allItems.sumOf { it.pnlAfterCost },
            totalFee = allItems.sumOf { it.fee },
            totalTax = allItems.sumOf { it.tax },
            items = allItems,
        )
    }

    suspend fun cancel(account: BrokerAccount, orgNo: String, ordNo: String, code: String): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "TTTC0803U",
            "custtype" to "P",
        )
        val body = mapOf(
            "CANO" to account.accountNum.take(8),
            "ACNT_PRDT_CD" to account.accountNum.drop(8),
            "KRX_FWDG_ORD_ORGNO" to orgNo,
            "ORGN_ODNO" to ordNo,
            "ORD_DVSN" to "00",
            "RVSE_CNCL_DVSN_CD" to "02",  // 취소
            "ORD_QTY" to "0",
            "ORD_UNPR" to "0",
            "QTY_ALL_ORD_YN" to "Y",
        )
        val resp = service.modifyOrCancel(headers, body)
        val b = resp.body() ?: error("취소 응답 없음")
        if (b.rtCd != "0") error("취소 오류: ${b.msg1}")
        OrderResult(success = true, ordNo = b.output?.odno ?: "", message = b.msg1)
    }

    suspend fun modify(account: BrokerAccount, orgNo: String, ordNo: String, code: String, newPrice: Long, qty: Long): Result<OrderResult> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "TTTC0803U",
            "custtype" to "P",
        )
        val body = mapOf(
            "CANO" to account.accountNum.take(8),
            "ACNT_PRDT_CD" to account.accountNum.drop(8),
            "KRX_FWDG_ORD_ORGNO" to orgNo,
            "ORGN_ODNO" to ordNo,
            "ORD_DVSN" to "00",
            "RVSE_CNCL_DVSN_CD" to "01",  // 정정
            "ORD_QTY" to qty.toString(),
            "ORD_UNPR" to newPrice.toString(),
            "QTY_ALL_ORD_YN" to "N",
        )
        val resp = service.modifyOrCancel(headers, body)
        val b = resp.body() ?: error("정정 응답 없음")
        if (b.rtCd != "0") error("정정 오류: ${b.msg1}")
        OrderResult(success = true, ordNo = b.output?.odno ?: "", message = b.msg1)
    }
}
