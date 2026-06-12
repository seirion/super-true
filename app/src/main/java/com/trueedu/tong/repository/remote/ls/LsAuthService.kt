package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.model.dto.auth.LsTokenResponse
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface LsAuthService {
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun getToken(@FieldMap fields: Map<String, String>): Response<LsTokenResponse>
}
