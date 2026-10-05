package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.di.TossRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.UsStockLocal
import com.trueedu.tong.model.dto.toss.TossDailyCandle
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.utils.logW
import kotlinx.coroutines.delay
import retrofit2.Response
import retrofit2.Retrofit
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** 토스 시장 데이터 조회: 미국 종목 유니버스, 현재가, 전일 종가 */
@Singleton
class TossMarketRepository @Inject constructor(
    @TossRetrofitQualifier retrofit: Retrofit,
    private val tokenManager: TokenManager,
) {
    private val service: TossMarketDataService by lazy { retrofit.create(TossMarketDataService::class.java) }

    companion object {
        /** 미국 검색 대상 마켓 */
        val US_MARKETS = listOf("NASDAQ", "NYSE", "AMEX")

        /** 검색 대상 종목 유형 (워런트·인프라펀드·ETN 제외) */
        val US_SEARCH_TYPES = setOf("STOCK", "FOREIGN_STOCK", "DEPOSITARY_RECEIPT", "REIT", "ETF", "FOREIGN_ETF")

        private const val STOCK_ALL_INTERVAL_MS = 1_100L // STOCK_ALL: 초당 1회

        /**
         * 일봉(최신순)에서 전일 종가를 고른다.
         * 미국은 20:00 ET(데이마켓 개장)에 다음 거래일이 시작되므로, 현재 ET 시각 + 4시간의 날짜를
         * "현재 거래일"로 보고 그보다 이전 날짜의 첫 봉 종가를 전일 종가로 쓴다.
         * (정규장 중에는 오늘 봉이 있으므로 건너뛰고, 장외·휴장에는 마지막 정규장 종가가 선택된다)
         */
        fun prevCloseFromCandles(candles: List<TossDailyCandle>, tradingDate: LocalDate): Double? =
            candles.firstOrNull { c ->
                runCatching { LocalDate.parse(c.timestamp.take(10)).isBefore(tradingDate) }.getOrDefault(false)
            }?.closePrice?.toDoubleOrNull()

        fun currentUsTradingDate(now: ZonedDateTime = ZonedDateTime.now(ZoneId.of("America/New_York"))): LocalDate =
            now.plusHours(4).toLocalDate()
    }

    /** 미국 3개 마켓의 종목 유니버스. 하나라도 실패하면 null (기존 캐시를 유지하기 위해) */
    suspend fun fetchUsUniverse(account: BrokerAccount): List<UsStockLocal>? {
        val all = mutableListOf<UsStockLocal>()
        for ((i, market) in US_MARKETS.withIndex()) {
            if (i > 0) delay(STOCK_ALL_INTERVAL_MS)
            val resp = runCatching {
                withTokenRetry(account) { token -> service.getAllStocks(auth(token), market) }
            }.getOrElse { logW("토스 종목 유니버스 조회 실패: $market ${it.message}"); return null }
            val body = resp.body()
            if (!resp.isSuccessful || body == null) {
                logW("토스 종목 유니버스 조회 실패: $market http=${resp.code()}")
                return null
            }
            body.result
                .filter { it.securityType in US_SEARCH_TYPES && it.symbol.isNotBlank() }
                .mapTo(all) { UsStockLocal(it.symbol, it.name, market, it.securityType) }
        }
        return all
    }

    /** 심볼별 현재가 (최대 200종목). 실패하면 빈 맵 */
    suspend fun fetchLastPrices(account: BrokerAccount, symbols: List<String>): Map<String, Double> {
        if (symbols.isEmpty()) return emptyMap()
        val out = mutableMapOf<String, Double>()
        for (chunk in symbols.distinct().chunked(200)) {
            val resp = runCatching {
                withTokenRetry(account) { token -> service.getLastPrices(auth(token), chunk.joinToString(",")) }
            }.getOrNull() ?: continue
            resp.body()?.result?.forEach { p -> p.lastPrice.toDoubleOrNull()?.let { out[p.symbol] = it } }
        }
        return out
    }

    /** 전일 종가. 조회 실패나 데이터 없음이면 null */
    suspend fun fetchPrevClose(account: BrokerAccount, symbol: String): Double? {
        val resp = runCatching {
            withTokenRetry(account) { token -> service.getDailyCandles(auth(token), symbol = symbol) }
        }.getOrNull() ?: return null
        val candles = resp.body()?.result?.candles ?: return null
        return prevCloseFromCandles(candles, currentUsTradingDate())
    }

    private fun auth(token: String) = mapOf("Authorization" to "Bearer $token")

    /** 토큰 오류(HTTP 401) 시 강제 갱신 후 1회 재시도 */
    private suspend fun <T> withTokenRetry(
        account: BrokerAccount,
        block: suspend (token: String) -> Response<T>,
    ): Response<T> {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val resp = block(token)
        if (resp.code() != 401) return resp
        tokenManager.invalidateToken(account.id)
        return block(tokenManager.refreshToken(account).getOrThrow())
    }
}
