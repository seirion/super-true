package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.model.dto.toss.TossOrderCreateRequest
import com.trueedu.tong.model.dto.toss.TossOrderModifyRequest
import com.trueedu.tong.model.dto.toss.TossOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path

interface TossOrderService {
    @POST("api/v1/orders")
    suspend fun createOrder(
        @HeaderMap headers: Map<String, String>,
        @Body body: TossOrderCreateRequest,
    ): Response<TossOrderResponse>

    @POST("api/v1/orders/{orderId}/modify")
    suspend fun modifyOrder(
        @HeaderMap headers: Map<String, String>,
        @Path("orderId") orderId: String,
        @Body body: TossOrderModifyRequest,
    ): Response<TossOrderResponse>

    @POST("api/v1/orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @HeaderMap headers: Map<String, String>,
        @Path("orderId") orderId: String,
    ): Response<TossOrderResponse>
}
