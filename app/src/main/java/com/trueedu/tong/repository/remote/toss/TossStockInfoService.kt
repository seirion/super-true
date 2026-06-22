package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.model.dto.toss.TossPricesResponse
import com.trueedu.tong.model.dto.toss.TossStocksResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap

interface TossStockInfoService {
    @GET("api/v1/prices")
    suspend fun getPrices(
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): Response<TossPricesResponse>

    @GET("api/v1/stocks")
    suspend fun getStocks(
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): Response<TossStocksResponse>
}
