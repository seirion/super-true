package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.model.dto.toss.TossDailyCandleResponse
import com.trueedu.tong.model.dto.toss.TossLastPriceResponse
import com.trueedu.tong.model.dto.toss.TossUniverseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Query

/** 토스 시장 데이터 (종목 유니버스, 현재가, 일봉). 계좌 헤더 없이 토큰만으로 호출한다. */
interface TossMarketDataService {
    // STOCK_ALL 한도: 초당 1회
    @GET("api/v1/stocks/all")
    suspend fun getAllStocks(
        @HeaderMap headers: Map<String, String>,
        @Query("market") market: String,
    ): Response<TossUniverseResponse>

    // MARKET_DATA 한도: 초당 15회, 최대 200종목
    @GET("api/v1/prices")
    suspend fun getLastPrices(
        @HeaderMap headers: Map<String, String>,
        @Query("symbols") symbols: String,
    ): Response<TossLastPriceResponse>

    // MARKET_DATA_CHART 한도: 초당 20회
    @GET("api/v1/candles")
    suspend fun getDailyCandles(
        @HeaderMap headers: Map<String, String>,
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("count") count: Int = 5,
        @Query("adjusted") adjusted: Boolean = false,
    ): Response<TossDailyCandleResponse>
}
