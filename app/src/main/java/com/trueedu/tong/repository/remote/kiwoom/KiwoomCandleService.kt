package com.trueedu.tong.repository.remote.kiwoom

import com.trueedu.tong.model.dto.candle.KiwoomDayCandleResponse
import com.trueedu.tong.model.dto.candle.KiwoomMinuteCandleResponse
import com.trueedu.tong.model.dto.candle.KiwoomMonthCandleResponse
import com.trueedu.tong.model.dto.candle.KiwoomWeekCandleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface KiwoomCandleService {
    // ka10080 분봉
    @POST("api/dostk/chart")
    suspend fun getMinuteCandles(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomMinuteCandleResponse>

    // ka10081 일봉
    @POST("api/dostk/chart")
    suspend fun getDayCandles(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomDayCandleResponse>

    // ka10082 주봉
    @POST("api/dostk/chart")
    suspend fun getWeekCandles(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomWeekCandleResponse>

    // ka10083 월봉
    @POST("api/dostk/chart")
    suspend fun getMonthCandles(
        @HeaderMap headers: Map<String, String>,
        @Body body: Map<String, String>,
    ): Response<KiwoomMonthCandleResponse>
}
