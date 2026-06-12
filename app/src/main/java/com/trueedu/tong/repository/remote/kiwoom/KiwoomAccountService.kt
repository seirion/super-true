package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.model.dto.kiwoom.KiwoomBalanceResponse
import com.trueedu.tong.model.dto.kiwoom.KiwoomDepositResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface KiwoomAccountService {
    // kt00018 - 계좌평가잔고내역
    @POST("api/dostk/acnt")
    suspend fun getBalance(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomBalanceResponse>

    // kt00001 - 예수금상세현황
    @POST("api/dostk/acnt")
    suspend fun getDeposit(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomDepositResponse>
}
