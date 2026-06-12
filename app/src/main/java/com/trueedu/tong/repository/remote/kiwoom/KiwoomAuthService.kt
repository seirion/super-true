package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.model.dto.auth.KiwoomTokenRequest
import com.trueedu.tong.model.dto.auth.KiwoomTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface KiwoomAuthService {
    @POST("oauth2/token")
    suspend fun getToken(@Body request: KiwoomTokenRequest): Response<KiwoomTokenResponse>
}
