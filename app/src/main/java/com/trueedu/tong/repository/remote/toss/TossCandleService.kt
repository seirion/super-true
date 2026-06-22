package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.model.dto.toss.TossCandlesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap

interface TossCandleService {
    @GET("api/v1/candles")
    suspend fun getCandles(
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>,
    ): Response<TossCandlesResponse>
}
