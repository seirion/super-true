package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.model.dto.stockinfo.KisStockInfoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap

interface KisStockInfoService {
    // FHKST01010100 주식현재가 시세
    @GET("uapi/domestic-stock/v1/quotations/inquire-price")
    suspend fun getStockInfo(
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, String>,
    ): Response<KisStockInfoResponse>
}
