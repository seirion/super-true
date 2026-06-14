package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.model.dto.stockinfo.LsStockInfoRequest
import com.trueedu.tong.model.dto.stockinfo.LsStockInfoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface LsStockInfoService {
    // t1102 주식 현재가(시세) 조회
    @POST("stock/market-data")
    suspend fun getStockInfo(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsStockInfoRequest,
    ): Response<LsStockInfoResponse>
}
