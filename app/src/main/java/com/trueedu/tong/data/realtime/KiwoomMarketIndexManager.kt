package com.trueedu.tong.data.realtime

import androidx.compose.runtime.mutableStateMapOf
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.ws.KiwoomWsBody
import com.trueedu.tong.model.ws.KiwoomWsHeader
import com.trueedu.tong.model.ws.KiwoomWsRequest
import com.trueedu.tong.repository.remote.auth.TokenManager
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
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 키움증권 코스피/코스닥 업종지수 실시간 구독 매니저.
 *
 * KIS 와 달리 approval key 가 아닌 access token 으로 인증한다.
 */
@Singleton
class KiwoomMarketIndexManager @Inject constructor(
    private val wsService: KiwoomWebSocketService,
    private val tokenManager: TokenManager,
    private val json: Json,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val mutex = Mutex()
    private var token: String = ""
    private var connected = false
    private var intentionalDisconnect = false
    private var account: BrokerAccount? = null
    private var connectJob: kotlinx.coroutines.Job? = null

    // 구독 종목코드: 코스피/코스닥 (키움 기준)
    private val codes = listOf(MarketIndex.KIWOOM_KOSPI, MarketIndex.KIWOOM_KOSDAQ)

    private val _indexFlow = MutableSharedFlow<MarketIndex>(extraBufferCapacity = 16)
    val indexFlow = _indexFlow.asSharedFlow()

    // 종목코드 → 최신 지수
    val indexMap = mutableStateMapOf<String, MarketIndex>()

    fun start(account: BrokerAccount) {
        scope.launch {
            mutex.withLock {
                connectJob?.cancel()
                connectJob = null
                this@KiwoomMarketIndexManager.account = account
                if (connected && token.isNotEmpty()) {
                    logD("KiwoomMarketIndexManager: 이미 연결됨 — 재구독")
                    codes.forEach { wsService.send(makeRequest(it, subscribe = true)) }
                } else {
                    logD("KiwoomMarketIndexManager: 신규 연결 시작")
                    connectJob = launch {
                        val t = fetchToken(account) ?: return@launch
                        token = t
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
                wsService.disconnect()
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
                logD("KiwoomMarketIndexManager: pause")
                connectJob?.cancel()
                connectJob = null
                intentionalDisconnect = true
                wsService.disconnect()
                connected = false
            }
        }
    }

    /** 포그라운드 복귀 시: 재연결 */
    fun resume() {
        scope.launch {
            mutex.withLock {
                val currentAccount = account ?: return@withLock
                logD("KiwoomMarketIndexManager: resume")
                connectJob?.cancel()
                connectJob = launch {
                    val t = fetchToken(currentAccount) ?: return@launch
                    token = t
                    connect()
                }
            }
        }
    }

    private suspend fun fetchToken(account: BrokerAccount): String? {
        return tokenManager.getValidToken(account).getOrElse {
            logE(it, "KiwoomMarketIndexManager: 토큰 발급 실패")
            null
        }
    }

    private fun connect() {
        wsService.connect(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                logD("KiwoomMarketIndexManager: onOpen")
                intentionalDisconnect = false
                connected = true
                codes.forEach { wsService.send(makeRequest(it, subscribe = true)) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
                if (intentionalDisconnect) {
                    logD("KiwoomMarketIndexManager: intentional disconnect, skip reconnect")
                    intentionalDisconnect = false
                    return
                }
                logE(t, "KiwoomMarketIndexManager: onFailure — reconnecting")
                val currentAccount = account ?: return
                scope.launch {
                    delay(2000)
                    val newToken = fetchToken(currentAccount) ?: return@launch
                    token = newToken
                    connect()
                }
            }
        })
    }

    private fun handleMessage(text: String) {
        when {
            // PING 응답: 그대로 되돌려보낸다
            text.contains("PING") -> wsService.send(text)
            // 제어/응답용 JSON 메시지는 무시
            text.trimStart().startsWith("{") -> logD("KiwoomMarketIndexManager: system msg: $text")
            // 실시간 데이터 ("|" 구분)
            else -> {
                val index = MarketIndex.fromKiwoom(text)
                if (index.code.isBlank()) return
                indexMap[index.code] = index
                scope.launch { _indexFlow.emit(index) }
            }
        }
    }

    private fun makeRequest(code: String, subscribe: Boolean): String {
        val req = KiwoomWsRequest(
            header = KiwoomWsHeader(token = token),
            body = KiwoomWsBody(
                transactionType = if (subscribe) "1" else "2",
                transactionCode = "OPK20001",
                transactionKey = code,
            )
        )
        return json.encodeToString(req)
    }
}
