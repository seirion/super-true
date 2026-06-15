package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.model.dto.ls.LsCancelRequest
import com.trueedu.tong.model.dto.ls.LsModifyCancelResponse
import com.trueedu.tong.model.dto.ls.LsModifyRequest
import com.trueedu.tong.model.dto.ls.LsOrderStatusRequest
import com.trueedu.tong.model.dto.ls.LsOrderStatusResponse
import com.trueedu.tong.model.dto.ls.LsRealizedPnlRequest
import com.trueedu.tong.model.dto.ls.LsRealizedPnlResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface LsOrderStatusService {
    // t0425 미체결/체결 조회 (잔고와 동일한 stock/accno endpoint)
    @POST("stock/accno")
    suspend fun getOrders(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsOrderStatusRequest,
    ): Response<LsOrderStatusResponse>

    // CSPAQ22200 실현손익 조회
    @POST("stock/accno")
    suspend fun getRealizedPnl(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsRealizedPnlRequest,
    ): Response<LsRealizedPnlResponse>

    // CSPAT00701 정정
    @POST("stock/order")
    suspend fun modify(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsModifyRequest,
    ): Response<LsModifyCancelResponse>

    // CSPAT00801 취소
    @POST("stock/order")
    suspend fun cancel(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsCancelRequest,
    ): Response<LsModifyCancelResponse>
}
