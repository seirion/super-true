package com.trueedu.tong.repository.remote.auth

import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.dto.auth.KisTokenRequest
import com.trueedu.tong.model.dto.auth.KiwoomTokenRequest
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.kis.KisAuthService
import com.trueedu.tong.repository.remote.kiwoom.KiwoomAuthService
import com.trueedu.tong.repository.remote.ls.LsAuthService
import retrofit2.Retrofit
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import com.trueedu.tong.utils.logI
import com.trueedu.tong.utils.logW
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @com.trueedu.tong.di.KisRetrofitQualifier private val kisRetrofit: Retrofit,
    @com.trueedu.tong.di.KiwoomRetrofitQualifier private val kiwoomRetrofit: Retrofit,
    @com.trueedu.tong.di.LsRetrofitQualifier private val lsRetrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
) {
    private val kisAuthService: KisAuthService by lazy { kisRetrofit.create(KisAuthService::class.java) }
    private val kiwoomAuthService: KiwoomAuthService by lazy { kiwoomRetrofit.create(KiwoomAuthService::class.java) }
    private val lsAuthService: LsAuthService by lazy { lsRetrofit.create(LsAuthService::class.java) }

    private val bufferMs = 5 * 60 * 1000L // 만료 5분 전 갱신

    /**
     * 유효한 토큰 반환. 만료 임박 시 자동 갱신.
     */
    suspend fun getValidToken(account: BrokerAccount): Result<String> {
        val now = System.currentTimeMillis()
        val cached = credentialStorage.getAccessToken(account.id)
        val expiredAt = credentialStorage.getTokenExpiredAt(account.id)

        if (cached.isNotBlank() && expiredAt > now + bufferMs) {
            return Result.success(cached)
        }

        logD("TokenManager: 토큰 갱신 - accountId=${account.id}, broker=${account.brokerType}")
        return refreshToken(account)
    }

    /**
     * 강제 토큰 갱신
     */
    suspend fun refreshToken(account: BrokerAccount): Result<String> = runCatching {
        val appKey = credentialStorage.getAppKey(account.id)
        val appSecret = credentialStorage.getAppSecret(account.id)
        logD("TokenManager: refreshToken - accountId=${account.id}, broker=${account.brokerType}, appKey=${appKey.take(8)}..., appSecretEmpty=${appSecret.isBlank()}")

        when (account.brokerType) {
            BrokerType.KIS -> {
                val resp = kisAuthService.getToken(
                    KisTokenRequest(appKey = appKey, appSecret = appSecret)
                )
                val body = resp.body() ?: error("KIS 토큰 발급 실패: ${resp.code()}")
                val expiredAtMs = parseKisExpiry(body.expiredAt)
                credentialStorage.saveToken(account.id, body.accessToken, expiredAtMs)
                body.accessToken
            }

            BrokerType.KIWOOM -> {
                val resp = kiwoomAuthService.getToken(
                    KiwoomTokenRequest(appKey = appKey, secretKey = appSecret)
                )
                val body = resp.body() ?: error("키움 토큰 발급 실패: ${resp.code()}")
                if (body.returnCode != 0) error("키움 토큰 오류: ${body.returnMsg}")
                val expiredAtMs = parseKiwoomExpiry(body.expiresAt)
                logD("TokenManager: 키움 토큰 발급 성공 - token=${body.accessToken.take(10)}..., expiresAt=${body.expiresAt}, expiredAtMs=$expiredAtMs")
                credentialStorage.saveToken(account.id, body.accessToken, expiredAtMs)
                body.accessToken
            }

            BrokerType.LS -> {
                val fields = mapOf(
                    "grant_type" to "client_credentials",
                    "appkey" to appKey,
                    "appsecretkey" to appSecret,
                    "scope" to "oob",
                )
                val resp = lsAuthService.getToken(fields)
                val body = resp.body() ?: error("LS 토큰 발급 실패: ${resp.code()}")
                val expiredAtMs = System.currentTimeMillis() + body.expiresIn * 1000L
                credentialStorage.saveToken(account.id, body.accessToken, expiredAtMs)
                body.accessToken
            }

            BrokerType.TOSS -> error("토스증권 미지원")
        }
    }

    /**
     * KIS 만료 시각 파싱: "yyyy-MM-dd HH:mm:ss"
     */
    private fun parseKisExpiry(expiredAt: String): Long = runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .parse(expiredAt)?.time ?: (System.currentTimeMillis() + 24 * 3600 * 1000L)
    }.getOrDefault(System.currentTimeMillis() + 24 * 3600 * 1000L)

    /**
     * 키움 만료 시각 파싱: "yyyyMMddHHmmss"
     */
    private fun parseKiwoomExpiry(expiresAt: String): Long = runCatching {
        SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
            .parse(expiresAt)?.time ?: (System.currentTimeMillis() + 24 * 3600 * 1000L)
    }.getOrDefault(System.currentTimeMillis() + 24 * 3600 * 1000L)
}
