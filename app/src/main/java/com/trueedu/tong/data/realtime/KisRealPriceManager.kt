package com.trueedu.tong.data.realtime

import androidx.compose.runtime.mutableStateMapOf
import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.auth.KisApprovalKeyRequest
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.model.ws.KisWsBody
import com.trueedu.tong.model.ws.KisWsBodyInput
import com.trueedu.tong.model.ws.KisWsHeader
import com.trueedu.tong.model.ws.KisWsRequest
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.repository.remote.kis.KisAuthService
import com.trueedu.tong.repository.remote.kis.KisPriceService
import com.trueedu.tong.repository.remote.kis.KisWebSocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Retrofit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisRealPriceManager @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    private val wsService: KisWebSocketService,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
    private val json: Json,
) {
    private val authService: KisAuthService by lazy { retrofit.create(KisAuthService::class.java) }
    private val priceService: KisPriceService by lazy { retrofit.create(KisPriceService::class.java) }
    private val scope = CoroutineScope(Dispatchers.IO)

    private var approvalKey: String = ""
    private var connected = false
    private var account: BrokerAccount? = null
    private val subscribedCodes = mutableSetOf<String>()

    // 실시간 체결가 스트림
    private val _tradeFlow = MutableSharedFlow<KisRealTimeTrade>(extraBufferCapacity = 64)
    val tradeFlow = _tradeFlow.asSharedFlow()

    // 종목코드 → 최신 체결 데이터
    val priceMap = mutableStateMapOf<String, KisRealTimeTrade>()

    // 종목코드 → REST API 초기 현재가 (WebSocket 첫 체결 전 fallback)
    val initialPriceMap = mutableStateMapOf<String, InitialPrice>()
    private val _initialPriceFlow = MutableSharedFlow<Map<String, InitialPrice>>(replay = 1)
    val initialPriceFlow = _initialPriceFlow.asSharedFlow()

    fun start(account: BrokerAccount, codes: List<String>) {
        this.account = account
        // 초기 현재가 조회는 WebSocket 연결과 병렬로 실행
        scope.launch { fetchInitialPrices(account, codes) }
        scope.launch {
            val key = fetchApprovalKey(account) ?: return@launch
            approvalKey = key
            connect(codes)
        }
    }

    fun stop() {
        subscribedCodes.clear()
        wsService.disconnect()
        connected = false
        priceMap.clear()
        initialPriceMap.clear()
    }

    fun subscribe(codes: List<String>) {
        if (!connected || approvalKey.isEmpty()) return
        codes.forEach { code ->
            if (subscribedCodes.add(code)) {
                wsService.send(makeRequest(code, subscribe = true))
            }
        }
    }

    fun unsubscribe(codes: List<String>) {
        codes.forEach { code ->
            if (subscribedCodes.remove(code)) {
                wsService.send(makeRequest(code, subscribe = false))
            }
        }
    }

    private suspend fun fetchApprovalKey(account: BrokerAccount): String? {
        return try {
            val resp = authService.getApprovalKey(
                KisApprovalKeyRequest(
                    appKey = credentialStorage.getAppKey(account.id),
                    secretKey = credentialStorage.getAppSecret(account.id),
                )
            )
            resp.body()?.approvalKey?.also { Timber.d("KIS approval key 발급: $it") }
        } catch (e: Exception) {
            Timber.e(e, "KIS approval key 발급 실패")
            null
        }
    }

    /**
     * 구독 종목들의 현재가를 REST API로 1회 조회하여 [initialPriceMap]에 채운다.
     * WebSocket 첫 체결이 들어오기 전까지 빈 값 대신 이 값을 표시한다.
     * KIS API 초당 20건 제한 → 각 요청 사이에 60ms 딜레이.
     */
    private suspend fun fetchInitialPrices(account: BrokerAccount, codes: List<String>) {
        if (codes.isEmpty()) return
        val token = tokenManager.getValidToken(account).getOrElse {
            Timber.e(it, "KIS 초기 현재가: 토큰 발급 실패")
            return
        }
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "FHKST01010100",
            "custtype" to "P",
        )
        codes.forEach { code ->
            try {
                val queries = mapOf(
                    "FID_COND_MRKT_DIV_CODE" to "J",
                    "FID_INPUT_ISCD" to code,
                )
                val body = priceService.getCurrentPrice(headers, queries).body()
                val output = body?.output
                if (body?.rtCd == "0" && output != null) {
                    initialPriceMap[code] = InitialPrice(
                        code = code,
                        price = output.price.toDoubleOrNull() ?: 0.0,
                        delta = output.delta.toDoubleOrNull() ?: 0.0,
                        rate = output.rate.toDoubleOrNull() ?: 0.0,
                    )
                    _initialPriceFlow.emit(initialPriceMap.toMap())
                } else {
                    Timber.w("KIS 초기 현재가 실패: code=$code, msg=${body?.msg1}")
                }
            } catch (e: Exception) {
                Timber.e(e, "KIS 초기 현재가 조회 오류: code=$code")
            }
            delay(60) // 초당 20건 제한 대응
        }
    }

    private fun connect(codes: List<String>) {
        wsService.connect(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("KisRealPriceManager: onOpen")
                connected = true
                // 연결 후 종목 구독
                codes.forEach { code ->
                    subscribedCodes.add(code)
                    wsService.send(makeRequest(code, subscribe = true))
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "KisRealPriceManager: onFailure")
                connected = false
                val currentAccount = account ?: return
                // 재연결 시 approval key 재발급 (ALREADY IN USE 오류 방지)
                scope.launch {
                    delay(2000)
                    val newKey = fetchApprovalKey(currentAccount) ?: return@launch
                    approvalKey = newKey
                    connect(subscribedCodes.toList())
                }
            }
        })
    }

    private fun handleMessage(text: String) {
        when {
            text.startsWith("0|") -> {
                // 실시간 데이터 (비암호화) - "0|tr_id|count|data"
                val parts = text.split("|")
                if (parts.size < 4) return
                val trId = parts[1]
                if (trId == "H0STCNT0") {
                    val trade = KisRealTimeTrade.from(parts[3])
                    priceMap[trade.code] = trade
                    scope.launch { _tradeFlow.emit(trade) }
                }
            }
            text.contains("PINGPONG") -> {
                // PingPong 응답
                wsService.send(text)
            }
            else -> {
                Timber.d("KisRealPriceManager: system msg: $text")
            }
        }
    }

    private fun makeRequest(code: String, subscribe: Boolean): String {
        val req = KisWsRequest(
            header = KisWsHeader(
                approvalKey = approvalKey,
                transactionType = if (subscribe) "1" else "2",
            ),
            body = KisWsBody(
                input = KisWsBodyInput(
                    transactionId = "H0STCNT0",
                    transactionKey = code,
                )
            )
        )
        return json.encodeToString(req)
    }
}
