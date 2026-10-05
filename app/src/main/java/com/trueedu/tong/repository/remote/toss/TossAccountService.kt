package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.model.dto.toss.TossAccountListResponse
import com.trueedu.tong.model.dto.toss.TossBuyingPowerResponse
import com.trueedu.tong.model.dto.toss.TossExchangeRateResponse
import com.trueedu.tong.model.dto.toss.TossHoldingsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Query

interface TossAccountService {
    @GET("api/v1/accounts")
    suspend fun getAccounts(@HeaderMap headers: Map<String, String>): Response<TossAccountListResponse>

    @GET("api/v1/holdings")
    suspend fun getHoldings(@HeaderMap headers: Map<String, String>): Response<TossHoldingsResponse>

    // 매수 가능 금액 (ORDER_INFO 한도: 초당 6회). 통화별로 호출한다.
    @GET("api/v1/buying-power")
    suspend fun getBuyingPower(
        @HeaderMap headers: Map<String, String>,
        @Query("currency") currency: String,
    ): Response<TossBuyingPowerResponse>

    @GET("api/v1/exchange-rate")
    suspend fun getExchangeRate(
        @HeaderMap headers: Map<String, String>,
        @Query("baseCurrency") baseCurrency: String,
        @Query("quoteCurrency") quoteCurrency: String,
    ): Response<TossExchangeRateResponse>
}
