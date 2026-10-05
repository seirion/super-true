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
        logD("Toss accounts: count=${accounts.size}, accountSeq=${matched.accountSeqStr}")
        return matched.accountSeqStr
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
            .filter { (num(it.quantity) ?: 0.0) > 0.0 }
            .map { h ->
                HoldingStock(
                    code = h.symbol,
                    name = h.name,
                    quantity = num(h.quantity) ?: 0.0,
                    avgPrice = num(h.averagePurchasePrice) ?: 0.0,
                    currentPrice = num(h.lastPrice),
                    evaluationAmount = num(h.marketValue?.amount) ?: 0.0,
                    profitAmount = num(h.profitLoss?.amount) ?: 0.0,
                    profitRate = (num(h.profitLoss?.rate) ?: 0.0) * 100,
                    currency = h.currency.ifBlank { "KRW" },
                    prevClose = prevClose(h),
                )
            }

        // 미국 주식이 있을 때만 환율 조회. 실패하면 원화 종목만 합계에 반영된다
        val usdKrwRate = if (holdings.any { it.isUsd }) fetchUsdKrwRate(accessToken) else null

        val overview = holdingsBody.overview
        // 합계는 원화 기준: 국내 합계 + 해외(USD) 합계 × 환율
        val usdFactor = usdKrwRate ?: 0.0
        fun krwTotal(krw: String?, usd: String?, itemSum: (HoldingStock) -> Double): Double {
            val krwPart = num(krw) ?: holdings.filter { !it.isUsd }.sumOf(itemSum)
            val usdPart = num(usd) ?: holdings.filter { it.isUsd }.sumOf(itemSum)
            return krwPart + usdPart * usdFactor
        }
        val totalEval = krwTotal(overview?.marketValueKrw, overview?.marketValueUsd) { it.evaluationAmount }
        val profitTotal = krwTotal(overview?.profitLossKrw, overview?.profitLossUsd) { it.profitAmount }
        val profitRate = num(overview?.profitLossRate)?.let { it * 100 }
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
            usdKrwRate = usdKrwRate,
        )
    }.also { r -> r.onFailure { logE("TossAccountRepository error: ${it.message}") } }

    /** USD→KRW 매매기준율. 조회 실패 시 null */
    private suspend fun fetchUsdKrwRate(accessToken: String): Double? = runCatching {
        val resp = service.getExchangeRate(authHeaders(accessToken), "USD", "KRW")
        num(resp.body()?.result?.midRate)
    }.onFailure { logW("Toss exchange-rate error: ${it.message}") }.getOrNull()

    companion object {
        /**
         * 전일 종가 = 현재가 - (일간 손익 금액 ÷ 수량). 금액이 없으면 일간 손익률로 역산한다.
         * 토스 holdings 는 종목별 일간 손익(dailyProfitLoss)을 거래 통화 기준으로 내려준다.
         */
        fun prevClose(h: com.trueedu.tong.model.dto.toss.TossHolding): Double? {
            val last = num(h.lastPrice) ?: return null
            val qty = num(h.quantity) ?: 0.0
            val amount = num(h.dailyProfitLoss?.amount)
            val raw = when {
                amount != null && qty > 0 -> last - amount / qty
                else -> num(h.dailyProfitLoss?.rate)?.let { last / (1 + it) } ?: return null
            }
            if (raw <= 0) return null
            // 호가 단위에 맞춰 반올림: 원화 정수, 달러 1불 이상 센트, 1불 미만 소수 4자리
            return when {
                h.currency == "USD" && last >= 1 -> Math.round(raw * 100) / 100.0
                h.currency == "USD" -> Math.round(raw * 10000) / 10000.0
                else -> Math.round(raw).toDouble()
            }
        }

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
