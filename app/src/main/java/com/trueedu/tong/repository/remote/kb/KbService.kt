package com.trueedu.tong.repository.remote.kb

import com.trueedu.tong.model.dto.kb.KbRequest
import com.trueedu.tong.model.dto.kb.KbResponse
import com.trueedu.tong.model.dto.kb.KbTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

interface KbAuthService {
    @POST("oauth2/token")
    suspend fun getToken(@Body request: KbRequest): Response<KbTokenResponse>
}

/** 모든 KB 조회/주문 TR 은 `api/v1/<tr코드 소문자>` 로 POST 한다. */
interface KbApiService {
    @POST
    suspend fun call(
        @Url path: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: KbRequest,
    ): Response<KbResponse>
}
