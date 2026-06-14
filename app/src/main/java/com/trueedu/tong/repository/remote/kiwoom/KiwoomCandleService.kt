package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.model.dto.candle.KiwoomCandleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface KiwoomCandleService {
    // ka10081 주식 일봉차트 조회
    @POST("api/dostk/chart")
    suspend fun getDailyCandles(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomCandleResponse>
}
