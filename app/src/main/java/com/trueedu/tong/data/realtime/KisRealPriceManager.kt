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
import com.trueedu.tong.repository.remote.kis.KisAuthService
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
    private val json: Json,
) {
    private val authService: KisAuthService by lazy { retrofit.create(KisAuthService::class.java) }
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

    fun start(account: BrokerAccount, codes: List<String>) {
        this.account = account
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
