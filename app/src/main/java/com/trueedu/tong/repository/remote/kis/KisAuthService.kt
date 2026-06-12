package com.trueedu.tong.repository.remote.kis

import com.trueedu.tong.model.dto.auth.KisTokenRequest
import com.trueedu.tong.model.dto.auth.KisTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface KisAuthService {
    @POST("oauth2/tokenP")
    suspend fun getToken(@Body request: KisTokenRequest): Response<KisTokenResponse>
}
