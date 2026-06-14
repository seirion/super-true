package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.model.dto.candle.LsCandleRequest
import com.trueedu.tong.model.dto.candle.LsCandleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface LsCandleService {
    // t8410 주식 기간별 주가 조회
    @POST("stock/chart")
    suspend fun getDailyCandles(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsCandleRequest,
    ): Response<LsCandleResponse>
}
