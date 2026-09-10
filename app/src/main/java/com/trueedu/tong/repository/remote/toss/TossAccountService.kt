package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.model.dto.toss.TossAccountListResponse
import com.trueedu.tong.model.dto.toss.TossHoldingsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap

interface TossAccountService {
    @GET("api/v1/accounts")
    suspend fun getAccounts(@HeaderMap headers: Map<String, String>): Response<TossAccountListResponse>

    @GET("api/v1/holdings")
    suspend fun getHoldings(@HeaderMap headers: Map<String, String>): Response<TossHoldingsResponse>
}
