package com.trueedu.tong.repository.remote.ls

import com.trueedu.tong.model.dto.candle.LsCandleRequest
import com.trueedu.tong.model.dto.candle.LsCandleResponse
import com.trueedu.tong.model.dto.candle.LsMinuteCandleRequest
import com.trueedu.tong.model.dto.candle.LsMinuteCandleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface LsCandleService {
    // t8410 주식 기간별 주가 조회 (일/주/월봉)
    @POST("stock/chart")
    suspend fun getDailyCandles(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsCandleRequest,
    ): Response<LsCandleResponse>

    // t8412 분봉 조회
    @POST("stock/chart")
    suspend fun getMinuteCandles(
        @HeaderMap headers: Map<String, String>,
        @Body body: LsMinuteCandleRequest,
    ): Response<LsMinuteCandleResponse>
}
