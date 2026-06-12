package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.di.LsRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.account.HoldingStock
import com.trueedu.tong.repository.local.CredentialStorage
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LsAccountRepository @Inject constructor(
    @LsRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
) {
    private val service: LsAccountService by lazy {
        retrofit.create(LsAccountService::class.java)
    }

    suspend fun getAccountSummary(
        account: BrokerAccount,
        accessToken: String,
    ): Result<AccountSummary> = runCatching {
        // t0424 - 잔고/보유종목
        val balanceHeaders = mapOf(
            "authorization" to "Bearer $accessToken",
            "tr_cd" to "t0424",
            "tr_cont" to "N",
            "tr_cont_key" to "",
            "mac_address" to "",
        )
        // LS 는 nested body 구조 (tr_cd 별 InBlock 으로 감싼다)
        val balanceBody = mapOf<String, Any>(
            "t0424InBlock" to mapOf(
                "prcgb" to "1",
                "chegb" to "0",
                "sortgb" to "1",
                "cts_expcode" to "",
            )
        )
        val balanceResp = service.getBalance(balanceHeaders, balanceBody)
        val balanceData = balanceResp.body() ?: error("LS 잔고 응답 없음")

        // CSPAQ12200 - 예수금/총평가
        val depositHeaders = mapOf(
            "authorization" to "Bearer $accessToken",
            "tr_cd" to "CSPAQ12200",
            "tr_cont" to "N",
            "tr_cont_key" to "",
            "mac_address" to "",
        )
        val depositBody = mapOf<String, Any>(
            "CSPAQ12200InBlock1" to mapOf(
                "RecCnt" to "1",
                "AcntNo" to account.accountNum,
                "Pwd" to "",
                "BalCreTp" to "0",
            )
        )
        val depositResp = service.getDeposit(depositHeaders, depositBody)
        val depositData = depositResp.body()

        val summary = balanceData.summary
        val holdings = balanceData.holdings
            .filter { it.quantity.toDoubleOrNull()?.let { q -> q > 0 } == true }
            .map { h ->
                HoldingStock(
                    code = h.code,
                    name = h.name,
                    quantity = h.quantity.toLongOrNull() ?: 0L,
                    avgPrice = h.avgPrice.toDoubleOrNull() ?: 0.0,
                    currentPrice = null,  // t0424에 현재가 별도 없음
                    evaluationAmount = h.evalAmount.toDoubleOrNull() ?: 0.0,
                    profitAmount = h.profitAmount.toDoubleOrNull() ?: 0.0,
                    profitRate = h.profitRate.toDoubleOrNull() ?: 0.0,
                )
            }

        val totalBuy = summary?.totalBuyAmount?.toDoubleOrNull() ?: 0.0
        val totalEval = summary?.totalEvalAmount?.toDoubleOrNull() ?: 0.0
        val profitTotal = summary?.profitTotal?.toDoubleOrNull() ?: 0.0
        val profitRate = if (totalBuy > 0) profitTotal / totalBuy * 100 else 0.0
        val totalAsset = (depositData?.deposit?.depositD0?.toDoubleOrNull()
            ?: summary?.deposit?.toDoubleOrNull() ?: 0.0) + totalEval

        AccountSummary(
            accountId = account.id,
            totalAsset = totalAsset,
            deposit = summary?.deposit?.toDoubleOrNull()
                ?: depositData?.deposit?.depositD0?.toDoubleOrNull() ?: 0.0,
            depositD1 = depositData?.deposit?.depositD1?.toDoubleOrNull(),
            depositD2 = depositData?.deposit?.depositD2?.toDoubleOrNull(),
            totalProfitAmount = profitTotal,
            totalProfitRate = profitRate,
            holdings = holdings,
        )
    }
}
