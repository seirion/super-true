package com.trueedu.tong.data.realtime

import androidx.compose.runtime.mutableStateMapOf
import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.di.KisWsOkHttp
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.auth.KisApprovalKeyRequest
import com.trueedu.tong.model.ws.KisWsBody
import com.trueedu.tong.model.ws.KisWsBodyInput
import com.trueedu.tong.model.ws.KisWsHeader
import com.trueedu.tong.model.ws.KisWsRequest
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.kis.KisAuthService
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KIS 코스피/코스닥 업종지수 실시간 구독 매니저.
 *
 * KIS WebSocket 은 1세션 한도가 있어 기존 [KisRealPriceManager] 의 연결(체결/호가)을 건드리면 안 되므로,
 * 여기서는 @KisWsOkHttp OkHttpClient 로 **별도 WebSocket 연결**을 직접 생성한다.
 * (KisWebSocketService 는 @Singleton 이라 공유 불가 — 직접 연결을 만든다.)
 */
@Singleton
class KisMarketIndexManager @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    @KisWsOkHttp private val okHttpClient: OkHttpClient,
    private val credentialStorage: CredentialStorage,
    private val json: Json,
) {
    private val authService: KisAuthService by lazy { retrofit.create(KisAuthService::class.java) }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val wsUrl = "ws://ops.koreainvestment.com:21000"

    private val mutex = Mutex()
    private var webSocket: WebSocket? = null
    private var approvalKey: String = ""
    private var connected = false
    private var intentionalDisconnect = false
    private var account: BrokerAccount? = null
    private var connectJob: kotlinx.coroutines.Job? = null

    // 구독 종목코드: 코스피/코스닥
    private val codes = listOf(MarketIndex.KIS_KOSPI, MarketIndex.KIS_KOSDAQ)

    private val _indexFlow = MutableSharedFlow<MarketIndex>(extraBufferCapacity = 16)
    val indexFlow = _indexFlow.asSharedFlow()

    // 종목코드 → 최신 지수
    val indexMap = mutableStateMapOf<String, MarketIndex>()

    fun start(account: BrokerAccount) {
        scope.launch {
            mutex.withLock {
                connectJob?.cancel()
                connectJob = null
                this@KisMarketIndexManager.account = account
                if (connected && approvalKey.isNotEmpty()) {
                    logD("KisMarketIndexManager: 이미 연결됨 — 재구독")
                    codes.forEach { send(makeRequest(it, subscribe = true)) }
                } else {
                    logD("KisMarketIndexManager: 신규 연결 시작")
                    connectJob = launch {
                        val key = fetchApprovalKey(account) ?: return@launch
                        approvalKey = key
                        connect()
                    }
                }
            }
        }
    }

    fun stop() {
        scope.launch {
            mutex.withLock {
                connectJob?.cancel()
                connectJob = null
                intentionalDisconnect = true
                disconnect()
                connected = false
                indexMap.clear()
            }
        }
    }

    /** 백그라운드 진입 시: WebSocket만 끊고 계좌 정보는 유지 */
    fun pause() {
        scope.launch {
            mutex.withLock {
                if (!connected) return@withLock
                logD("KisMarketIndexManager: pause")
                connectJob?.cancel()
                connectJob = null
                intentionalDisconnect = true
                disconnect()
                connected = false
            }
        }
    }

    /** 포그라운드 복귀 시: 재연결 */
    fun resume() {
        scope.launch {
            mutex.withLock {
                val currentAccount = account ?: return@withLock
                logD("KisMarketIndexManager: resume")
                connectJob?.cancel()
                connectJob = launch {
                    val key = fetchApprovalKey(currentAccount) ?: return@launch
                    approvalKey = key
                    connect()
                }
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
            resp.body()?.approvalKey?.also { logD("KisMarketIndexManager: approval key 발급") }
        } catch (e: Exception) {
            logE(e, "KisMarketIndexManager: approval key 발급 실패")
            null
        }
    }

    private fun connect() {
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                logD("KisMarketIndexManager: onOpen")
                intentionalDisconnect = false
                connected = true
                codes.forEach { send(makeRequest(it, subscribe = true)) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
                if (intentionalDisconnect) {
                    logD("KisMarketIndexManager: intentional disconnect, skip reconnect")
                    intentionalDisconnect = false
                    return
                }
                logE(t, "KisMarketIndexManager: onFailure — reconnecting")
                val currentAccount = account ?: return
                scope.launch {
                    delay(2000)
                    val newKey = fetchApprovalKey(currentAccount) ?: return@launch
                    approvalKey = newKey
                    connect()
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
                when (parts[1]) {
                    "H0UPCNT0", "H0NXUPC0" -> {
                        val index = MarketIndex.fromKis(parts[3])
                        indexMap[index.code] = index
                        scope.launch { _indexFlow.emit(index) }
                    }
                }
            }
            text.contains("PINGPONG") -> send(text)
            else -> logD("KisMarketIndexManager: system msg: $text")
        }
    }

    private fun send(message: String) {
        webSocket?.send(message)
    }

    private fun disconnect() {
        webSocket?.cancel()
        webSocket = null
    }

    private fun makeRequest(code: String, subscribe: Boolean): String {
        val req = KisWsRequest(
            header = KisWsHeader(
                approvalKey = approvalKey,
                transactionType = if (subscribe) "1" else "2",
            ),
            body = KisWsBody(
                input = KisWsBodyInput(
                    transactionId = indexTransactionId(),
                    transactionKey = code,
                )
            )
        )
        return json.encodeToString(req)
    }

    companion object {
        /**
         * 업종현재가 TR ID.
         * - H0UPCNT0: KRX 정규장 (09:00~15:30)
         * - H0NXUPC0: NXT (08:00~09:00, 15:30~20:00)
         */
        fun indexTransactionId() =
            if (KisRealPriceManager.isNxtTradingHour()) "H0NXUPC0" else "H0UPCNT0"
    }
}
