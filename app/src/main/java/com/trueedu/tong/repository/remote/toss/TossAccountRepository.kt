package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.di.TossRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.account.HoldingStock
import com.trueedu.tong.repository.remote.auth.TokenManager
import retrofit2.Retrofit
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import com.trueedu.tong.utils.logW
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class TossAccountRepository @Inject constructor(
    @TossRetrofitQualifier private val retrofit: Retrofit,
    private val tokenManager: TokenManager,
) {
    private val service: TossAccountService by lazy {
        retrofit.create(TossAccountService::class.java)
    }

    /**
     * 계좌 목록을 조회해 거래에 사용할 accountSeq 를 결정한다.
     * account.accountNum 과 일치하는 계좌를 우선 사용하고, 없으면 첫 번째 계좌를 사용한다.
     */
    suspend fun resolveAccountSeq(account: BrokerAccount, accessToken: String): String {
        val resp = service.getAccounts(authHeaders(accessToken))
        val body = resp.body() ?: error("토스 계좌목록 응답 없음: ${resp.code()}")
        val accounts = body.data
        if (accounts.isEmpty()) error("토스 계좌목록 없음")
        val matched = accounts.firstOrNull { it.accountNumber == account.accountNum }
            ?: accounts.first()
        logD("Toss accounts: count=${accounts.size}, accountSeq=${matched.accountSeq}")
        return matched.accountSeq
    }

    suspend fun getAccountSummary(
        account: BrokerAccount,
        accessToken: String,
    ): Result<AccountSummary> = runCatching {
        val accountSeq = resolveAccountSeq(account, accessToken)

        val holdingsResp = service.getHoldings(accountHeaders(accessToken, accountSeq))
        val holdingsBody = holdingsResp.body() ?: error("토스 잔고 응답 없음: ${holdingsResp.code()}")
        logD("Toss holdings: count=${holdingsBody.holdings.size}")

        val holdings = holdingsBody.holdings
            .filter { (long(it.quantity) ?: 0L) > 0L }
            .map { h ->
                HoldingStock(
                    code = h.symbol,
                    name = h.name,
                    quantity = long(h.quantity) ?: 0L,
                    avgPrice = num(h.averagePrice) ?: 0.0,
                    currentPrice = num(h.currentPrice),
                    evaluationAmount = num(h.marketValue) ?: 0.0,
                    profitAmount = num(h.profitLoss) ?: 0.0,
                    profitRate = num(h.profitLossRate) ?: 0.0,
                )
            }

        val overview = holdingsBody.overview
        val totalEval = num(overview?.totalMarketValue) ?: holdings.sumOf { it.evaluationAmount }
        val profitTotal = num(overview?.totalProfitLoss) ?: holdings.sumOf { it.profitAmount }
        val profitRate = num(overview?.totalProfitLossRate)
            ?: (if (totalEval - profitTotal > 0) profitTotal / (totalEval - profitTotal) * 100 else 0.0)

        AccountSummary(
            accountId = account.id,
            totalAsset = totalEval,
            deposit = 0.0,   // 토스 holdings API 는 예수금을 제공하지 않음
            depositD1 = null,
            depositD2 = null,
            totalProfitAmount = profitTotal,
            totalProfitRate = profitRate,
            holdings = holdings,
        )
    }.also { r -> r.onFailure { logE("TossAccountRepository error: ${it.message}") } }

    companion object {
        fun authHeaders(accessToken: String): Map<String, String> = mapOf(
            "Authorization" to "Bearer $accessToken",
        )

        fun accountHeaders(accessToken: String, accountSeq: String): Map<String, String> = mapOf(
            "Authorization" to "Bearer $accessToken",
            "X-Tossinvest-Account" to accountSeq,
        )

        /** 부호/콤마 제거 후 Double. "+1,200" -> 1200.0, "" -> null */
        fun num(s: String?): Double? =
            s?.trim()?.replace(",", "")?.replace("+", "")?.takeIf { it.isNotBlank() }?.toDoubleOrNull()

        fun long(s: String?): Long? =
            s?.trim()?.replace(",", "")?.replace("+", "")?.takeIf { it.isNotBlank() }
                ?.toDoubleOrNull()?.let { abs(it).toLong() }
    }
}
