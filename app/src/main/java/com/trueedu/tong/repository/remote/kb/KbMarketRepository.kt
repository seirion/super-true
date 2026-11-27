package com.trueedu.tong.repository.remote.kb

import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.CandleData
import com.trueedu.tong.model.CandlePeriod
import com.trueedu.tong.model.StockInfo
import com.trueedu.tong.model.dto.kb.KbFields
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class KbMarketRepository @Inject constructor(
    private val client: KbApiClient,
) {
    // 종목의 시장(KOSPI/KOSDAQ)은 바뀌지 않으므로 통합차트 요청용으로 한 번만 조회한다
    private val kosdaqCache = ConcurrentHashMap<String, Boolean>()

    /** IVU10140 주식현재가 (거래소 0:통합) */
    private suspend fun fetchQuote(account: BrokerAccount, code: String): KbFields =
        client.call(account, "IVU10140", mapOf("excg_clsf" to "0", "shrt_cd" to code)).getOrThrow()

    suspend fun getStockInfo(account: BrokerAccount, code: String): Result<StockInfo> = runCatching {
        val q = fetchQuote(account, code)
        kosdaqCache[code] = isKosdaq(q)

        // 전일대비 구분코드: 1 상한, 2 상승, 3 보합, 4 하한, 5 하락. 값 자체는 부호 없이 내려온다
        val sign = if (q.str("bdy_cmpr_ccd") in setOf("4", "5")) -1.0 else 1.0

        StockInfo(
            code = code,
            name = q.str("is_nm"),
            currentPrice = q.num("now_prc") ?: 0.0,
            delta = abs(q.num("bdy_cmpr") ?: 0.0) * sign,
            rate = abs(q.num("up_dwn_r_p2") ?: 0.0) * sign,
            open = q.num("opn_prc") ?: 0.0,
            high = q.num("hgh_prc") ?: 0.0,
            low = q.num("lw_prc") ?: 0.0,
            volume = q.long("acml_vlm") ?: 0L,
            marketCap = q.num("opn_prc_tl_amt"),
            per = q.num("per_p2"),
            pbr = q.num("pbr_p2"),
            eps = q.num("eps_p2"),
            bps = null,
            roe = q.num("bfr_yr_clsac_roe_p2"),
            ev = null,
            salesAmount = q.num("bfr_yr_clsac_sale_amt"),
            operatingProfit = q.num("bfr_yr_clsac_bsnss_prft"),
            netProfit = q.num("bfr_yr_clsac_nt_prft"),
            parValue = q.num("pr_vl_prc_p3"),
            // 자본금은 백만원 단위로 내려온다
            capital = q.num("cptl_amt")?.div(100),
            listedShares = q.long("lstng_stk_q"),
            settlementMonth = q.str("clsac_mm").ifBlank { null },
            week52High = positiveOrNull(q.num("dy250_max_prc")),
            week52Low = positiveOrNull(q.num("dy250_min_prc")),
            upperLimit = positiveOrNull(q.num("ulmt_prc")),
            lowerLimit = positiveOrNull(q.num("llmt_prc")),
            foreignExhaustionRate = q.num("lmt_vnsh_r_p2"),
            creditRate = q.num("crdt_blnc_rt_p2"),
        )
    }

    /** IVS11560 통합차트. 응답 순서와 무관하게 최신 → 과거 순으로 돌려준다 */
    suspend fun getCandles(
        account: BrokerAccount,
        code: String,
        period: CandlePeriod,
        minuteInterval: Int,
    ): Result<List<CandleData>> = runCatching {
        val kosdaq = kosdaqCache[code] ?: isKosdaq(fetchQuote(account, code)).also { kosdaqCache[code] = it }
        val data = client.call(
            account,
            "IVS11560",
            mapOf(
                "info_ccd" to "1",
                "mkt_clsf" to if (kosdaq) "1" else "0",
                "chrt_clsf" to when (period) {
                    CandlePeriod.MINUTE -> "B"
                    CandlePeriod.DAY -> "D"
                    CandlePeriod.WEEK -> "W"
                    CandlePeriod.MONTH -> "M"
                },
                "minute_tck_indx" to if (period == CandlePeriod.MINUTE) minuteInterval.toString() else "1",
                "is_cd" to code,
                "inq_clsf" to "2", // 데이터 수로 조회
                "inq_cnt" to CANDLE_COUNT.toString(),
            ),
        ).getOrThrow()

        data.rows("out2")
            .map { c ->
                CandleData(
                    datetime = c.str("dt") + c.str("tm").take(4),
                    open = c.num("opn_prc_p2") ?: 0.0,
                    high = c.num("hgh_prc_p2") ?: 0.0,
                    low = c.num("lw_prc_p2") ?: 0.0,
                    close = c.num("cls_prc_p2") ?: 0.0,
                    volume = c.long("vlm") ?: 0L,
                )
            }
            .sortedByDescending { it.datetime }
    }

    private fun isKosdaq(quote: KbFields): Boolean = quote.str("mkt_clsf_nm").contains("KOSDAQ", ignoreCase = true)

    private fun positiveOrNull(v: Double?): Double? = v?.takeIf { it > 0.0 }

    private companion object {
        const val CANDLE_COUNT = 200
    }
}
