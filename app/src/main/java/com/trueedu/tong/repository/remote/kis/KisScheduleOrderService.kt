package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.model.dto.kis.KisScheduleOrderCancelResponse
import com.trueedu.tong.model.dto.kis.KisScheduleOrderListResponse
import com.trueedu.tong.model.dto.kis.KisScheduleOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface KisScheduleOrderService {
    // 예약주문 조회 (CTSC0004R)
    @GET("uapi/domestic-stock/v1/trading/order-resv-ccnl")
    suspend fun getScheduleOrders(
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, String>,
    ): Response<KisScheduleOrderListResponse>

    // 예약주문 등록 (CTSC0008U)
    @POST("uapi/domestic-stock/v1/trading/order-resv")
    suspend fun placeScheduleOrder(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KisScheduleOrderResponse>

    // 예약주문 정정(CTSC0013U) / 취소(CTSC0009U) — tr_id 로 구분
    @POST("uapi/domestic-stock/v1/trading/order-resv-rvsecncl")
    suspend fun modifyOrCancelScheduleOrder(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KisScheduleOrderCancelResponse>
}
