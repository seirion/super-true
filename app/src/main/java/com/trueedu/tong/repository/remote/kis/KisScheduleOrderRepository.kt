package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.order.ScheduleOrderItem
import com.trueedu.tong.model.dto.order.ScheduleOrderRequest
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.utils.scheduleEndDate
import com.trueedu.tong.utils.yyyyMMdd
import retrofit2.Retrofit
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KIS 국내주식 예약주문 레포지토리.
 *
 * 조회 CTSC0004R / 등록 CTSC0008U / 취소 CTSC0009U / 정정 CTSC0013U
 */
@Singleton
class KisScheduleOrderRepository @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val service by lazy { retrofit.create(KisScheduleOrderService::class.java) }

    private suspend fun headers(account: BrokerAccount, trId: String, trCont: String? = null): Map<String, String> {
        val token = tokenManager.getValidToken(account).getOrThrow()
        return buildMap {
            put("authorization", "Bearer $token")
            put("appkey", credentialStorage.getAppKey(account.id))
            put("appsecret", credentialStorage.getAppSecret(account.id))
            put("tr_id", trId)
            put("custtype", "P")
            if (trCont != null) put("tr_cont", trCont)
        }
    }

    private fun accountQueries(account: BrokerAccount) = mapOf(
        "CANO" to account.accountNum.take(8),
        "ACNT_PRDT_CD" to account.accountNum.drop(8),
    )

    /**
     * 예약주문 조회. 오늘부터 [monthsAhead] 개월 뒤까지의 유효한 예약을 가져온다.
     */
    suspend fun getScheduleOrders(
        account: BrokerAccount,
        monthsAhead: Long = 1,
    ): Result<List<ScheduleOrderItem>> = runCatching {
        val today = LocalDate.now()
        val queries = accountQueries(account) + mapOf(
            "RSVN_ORD_ORD_DT" to today.yyyyMMdd(),                    // 예약주문 시작일자
            "RSVN_ORD_END_DT" to today.plusMonths(monthsAhead).yyyyMMdd(), // 예약주문 종료일자
            "RSVN_ORD_SEQ" to "",
            "TMNL_MDIA_KIND_CD" to "00",  // 단말매체종류코드: "00" 고정
            "PRCS_DVSN_CD" to "0",        // 0: 전체, 1: 처리내역, 2: 미처리내역
            "CNCL_YN" to "Y",             // Y: 유효한 주문만 조회
            "PDNO" to "",                 // 공백이면 전체 종목
            "SLL_BUY_DVSN_CD" to "",      // 공백이면 매도/매수 전체
            "CTX_AREA_FK200" to "",
            "CTX_AREA_NK200" to "",
        )
        val resp = service.getScheduleOrders(headers(account, "CTSC0004R", trCont = ""), queries)
        val body = resp.body() ?: error("예약주문 조회 응답 없음")
        if (body.rtCd != "0") error("예약주문 조회 오류: ${body.msg1}")
        body.orders.map { o ->
            ScheduleOrderItem(
                seq = o.seq,
                code = o.code,
                name = o.name,
                isBuy = o.sellBuyDivisionCode.trim() == "02",
                price = o.price.trim().replace(",", "").toDoubleOrNull()?.toLong() ?: 0L,
                quantity = o.quantity.trim().replace(",", "").toLongOrNull() ?: 0L,
                filledQuantity = o.filledQuantity.trim().replace(",", "").toLongOrNull() ?: 0L,
                orderDate = o.orderDate,
                endDate = o.endDate,
                processResult = o.processResult,
                rejectReason = o.rejectReason,
            )
        }
    }

    /**
     * 예약주문 등록. 종료일자를 비우면 다음 영업일 1회만 처리되고 예약이 종료되므로
     * [scheduleEndDate] 로 유효기간을 채운다.
     */
    suspend fun placeScheduleOrder(
        account: BrokerAccount,
        request: ScheduleOrderRequest,
    ): Result<String> = runCatching {
        val body = accountQueries(account) + mapOf(
            "PDNO" to request.code.removePrefix("A"),
            "ORD_QTY" to request.quantity.toString(),
            "ORD_UNPR" to request.price.toString(),
            "SLL_BUY_DVSN_CD" to if (request.isBuy) "02" else "01",  // 01: 매도, 02: 매수
            "ORD_DVSN_CD" to "00",           // 00: 지정가
            "ORD_OBJT_CBLC_DVSN_CD" to "10", // 주문대상잔고구분: 현금
            "RSVN_ORD_END_DT" to scheduleEndDate(),
        )
        val resp = service.placeScheduleOrder(headers(account, "CTSC0008U"), body)
        val result = resp.body() ?: error("예약주문 등록 응답 없음")
        if (result.rtCd != "0") error(result.msg1?.ifBlank { null } ?: "예약주문 등록 실패 (${result.msgCd})")
        result.output?.seq ?: ""
    }

    /** 예약주문 취소 */
    suspend fun cancelScheduleOrder(
        account: BrokerAccount,
        seq: String,
    ): Result<Unit> = runCatching {
        val body = accountQueries(account) + mapOf("RSVN_ORD_SEQ" to seq)
        val resp = service.modifyOrCancelScheduleOrder(headers(account, "CTSC0009U"), body)
        val result = resp.body() ?: error("예약주문 취소 응답 없음")
        if (result.rtCd != "0") error(result.message.ifBlank { "예약주문 취소 실패 (${result.msgCd})" })
    }

    /** 예약주문 정정 (가격/수량) */
    suspend fun modifyScheduleOrder(
        account: BrokerAccount,
        seq: String,
        request: ScheduleOrderRequest,
    ): Result<Unit> = runCatching {
        val body = accountQueries(account) + mapOf(
            "PDNO" to request.code.removePrefix("A"),
            "ORD_QTY" to request.quantity.toString(),
            "ORD_UNPR" to request.price.toString(),
            "SLL_BUY_DVSN_CD" to if (request.isBuy) "02" else "01",
            "ORD_DVSN_CD" to "00",
            "ORD_OBJT_CBLC_DVSN_CD" to "10",
            "RSVN_ORD_SEQ" to seq,
        )
        val resp = service.modifyOrCancelScheduleOrder(headers(account, "CTSC0013U"), body)
        val result = resp.body() ?: error("예약주문 정정 응답 없음")
        if (result.rtCd != "0") error(result.message.ifBlank { "예약주문 정정 실패 (${result.msgCd})" })
    }
}
