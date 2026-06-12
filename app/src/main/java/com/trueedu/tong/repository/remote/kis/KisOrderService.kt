package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.model.dto.order.KisOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface KisOrderService {
    @POST("uapi/domestic-stock/v1/trading/order-cash")
    suspend fun order(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KisOrderResponse>
}
