package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.di.KiwoomRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.account.HoldingStock
import com.trueedu.tong.repository.local.CredentialStorage
import retrofit2.Retrofit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KiwoomAccountRepository @Inject constructor(
    @KiwoomRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
) {
    private val service: KiwoomAccountService by lazy {
        retrofit.create(KiwoomAccountService::class.java)
    }

    suspend fun getAccountSummary(
        account: BrokerAccount,
        accessToken: String,
    ): Result<AccountSummary> = runCatching {
        Timber.d("KiwoomAccountRepository: getAccountSummary 시작 - accountId=${account.id}")
        // kt00018 - 잔고/보유종목
        val balanceHeaders = mapOf(
            "authorization" to "Bearer $accessToken",
            "api-id" to "kt00018",
            "cont-yn" to "N",
            "next-key" to "",
        )
        val balanceBody = mapOf(
            "acnt_no" to account.accountNum,
            "acnt_pw" to credentialStorage.getPassword(account.id),
            "dmst_stex_tp" to "KRX",     // 국내거래소구분: KRX
            "inqr_tp_code" to "0",       // 조회구분: 전체
            "hist_dt" to "",
            "stk_cd" to "",
            "etf_tp_code" to "0",
            "blnc_tp_code" to "0",
            "ccld_tp_code" to "0",
            "ord_gb_code" to "0",
            "prdt_cd" to "0",
            "inqr_cond_tp_code" to "0",
            "inqr_sort_tp_code" to "0",
        )
        val balanceResp = service.getBalance(balanceHeaders, balanceBody)
        Timber.d("KiwoomAccountRepository: kt00018 응답코드=${balanceResp.code()}, body=${balanceResp.body()}, error=${balanceResp.errorBody()?.string()}")
        val balanceBody2 = balanceResp.body() ?: error("키움 잔고 응답 없음")

        // kt00001 - 예수금
        val depositHeaders = mapOf(
            "authorization" to "Bearer $accessToken",
            "api-id" to "kt00001",
            "cont-yn" to "N",
            "next-key" to "",
        )
        val depositBody = mapOf(
            "acnt_no" to account.accountNum,
            "acnt_prdt_cd" to "01",
            "base_dt" to "",
            "qry_tp" to "0",
        )
        val depositResp = service.getDeposit(depositHeaders, depositBody)
        val depositData = depositResp.body() ?: error("키움 예수금 응답 없음")

        Timber.d("KiwoomAccountRepository: kt00018 returnCode=${balanceBody2.returnCode}, holdings=${balanceBody2.holdings.size}개, summary=${balanceBody2.summary}")
        val summary = balanceBody2.summary
        val holdings = balanceBody2.holdings
            .filter { it.quantity.toDoubleOrNull()?.let { q -> q > 0 } == true }
            .map { h ->
                HoldingStock(
                    code = h.code,
                    name = h.name,
                    quantity = h.quantity.toLongOrNull() ?: 0L,
                    avgPrice = h.avgPrice.toDoubleOrNull() ?: 0.0,
                    currentPrice = h.currentPrice.toDoubleOrNull(),
                    evaluationAmount = h.evalAmount.toDoubleOrNull() ?: 0.0,
                    profitAmount = h.profitAmount.toDoubleOrNull() ?: 0.0,
                    profitRate = h.profitRate.toDoubleOrNull() ?: 0.0,
                )
            }

        val totalBuy = summary?.totalBuyAmount?.toDoubleOrNull() ?: 0.0
        val totalAsset = summary?.totalAsset?.toDoubleOrNull() ?: 0.0
        val profitTotal = summary?.profitLossTotal?.toDoubleOrNull() ?: 0.0
        val profitRate = if (totalBuy > 0) profitTotal / totalBuy * 100 else 0.0

        AccountSummary(
            accountId = account.id,
            totalAsset = totalAsset,
            deposit = depositData.deposit.toDoubleOrNull() ?: 0.0,
            depositD1 = depositData.depositD1.toDoubleOrNull(),
            depositD2 = depositData.depositD2.toDoubleOrNull(),
            totalProfitAmount = profitTotal,
            totalProfitRate = profitRate,
            holdings = holdings,
        )
    }
}
