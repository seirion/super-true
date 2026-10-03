package com.trueedu.tong.data.realtime

import android.content.Context
import android.content.SharedPreferences
import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.repository.remote.kis.KisPriceService
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import com.trueedu.tong.utils.logW
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 미국 티커의 거래소 코드(NAS/NYS/AMS)를 KIS 해외주식 상품기본정보(CTPF1702R)로 조회한다.
 * KIS 실시간 구독 키가 `D` + 시장구분(3자리) + 티커 이므로 시장 구분이 필요하다.
 *
 * 조회한 값은 SharedPreferences 에 저장해 재사용한다 (거래소는 거의 바뀌지 않음).
 * 조회 실패는 저장하지 않으므로 다음 요청 때 다시 시도한다.
 * (평일 08:59~09:31 은 KIS 가 이 API 호출을 제한한다: EGW00317)
 */
@Singleton
class KisOverseasExchangeResolver @Inject constructor(
    @KisRetrofitQualifier retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
    @ApplicationContext context: Context,
) {
    // 거래소 캐시 전용 저장소
    private val preferences: SharedPreferences =
        context.getSharedPreferences("kis_us_exchange", Context.MODE_PRIVATE)

    private val priceService: KisPriceService by lazy { retrofit.create(KisPriceService::class.java) }

    private companion object {
        const val KEY_PREFIX = "kis_us_excg_"
        // 상품유형코드 → 실시간 구독 시장구분: 512 나스닥, 513 뉴욕, 529 아멕스
        val CANDIDATES = listOf("512" to "NAS", "513" to "NYS", "529" to "AMS")
    }

    /** 캐시된 거래소 코드. 없으면 null */
    fun cached(symbol: String): String? =
        preferences.getString(KEY_PREFIX + symbol, null)?.takeIf { it.isNotBlank() }

    /**
     * 티커 → 거래소 코드. 캐시에 없는 티커만 REST 로 조회한다.
     * 조회하지 못한 티커는 결과에서 빠진다.
     */
    suspend fun resolve(account: BrokerAccount, symbols: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val missing = mutableListOf<String>()
        symbols.forEach { s -> cached(s)?.let { result[s] = it } ?: missing.add(s) }
        if (missing.isEmpty()) return result

        val token = tokenManager.getValidToken(account).getOrElse {
            logE(it, "KIS 해외 거래소 조회: 토큰 발급 실패")
            return result
        }
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "CTPF1702R",
            "custtype" to "P",
        )
        for (symbol in missing) {
            val excg = lookup(headers, symbol)
            if (excg != null) {
                preferences.edit().putString(KEY_PREFIX + symbol, excg).apply()
                result[symbol] = excg
            }
        }
        return result
    }

    private suspend fun lookup(headers: Map<String, String>, symbol: String): String? {
        for ((typeCode, excg) in CANDIDATES) {
            try {
                val body = priceService.getOverseasProduct(
                    headers, mapOf("PRDT_TYPE_CD" to typeCode, "PDNO" to symbol)
                ).body()
                if (body?.rtCd == "0" && body.output?.stdPdno?.isNotBlank() == true) {
                    logD("KIS 해외 거래소: $symbol → $excg")
                    return excg
                }
                // 제한 시간대(EGW00317) 등은 다른 거래소를 더 시도해도 같으므로 중단
                if (body?.msg1?.contains("조회하실 수 없습니다") == true) {
                    logW("KIS 해외 거래소 조회 제한: ${body.msg1}")
                    return null
                }
            } catch (e: Exception) {
                logE(e, "KIS 해외 거래소 조회 오류: $symbol/$typeCode")
                return null
            }
            delay(60) // 초당 20건 제한 대응
        }
        logW("KIS 해외 거래소를 찾지 못함: $symbol")
        return null
    }
}
