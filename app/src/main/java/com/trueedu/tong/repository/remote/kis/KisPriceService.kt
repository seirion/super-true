package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.model.dto.kis.KisIndexPriceResponse
import com.trueedu.tong.model.dto.kis.KisPriceResponse
import com.trueedu.tong.model.dto.kis.KisQuoteResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap

interface KisPriceService {
    @GET("uapi/domestic-stock/v1/quotations/inquire-price")
    suspend fun getCurrentPrice(
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, String>,
    ): Response<KisPriceResponse>

    @GET("uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn")
    suspend fun getQuote(
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, String>,
    ): Response<KisQuoteResponse>

    @GET("uapi/domestic-stock/v1/quotations/inquire-index-price")
    suspend fun getIndexPrice(
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, String>,
    ): Response<KisIndexPriceResponse>
}
