package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.model.dto.order.KiwoomOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface KiwoomOrderService {
    @POST("api/dostk/ordr")
    suspend fun order(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomOrderResponse>
}
