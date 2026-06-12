package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.model.dto.ls.LsOrderRequest
import com.trueedu.tong.model.dto.ls.LsOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface LsOrderService {
    @POST("stock/order")
    suspend fun order(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsOrderRequest,
    ): Response<LsOrderResponse>
}
