package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.model.dto.kis.KisFilledOrderResponse
import com.trueedu.tong.model.dto.kis.KisUnfilledOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface KisOrderStatusService {
    // 미체결 조회
    @GET("uapi/domestic-stock/v1/trading/inquire-psbl-rvsecncl")
    suspend fun getUnfilled(
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, String>,
    ): Response<KisUnfilledOrderResponse>

    // 체결 내역 조회
    @GET("uapi/domestic-stock/v1/trading/inquire-daily-ccld")
    suspend fun getFilled(
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, String>,
    ): Response<KisFilledOrderResponse>

    // 취소/정정 주문
    @POST("uapi/domestic-stock/v1/trading/order-rvsecncl")
    suspend fun modifyOrCancel(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<com.trueedu.tong.model.dto.order.KisOrderResponse>
}
