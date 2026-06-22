package com.trueedu.tong.repository.remote

import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.di.KiwoomRetrofitQualifier
import com.trueedu.tong.di.LsRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.StockInfo
import com.trueedu.tong.model.dto.stockinfo.LsStockInfoInBlock
import com.trueedu.tong.model.dto.stockinfo.LsStockInfoRequest
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.repository.remote.kis.KisStockInfoService
import com.trueedu.tong.repository.remote.kiwoom.KiwoomStockInfoService
import com.trueedu.tong.repository.remote.ls.LsStockInfoService
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 종목 상세 정보 조회 레포지토리.
 *
 * 증권사 별 시세/기본정보 API 응답을 공통 [StockInfo] 모델로 변환한다.
 * 계좌 우선순위 선택(키움→KIS→LS)은 ViewModel 에서 담당한다.
 */
@Singleton
class StockInfoRepository @Inject constructor(
    @KiwoomRetrofitQualifier private val kiwoomRetrofit: Retrofit,
    @KisRetrofitQualifier private val kisRetrofit: Retrofit,
    @LsRetrofitQualifier private val lsRetrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
) {
    private val kiwoomService by lazy { kiwoomRetrofit.create(KiwoomStockInfoService::class.java) }
    private val kisService by lazy { kisRetrofit.create(KisStockInfoService::class.java) }
    private val lsService by lazy { lsRetrofit.create(LsStockInfoService::class.java) }

    /** 키움증권 ka10001 */
    suspend fun fetchKiwoom(account: BrokerAccount, code: String): Result<StockInfo> = runCatching {
        val shortCode = code.removePrefix("A")
        val body = tokenManager.withTokenRetry(account, { it.returnCode }) { token ->
            val headers = mapOf(
                "authorization" to "Bearer $token",
                "api-id" to "ka10001",
                "content-type" to "application/json;charset=UTF-8",
                "cont-yn" to "N",
                "next-key" to "",
            )
            val resp = kiwoomService.getStockInfo(headers, mapOf("stk_cd" to shortCode))
            resp.body() ?: error("키움 종목정보 응답 없음: ${resp.code()}")
        }
        if (body.returnCode != 0) error("키움 종목정보 오류: ${body.returnMsg}")

        StockInfo(
            code = shortCode,
            name = body.name,
            currentPrice = priceOf(body.currentPrice),
            delta = num(body.delta) ?: 0.0,
            rate = num(body.rate) ?: 0.0,
            open = priceOf(body.open),
            high = priceOf(body.high),
            low = priceOf(body.low),
            volume = long(body.volume) ?: 0L,
            marketCap = num(body.marketCap),
            per = num(body.per),
            pbr = num(body.pbr),
            eps = num(body.eps),
            bps = num(body.bps),
            roe = num(body.roe),
            ev = num(body.ev),
            salesAmount = num(body.salesAmount),
            operatingProfit = num(body.operatingProfit),
            netProfit = num(body.netProfit),
            parValue = num(body.parValue),
            capital = num(body.capital),
            // flo_stk 단위: 천주 → 주
            listedShares = long(body.listedShares)?.let { it * 1000 },
            settlementMonth = body.settlementMonth.ifBlank { null },
            week52High = positiveOrNull(priceOf(body.high250)),
            week52Low = positiveOrNull(priceOf(body.low250)),
            upperLimit = positiveOrNull(priceOf(body.upperLimit)),
            lowerLimit = positiveOrNull(priceOf(body.lowerLimit)),
            foreignExhaustionRate = num(body.foreignExhaustionRate),
            creditRate = num(body.creditRate),
        )
    }

    /** KIS inquire-price (FHKST01010100) */
    suspend fun fetchKis(account: BrokerAccount, code: String): Result<StockInfo> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val shortCode = code.removePrefix("A")
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "FHKST01010100",
            "custtype" to "P",
        )
        val queries = mapOf(
            "FID_COND_MRKT_DIV_CODE" to "J",
            "FID_INPUT_ISCD" to shortCode,
        )
        val resp = kisService.getStockInfo(headers, queries)
        val body = resp.body() ?: error("KIS 종목정보 응답 없음: ${resp.code()}")
        if (body.rtCd != "0") error("KIS 종목정보 오류: ${body.msg1}")
        val o = body.output ?: error("KIS 종목정보 output 없음")

        StockInfo(
            code = shortCode,
            name = o.name,
            currentPrice = priceOf(o.currentPrice),
            delta = num(o.delta) ?: 0.0,
            rate = num(o.rate) ?: 0.0,
            open = priceOf(o.open),
            high = priceOf(o.high),
            low = priceOf(o.low),
            volume = long(o.volume) ?: 0L,
            marketCap = num(o.marketCap),
            per = num(o.per),
            pbr = num(o.pbr),
            eps = num(o.eps),
            bps = num(o.bps),
            roe = null,
            ev = null,
            salesAmount = null,
            operatingProfit = null,
            netProfit = null,
            parValue = null,
            capital = null,
            listedShares = long(o.listedShares),
            settlementMonth = o.settlementMonth.ifBlank { null },
            week52High = positiveOrNull(priceOf(o.week52High)),
            week52Low = positiveOrNull(priceOf(o.week52Low)),
            upperLimit = positiveOrNull(priceOf(o.upperLimit)),
            lowerLimit = positiveOrNull(priceOf(o.lowerLimit)),
            foreignExhaustionRate = num(o.foreignExhaustionRate),
            creditRate = null,
        )
    }

    /** LS증권 t1102 */
    suspend fun fetchLs(account: BrokerAccount, code: String): Result<StockInfo> = runCatching {
        val token = tokenManager.getValidToken(account).getOrThrow()
        val shortCode = code.removePrefix("A")
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "tr_cd" to "t1102",
            "tr_cont" to "N",
            "tr_cont_key" to "",
            "content-type" to "application/json; charset=utf-8",
        )
        val req = LsStockInfoRequest(block = LsStockInfoInBlock(shcode = shortCode))
        val resp = lsService.getStockInfo(headers, req)
        val body = resp.body() ?: error("LS 종목정보 응답 없음: ${resp.code()}")
        val o = body.output ?: error("LS 종목정보 오류: ${body.rspMsg.ifBlank { "응답 없음" }}")

        val price = priceOf(o.price)
        val prevClose = priceOf(o.prevClose)
        // change/diff 는 부호 정보가 분리돼 있어 전일종가 기준으로 직접 계산
        val delta = if (prevClose > 0) price - prevClose else num(o.change) ?: 0.0
        val rate = if (prevClose > 0) (price - prevClose) / prevClose * 100 else num(o.rate) ?: 0.0

        StockInfo(
            code = shortCode,
            name = o.name,
            currentPrice = price,
            delta = delta,
            rate = rate,
            open = priceOf(o.open),
            high = priceOf(o.high),
            low = priceOf(o.low),
            volume = long(o.volume) ?: 0L,
            marketCap = num(o.marketCap),
            per = num(o.per),
            pbr = null,
            eps = num(o.eps),
            bps = null,
            roe = null,
            ev = null,
            salesAmount = null,
            operatingProfit = null,
            netProfit = null,
            parValue = null,
            capital = null,
            listedShares = null,
            settlementMonth = null,
            week52High = positiveOrNull(priceOf(o.week52High)),
            week52Low = positiveOrNull(priceOf(o.week52Low)),
            upperLimit = positiveOrNull(priceOf(o.upperLimit)),
            lowerLimit = positiveOrNull(priceOf(o.lowerLimit)),
            foreignExhaustionRate = null,
            creditRate = null,
        )
    }

    // --- 파싱 헬퍼 ---

    /** 부호/콤마 제거 후 Double. "+1,200" -> 1200.0, "-50" -> -50.0, "" -> null */
    private fun num(s: String?): Double? =
        s?.trim()?.replace(",", "")?.replace("+", "")?.takeIf { it.isNotBlank() }?.toDoubleOrNull()

    /** 가격 필드: 부호는 등락방향 표시일 뿐이므로 절대값 사용 */
    private fun priceOf(s: String?): Double = num(s)?.let { abs(it) } ?: 0.0

    private fun long(s: String?): Long? =
        s?.trim()?.replace(",", "")?.replace("+", "")?.takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()?.let { abs(it).toLong() }

    private fun positiveOrNull(v: Double): Double? = if (v > 0.0) v else null
}
