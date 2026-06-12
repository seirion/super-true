package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.di.LsRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.account.HoldingStock
import com.trueedu.tong.model.dto.ls.LsBalanceInBlock
import com.trueedu.tong.model.dto.ls.LsBalanceRequest
import com.trueedu.tong.model.dto.ls.LsDepositInBlock
import com.trueedu.tong.model.dto.ls.LsDepositRequest
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Retrofit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LsAccountRepository @Inject constructor(
    @LsRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val service: LsAccountService by lazy {
        retrofit.create(LsAccountService::class.java)
    }

    suspend fun getAccountSummary(
        account: BrokerAccount,
        accessToken: String,
    ): Result<AccountSummary> = runCatching {
        val commonHeaders = mapOf(
            "authorization" to "Bearer $accessToken",
            "tr_cont" to "N",
            "tr_cont_key" to "",
            "mac_address" to "",
        )

        // t0424 - 잔고/보유종목
        val balanceResp = service.getBalance(
            commonHeaders + mapOf("tr_cd" to "t0424"),
            LsBalanceRequest(LsBalanceInBlock()),
        )
        val balanceData = balanceResp.body() ?: error("LS 잔고 응답 없음")
        Timber.d("LS t0424: rsp_cd=${balanceData.rspCd}, holdings=${balanceData.holdings.size}")
        // LS rsp_cd 성공: "00000"
        if (balanceData.rspCd.isNotBlank() && balanceData.rspCd != "00000") {
            error("LS 잔고 오류: ${balanceData.rspMsg}")
        }

        // CSPAQ12200 - 예수금/총평가
        val depositResp = service.getDeposit(
            commonHeaders + mapOf("tr_cd" to "CSPAQ12200"),
            LsDepositRequest(LsDepositInBlock(acntNo = account.accountNum)),
        )
        val depositData = depositResp.body()
        Timber.d("LS CSPAQ12200: rsp_cd=${depositData?.rspCd}")

        val summary = balanceData.summary
        val holdings = balanceData.holdings
            .filter { it.quantity > 0 }
            .map { h ->
                HoldingStock(
                    code = h.code,
                    name = h.name,
                    quantity = h.quantity,
                    avgPrice = h.avgPrice.toDouble(),
                    currentPrice = null,
                    evaluationAmount = h.evalAmount.toDouble(),
                    profitAmount = h.profitAmount.toDouble(),
                    profitRate = h.profitRate,
                )
            }

        val totalBuy = summary?.totalBuyAmount?.toDouble() ?: 0.0
        val totalEval = summary?.totalEvalAmount?.toDouble() ?: 0.0
        val profitTotal = summary?.profitTotal?.toDouble() ?: 0.0
        val profitRate = if (totalBuy > 0) profitTotal / totalBuy * 100 else 0.0
        val depositD0 = depositData?.deposit?.depositD0?.toDouble()
            ?: summary?.deposit?.toDouble() ?: 0.0
        val totalAsset = depositD0 + totalEval

        AccountSummary(
            accountId = account.id,
            totalAsset = totalAsset,
            deposit = depositD0,
            depositD1 = depositData?.deposit?.depositD1?.toDouble(),
            depositD2 = depositData?.deposit?.depositD2?.toDouble(),
            totalProfitAmount = profitTotal,
            totalProfitRate = profitRate,
            holdings = holdings,
        )
    }
}
