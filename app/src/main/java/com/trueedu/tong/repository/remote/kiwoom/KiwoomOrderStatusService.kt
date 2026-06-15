package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.model.dto.kiwoom.KiwoomFilledOrderResponse
import com.trueedu.tong.model.dto.kiwoom.KiwoomModifyCancelResponse
import com.trueedu.tong.model.dto.kiwoom.KiwoomRealizedPnlResponse
import com.trueedu.tong.model.dto.kiwoom.KiwoomUnfilledOrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface KiwoomOrderStatusService {
    // ka10075 미체결
    @POST("api/dostk/acnt")
    suspend fun getUnfilled(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomUnfilledOrderResponse>

    // ka10076 체결
    @POST("api/dostk/acnt")
    suspend fun getFilled(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomFilledOrderResponse>

    // kt00015 일별 실현손익
    @POST("api/dostk/acnt")
    suspend fun getRealizedPnl(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomRealizedPnlResponse>

    // kt10002 정정 / kt10003 취소
    @POST("api/dostk/ordr")
    suspend fun modifyOrCancel(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomModifyCancelResponse>
}
