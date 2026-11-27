package com.trueedu.tong.repository.remote.kb

import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.kb.KbFields
import com.trueedu.tong.model.dto.order.FilledOrderItem
import com.trueedu.tong.model.dto.order.OrderResult
import com.trueedu.tong.model.dto.order.RealizedPnlItem
import com.trueedu.tong.model.dto.order.RealizedPnlSummary
import com.trueedu.tong.model.dto.order.UnfilledOrderItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KbOrderStatusRepository @Inject constructor(
    private val client: KbApiClient,
) {
    /** SSQM2341 체결미체결 조회 (ccls_clsf 0:전체 1:체결 2:미체결, 당일) */
    private suspend fun fetchOrders(account: BrokerAccount, cclsClsf: String): List<KbFields> {
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return client.callPaged(
            account,
            "SSQM2341",
            mapOf("ccls_clsf" to cclsClsf, "ordr_dt" to today),
            rowsKey = "Record1",
        ).getOrThrow()
    }

    suspend fun getUnfilled(account: BrokerAccount): Result<List<UnfilledOrderItem>> = runCatching {
        fetchOrders(account, "2")
            .filter { (it.long("nccls_q") ?: 0L) > 0L }
            .map { o ->
                UnfilledOrderItem(
                    ordNo = o.str("ordr_no"),
                    code = o.str("stnd_is_no").removePrefix("A"),
                    name = o.str("hngl_shrt_nm"),
                    isBuy = o.str("trd_dl_ccd_nm").contains("매수"),
                    ordPrice = o.long("ordr_uprc") ?: 0L,
                    ordQty = o.long("ordr_q") ?: 0L,
                    filledQty = o.long("tl_ccls_q") ?: 0L,
                    remainQty = o.long("nccls_q") ?: 0L,
                    ordTime = o.str("ordr_tm"),
                    orgNo = o.str("orgn_ordr_no"),
                    stexTp = KbExchange.fromCode(o.str("sor_ordr_ccd")),
                )
            }
    }

    suspend fun getFilled(account: BrokerAccount): Result<List<FilledOrderItem>> = runCatching {
        fetchOrders(account, "1")
            .filter { (it.long("tl_ccls_q") ?: 0L) > 0L }
            .map { o ->
                FilledOrderItem(
                    code = o.str("stnd_is_no").removePrefix("A"),
                    name = o.str("hngl_shrt_nm"),
                    isBuy = o.str("trd_dl_ccd_nm").contains("매수"),
                    filledPrice = o.long("ccls_uprc") ?: 0L,
                    filledQty = o.long("tl_ccls_q") ?: 0L,
                    filledTime = o.str("ordr_tm"),
                )
            }
    }

    /** SSQM2442 실현손익현황 */
    suspend fun getRealizedPnl(account: BrokerAccount, startDate: String, endDate: String): Result<RealizedPnlSummary> = runCatching {
        val rows = client.callPaged(
            account,
            "SSQM2442",
            mapOf("inq_strt_dt" to startDate, "inq_end_dt" to endDate, "is_cd" to ""),
            rowsKey = "Record1",
        ).getOrThrow()

        val items = rows
            .filter { (it.long("s_amt") ?: 0L) > 0L }
            .map { r ->
                val fee = r.long("fee") ?: 0L
                val tax = r.long("svrl_tx") ?: 0L
                val pnl = r.long("rlztn_pl") ?: 0L
                RealizedPnlItem(
                    code = r.str("shrt_is_cd").removePrefix("A"),
                    name = r.str("is_nm"),
                    sellQty = r.long("ccls_q") ?: 0L,
                    sellPrice = r.long("ccls_uprc") ?: 0L,
                    fee = fee,
                    tax = tax,
                    pnlBeforeCost = pnl + fee + tax,
                    pnlAfterCost = pnl,
                    totalSellAmount = r.long("s_amt") ?: 0L,
                )
            }
        RealizedPnlSummary(
            totalPnlBeforeCost = items.sumOf { it.pnlBeforeCost },
            totalPnlAfterCost = items.sumOf { it.pnlAfterCost },
            totalFee = items.sumOf { it.fee },
            totalTax = items.sumOf { it.tax },
            items = items,
        )
    }

    /** SSAM1806 취소주문 (전부취소) */
    suspend fun cancel(account: BrokerAccount, ordNo: String, code: String): Result<OrderResult> = runCatching {
        val data = client.call(
            account,
            "SSAM1806",
            mapOf(
                "is_cd" to code,
                "ordr_q" to "",
                "crct_clsf" to "2",
                "orgn_ordr_no" to ordNo,
            ),
        ).getOrThrow()
        OrderResult(success = true, ordNo = data.str("ordr_no"), message = data.str("o_msg"))
    }

    /** SSAM1805 정정주문 (남은 수량 전부 지정가로 정정) */
    suspend fun modify(
        account: BrokerAccount,
        ordNo: String,
        code: String,
        newPrice: Long,
        stexTp: String,
    ): Result<OrderResult> = runCatching {
        val data = client.call(
            account,
            "SSAM1805",
            mapOf(
                "mkt_tm_clsf" to "1",
                "is_cd" to code,
                "ordr_q" to "",
                "ordr_uprc" to newPrice.toString(),
                "ordr_ccd" to "00",
                "crct_clsf" to "2",
                "orgn_ordr_no" to ordNo,
                "sor_ordr_ccd" to KbExchange.toCode(stexTp),
            ),
        ).getOrThrow()
        OrderResult(success = true, ordNo = data.str("ordr_no"), message = data.str("o_msg"))
    }
}
