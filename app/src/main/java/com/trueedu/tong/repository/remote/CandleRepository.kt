package com.trueedu.tong.repository.remote

import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.di.KiwoomRetrofitQualifier
import com.trueedu.tong.di.LsRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.CandleData
import com.trueedu.tong.model.dto.candle.LsCandleInBlock
import com.trueedu.tong.model.dto.candle.LsCandleRequest
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.repository.remote.kis.KisCandleService
import com.trueedu.tong.repository.remote.kiwoom.KiwoomCandleService
import com.trueedu.tong.repository.remote.ls.LsCandleService
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import retrofit2.Retrofit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 캔들(봉) 차트 데이터 조회 레포지토리.
 *
 * 증권사 별 차트 API 응답을 공통 [CandleData] 모델로 변환한다.
 * 계좌 우선순위 선택(키움→LS→KIS)은 ViewModel 에서 담당한다.
 */
@Singleton
class CandleRepository @Inject constructor(
    @KiwoomRetrofitQualifier private val kiwoomRetrofit: Retrofit,
    @KisRetrofitQualifier private val kisRetrofit: Retrofit,
    @LsRetrofitQualifier private val lsRetrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val kiwoomService by lazy { kiwoomRetrofit.create(KiwoomCandleService::class.java) }
    private val kisService by lazy { kisRetrofit.create(KisCandleService::class.java) }
    private val lsService by lazy { lsRetrofit.create(LsCandleService::class.java) }

    /** 키움증권 ka10081 (일봉) */
    suspend fun fetchKiwoom(account: BrokerAccount, code: String): Result<List<CandleData>> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val shortCode = code.removePrefix("A")
        logD("CandleRepo.fetchKiwoom: code=$shortCode, tokenLen=${token.length}")
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "api-id" to "ka10081",
            "content-type" to "application/json;charset=UTF-8",
            "cont-yn" to "N",
            "next-key" to "",
        )
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val body = mapOf(
            "stk_cd" to shortCode,
            "base_dt" to today,      // 오늘 날짜 기준 (필수값)
            "upd_stkpc_tp" to "1",   // 수정주가 적용
        )
        logD("CandleRepo.fetchKiwoom: base_dt=$today")
        val resp = kiwoomService.getDailyCandles(headers, body)
        logD("CandleRepo.fetchKiwoom: httpCode=${resp.code()}, bodyNull=${resp.body() == null}")
        val data = resp.body() ?: error("키움 캔들 응답 없음: ${resp.code()}")
        logD("CandleRepo.fetchKiwoom: candleCount=${data.candles.size}, first=${data.candles.firstOrNull()}")

        val result = data.candles.map {
            CandleData(
                datetime = it.date,
                open = priceOf(it.open),
                high = priceOf(it.high),
                low = priceOf(it.low),
                close = priceOf(it.close),
                volume = long(it.volume) ?: 0L,
            )
        }
        logD("CandleRepo.fetchKiwoom: parsed ${result.size} candles")
        result
    }.also { r -> r.onFailure { logE("CandleRepo.fetchKiwoom error: ${it.message}") } }

    /** LS증권 t8410 (일봉) */
    suspend fun fetchLs(account: BrokerAccount, code: String): Result<List<CandleData>> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val shortCode = code.removePrefix("A")
        logD("CandleRepo.fetchLs: code=$shortCode")
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "tr_cd" to "t8410",
            "tr_cont" to "N",
            "tr_cont_key" to "",
            "content-type" to "application/json; charset=utf-8",
        )
        val req = LsCandleRequest(inBlock = LsCandleInBlock(code = shortCode, period = "2"))
        val resp = lsService.getDailyCandles(headers, req)
        logD("CandleRepo.fetchLs: httpCode=${resp.code()}, bodyNull=${resp.body() == null}")
        val data = resp.body() ?: error("LS 캔들 응답 없음: ${resp.code()}")
        logD("CandleRepo.fetchLs: candleCount=${data.candles.size}")

        val result = data.candles.map {
            CandleData(
                datetime = it.date,
                open = priceOf(it.open),
                high = priceOf(it.high),
                low = priceOf(it.low),
                close = priceOf(it.close),
                volume = long(it.volume) ?: 0L,
            )
        }
        logD("CandleRepo.fetchLs: parsed ${result.size} candles")
        result
    }.also { r -> r.onFailure { logE("CandleRepo.fetchLs error: ${it.message}") } }

    /** KIS inquire-daily-price (FHKST03010100, 일봉) */
    suspend fun fetchKis(account: BrokerAccount, code: String): Result<List<CandleData>> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val shortCode = code.removePrefix("A")
        logD("CandleRepo.fetchKis: code=$shortCode")
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "FHKST03010100",
            "custtype" to "P",
        )
        val queries = mapOf(
            "FID_COND_MRKT_DIV_CODE" to "J",
            "FID_INPUT_ISCD" to shortCode,
            "FID_PERIOD_DIV_CODE" to "D",
            "FID_ORG_ADJ_PRC" to "0",
        )
        val resp = kisService.getDailyCandles(headers, queries)
        logD("CandleRepo.fetchKis: httpCode=${resp.code()}, bodyNull=${resp.body() == null}")
        val data = resp.body() ?: error("KIS 캔들 응답 없음: ${resp.code()}")
        logD("CandleRepo.fetchKis: rtCd=${data.rtCd}, msg=${data.msg}, candleCount=${data.candles.size}")
        if (data.rtCd != "0") error("KIS 캔들 오류: ${data.msg}")

        val result = data.candles.map {
            CandleData(
                datetime = it.date,
                open = priceOf(it.open),
                high = priceOf(it.high),
                low = priceOf(it.low),
                close = priceOf(it.close),
                volume = long(it.volume) ?: 0L,
            )
        }
        logD("CandleRepo.fetchKis: parsed ${result.size} candles")
        result
    }.also { r -> r.onFailure { logE("CandleRepo.fetchKis error: ${it.message}") } }

    // --- 파싱 헬퍼 ---

    /** 부호/콤마 제거 후 Double. "+1,200" -> 1200.0, "" -> null */
    private fun num(s: String?): Double? =
        s?.trim()?.replace(",", "")?.replace("+", "")?.takeIf { it.isNotBlank() }?.toDoubleOrNull()

    /** 가격 필드: 부호는 등락방향 표시일 뿐이므로 절대값 사용 */
    private fun priceOf(s: String?): Double = num(s)?.let { abs(it) } ?: 0.0

    private fun long(s: String?): Long? =
        s?.trim()?.replace(",", "")?.replace("+", "")?.takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()?.let { abs(it).toLong() }
}
