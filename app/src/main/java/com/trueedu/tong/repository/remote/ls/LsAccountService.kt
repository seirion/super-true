package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.model.dto.ls.LsBalanceRequest
import com.trueedu.tong.model.dto.ls.LsBalanceResponse
import com.trueedu.tong.model.dto.ls.LsDepositRequest
import com.trueedu.tong.model.dto.ls.LsDepositResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface LsAccountService {
    // t0424 - 주식잔고2 (보유종목)
    @POST("stock/accno")
    suspend fun getBalance(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsBalanceRequest,
    ): Response<LsBalanceResponse>

    // CSPAQ12200 - 현물계좌 예수금/총평가
    @POST("stock/accno")
    suspend fun getDeposit(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsDepositRequest,
    ): Response<LsDepositResponse>
}
