package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.model.dto.order.LsOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface LsOrderService {
    @POST("stock/order")
    suspend fun order(@HeaderMap headers: Map<String, String>, @Body body: Map<String, String>): Response<LsOrderResponse>
}
