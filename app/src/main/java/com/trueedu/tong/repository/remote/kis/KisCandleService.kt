package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.model.dto.candle.KisCandleResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap

interface KisCandleService {
    // FHKST03010100 주식현재가 일자별
    @GET("uapi/domestic-stock/v1/quotations/inquire-daily-price")
    suspend fun getDailyCandles(
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, String>,
    ): Response<KisCandleResponse>
}
