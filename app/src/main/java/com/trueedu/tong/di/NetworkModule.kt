package com.trueedu.tong.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.trueedu.tong.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import timber.log.Timber
import com.trueedu.tong.utils.logI
import com.trueedu.tong.utils.logW
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * 네트워크 관련 의존성 제공 모듈.
 *
 * 현재는 KIS(한국투자증권) base URL 만 설정한 제네릭 베이스 구조이며,
 * 토큰 발급/갱신 등 브로커 별 비즈니스 로직은 포함하지 않는다.
 */
@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {
    private val connectTimeout = 20.seconds
    private val callTimeout = 20.seconds
    private val writeTimeout = 20.seconds
    private val readTimeout = 20.seconds

    @Provides
    @BaseUrl
    fun providesBaseUrl(): String {
        // TODO: 브로커 별 base URL 분기는 추후 추가
        return "https://openapi.koreainvestment.com:9443"
    }

    @Provides
    @WebSocketUrl
    fun providesWebSocketUrl(): String {
        return "ws://ops.koreainvestment.com:21000"
    }

    @Provides
    @Singleton
    fun providesJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = false
            explicitNulls = false
            encodeDefaults = true  // default 값 필드도 직렬화 (grant_type 등)
        }
    }

    @Provides
    @Singleton
    fun providesLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message -> Timber.tag("OkHttp").d(message) }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun providesChuckerInterceptor(@ApplicationContext context: Context): ChuckerInterceptor {
        return ChuckerInterceptor.Builder(context)
            .alwaysReadResponseBody(true)
            .collector(
                ChuckerCollector(
                    context,
                    showNotification = true,
                    retentionPeriod = RetentionManager.Period.ONE_WEEK
                )
            )
            .build()
    }

    @Provides
    @Singleton
    @KisOkHttp
    fun providesOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(chuckerInterceptor)
            .connectTimeout(connectTimeout.toJavaDuration())
            .callTimeout(callTimeout.toJavaDuration())
            .writeTimeout(writeTimeout.toJavaDuration())
            .readTimeout(readTimeout.toJavaDuration())
            .build()
    }

    // WebSocket 전용: callTimeout/readTimeout=0 (long-lived connection)
    @Provides
    @Singleton
    @KisWsOkHttp
    fun providesKisWsOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(chuckerInterceptor)
            .connectTimeout(connectTimeout.toJavaDuration())
            .callTimeout(java.time.Duration.ZERO)   // WebSocket은 timeout 없음
            .writeTimeout(writeTimeout.toJavaDuration())
            .readTimeout(java.time.Duration.ZERO)   // WebSocket은 읽기 timeout 없음
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    @KisRetrofit
    fun providesRetrofit(
        @BaseUrl baseUrl: String,
        @KisOkHttp okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    // ---------------------------------------------------------------------
    // 홈 화면 자산/잔고용 증권사 별 OkHttp / Retrofit
    // 각 증권사 base URL 이 다르므로 별도 인스턴스를 제공한다.
    // loggingInterceptor / chuckerInterceptor / json 은 공유한다.
    // ---------------------------------------------------------------------

    private fun buildOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(chuckerInterceptor)
            .connectTimeout(connectTimeout.toJavaDuration())
            .callTimeout(callTimeout.toJavaDuration())
            .writeTimeout(writeTimeout.toJavaDuration())
            .readTimeout(readTimeout.toJavaDuration())
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun buildRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    // KIS (한국투자증권) - 기존 @KisRetrofit 과 동일 baseUrl, 별도 인스턴스
    @Provides
    @Singleton
    @KisRetrofitQualifier
    fun providesKisRetrofit(
        @KisOkHttp okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(
        baseUrl = "https://openapi.koreainvestment.com:9443/",
        okHttpClient = okHttpClient,
        json = json,
    )

    // 키움증권
    @Provides
    @Singleton
    @KiwoomOkHttp
    fun providesKiwoomOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient = buildOkHttpClient(loggingInterceptor, chuckerInterceptor)

    // 키움 WebSocket 전용: callTimeout/readTimeout=0 (long-lived connection)
    @Provides
    @Singleton
    @KiwoomWsOkHttp
    fun providesKiwoomWsOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(chuckerInterceptor)
            .connectTimeout(connectTimeout.toJavaDuration())
            .callTimeout(java.time.Duration.ZERO)   // WebSocket은 timeout 없음
            .writeTimeout(writeTimeout.toJavaDuration())
            .readTimeout(java.time.Duration.ZERO)   // WebSocket은 읽기 timeout 없음
            .build()
    }

    @Provides
    @Singleton
    @KiwoomRetrofitQualifier
    fun providesKiwoomRetrofit(
        @KiwoomOkHttp okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(
        baseUrl = "https://api.kiwoom.com/",
        okHttpClient = okHttpClient,
        json = json,
    )

    // LS증권
    @Provides
    @Singleton
    @LsOkHttp
    fun providesLsOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient = buildOkHttpClient(loggingInterceptor, chuckerInterceptor)

    @Provides
    @Singleton
    @LsRetrofitQualifier
    fun providesLsRetrofit(
        @LsOkHttp okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(
        baseUrl = "https://openapi.ls-sec.co.kr:8080/",
        okHttpClient = okHttpClient,
        json = json,
    )

    // 토스증권
    @Provides
    @Singleton
    @TossOkHttp
    fun providesTossOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient = buildOkHttpClient(loggingInterceptor, chuckerInterceptor)

    @Provides
    @Singleton
    @TossRetrofitQualifier
    fun providesTossRetrofit(
        @TossOkHttp okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(
        baseUrl = "https://openapi.tossinvest.com/",
        okHttpClient = okHttpClient,
        json = json,
    )
}
