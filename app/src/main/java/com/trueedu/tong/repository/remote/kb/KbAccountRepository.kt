package com.trueedu.tong.repository.remote.kb

import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.account.HoldingStock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KbAccountRepository @Inject constructor(
    private val client: KbApiClient,
) {
    /** SSQM2952 체결기준 잔고현황 (A: 통합시세) */
    suspend fun getAccountSummary(account: BrokerAccount): Result<AccountSummary> = runCatching {
        val data = client.call(account, "SSQM2952", mapOf("excg_mktpr_ccd" to "A")).getOrThrow()

        val holdings = data.rows("Record1")
            // 체결기준 반영 수량은 정산수량(ec_q). 보유수량(hld_q)은 결제 전 매수분이 0 으로 나온다
            .filter { (it.num("ec_q") ?: 0.0) > 0 }
            .map { h ->
                HoldingStock(
                    code = h.str("is_cd").removePrefix("A"),
                    name = h.str("is_nm"),
                    quantity = h.num("ec_q") ?: 0.0,
                    avgPrice = h.num("byng_avr_prc") ?: 0.0,
                    currentPrice = h.num("now_prc"),
                    evaluationAmount = h.num("val_amt") ?: 0.0,
                    profitAmount = h.num("val_pl") ?: 0.0,
                    profitRate = h.num("val_yld") ?: 0.0,
                )
            }

        AccountSummary(
            accountId = account.id,
            totalAsset = data.num("nt_asts_val_amt") ?: 0.0,
            deposit = data.num("dy_tfnd") ?: 0.0,
            depositD1 = data.num("ndy_tfnd"),
            depositD2 = data.num("nxt2_dy_tfnd"),
            totalProfitAmount = data.num("val_pl_sum") ?: 0.0,
            totalProfitRate = data.num("val_yld_sum") ?: 0.0,
            holdings = holdings,
        )
    }
}
