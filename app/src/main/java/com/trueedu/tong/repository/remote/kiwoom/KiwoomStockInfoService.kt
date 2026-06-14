package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.model.dto.stockinfo.KiwoomStockInfoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface KiwoomStockInfoService {
    // ka10001 주식기본정보요청
    @POST("api/dostk/stkinfo")
    suspend fun getStockInfo(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomStockInfoResponse>
}
