package com.trueedu.tong.data.realtime

import androidx.compose.runtime.mutableStateMapOf
import com.trueedu.tong.di.TossOkHttp
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.ws.TossRealTimeTrade
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import com.trueedu.tong.utils.logI
import com.trueedu.tong.utils.logW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 토스증권 실시간 체결가 (웹소켓 wss://openapi-ws.tossinvest.com/ws/v1).
 *
 * - 구독은 선언형 full-replace: 배열 1개가 현재 구독 전체이므로 종목이 바뀌면 전체를 다시 보낸다.
 * - 연결당 구독 100건 한도 (채널×종목). 체결만 구독하므로 종목 100개까지.
 * - 서버는 수신이 180초간 없으면 끊으므로 60초마다 텍스트 PING 을 보낸다.
 * - 끊기면 지수 백오프로 재연결하고 구독을 다시 선언한다.
 */
@Singleton
class TossRealtimeManager @Inject constructor(
    @TossOkHttp okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val json: Json,
) {
    companion object {
        private const val WS_URL = "wss://openapi-ws.tossinvest.com/ws/v1"
        private const val MAX_TOPICS = 100
        private const val PING_INTERVAL_MS = 60_000L
        private const val MAX_BACKOFF_MS = 30_000L

        const val OWNER_HOME = "home"
        const val OWNER_WATCH = "watch"
    }

    // 웹소켓은 수신이 없는 구간이 길 수 있어 read timeout 을 끈다 (연결 유지는 PING 으로)
    private val client: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private var account: BrokerAccount? = null
    // 구독 요청자(owner) → (국내 코드, 미국 티커). 실제 구독은 합집합
    private val owners = mutableMapOf<String, Pair<List<String>, List<String>>>()
    private var krCodes: List<String> = emptyList()
    private var usCodes: List<String> = emptyList()

    private var webSocket: WebSocket? = null
    private var connected = false
    private var intentionalClose = false
    private var connectJob: Job? = null
    private var pingJob: Job? = null
    private var retryCount = 0

    // 실시간 체결 스트림
    private val _tradeFlow = MutableSharedFlow<TossRealTimeTrade>(extraBufferCapacity = 64)
    val tradeFlow = _tradeFlow.asSharedFlow()

    // 종목 심볼 → 최신 체결
    val priceMap = mutableStateMapOf<String, TossRealTimeTrade>()

    /**
     * [owner] 의 구독 종목을 교체한다. 여러 화면(홈 보유종목, 관심종목 등)이 같은 연결을 공유하므로
     * 실제 구독은 모든 owner 의 합집합이다. 연결이 있으면 선언만 다시 보내고, 없으면 연결한다.
     * @param krCodes 국내 6자리 종목코드
     * @param usCodes 미국 티커
     */
    fun start(account: BrokerAccount, krCodes: List<String>, usCodes: List<String>, owner: String = OWNER_HOME) {
        scope.launch {
            mutex.withLock {
                owners[owner] = krCodes.map { it.removePrefix("A") }.distinct() to usCodes.distinct()
                val changedAccount = this@TossRealtimeManager.account?.id != account.id
                this@TossRealtimeManager.account = account
                applyOwnersLocked(changedAccount)
            }
        }
    }

    /** [owner] 의 구독을 해제한다. 남은 owner 가 없으면 연결을 닫는다. */
    fun stop(owner: String = OWNER_HOME) {
        scope.launch {
            mutex.withLock {
                owners.remove(owner)
                if (owners.isEmpty()) account = null
                applyOwnersLocked(changedAccount = false)
            }
        }
    }

    /** 모든 owner 의 구독을 합쳐 반영한다 (mutex 보유 상태에서 호출). */
    private fun applyOwnersLocked(changedAccount: Boolean) {
        val kr = owners.values.flatMap { it.first }.distinct()
        val us = owners.values.flatMap { it.second }.distinct()
        if (kr.size + us.size > MAX_TOPICS) {
            logW("TossRealtimeManager: 종목 ${kr.size + us.size}개 중 ${MAX_TOPICS}개만 구독 (토스 한도)")
        }
        krCodes = kr
        usCodes = us

        if (kr.isEmpty() && us.isEmpty()) {
            closeLocked()
            priceMap.clear()
            return
        }
        if (changedAccount) closeLocked()
        priceMap.keys.retainAll((kr + us).toSet())

        if (connected) {
            declareLocked()
        } else if (connectJob?.isActive != true) {
            connectLocked()
        }
    }

    /** 백그라운드 진입: 연결만 끊고 구독 목록은 유지 */
    fun pause() {
        scope.launch { mutex.withLock { closeLocked() } }
    }

    /** 포그라운드 복귀: 유지된 구독 목록으로 재연결 */
    fun resume() {
        scope.launch {
            mutex.withLock {
                if (account == null || (krCodes.isEmpty() && usCodes.isEmpty())) return@withLock
                if (!connected && connectJob?.isActive != true) connectLocked()
            }
        }
    }

    // ---- 내부 (mutex 보유 상태에서 호출) ----

    private fun closeLocked() {
        intentionalClose = true
        connectJob?.cancel()
        connectJob = null
        pingJob?.cancel()
        pingJob = null
        webSocket?.close(1000, "client close")
        webSocket = null
        connected = false
        retryCount = 0
    }

    private fun connectLocked() {
        val acc = account ?: return
        connectJob = scope.launch {
            // 재연결 대기: 1s → 2s → 4s ... 최대 30s
            if (retryCount > 0) {
                val backoff = minOf(1000L shl (retryCount - 1).coerceAtMost(5), MAX_BACKOFF_MS)
                logD("TossRealtimeManager: ${backoff}ms 후 재연결 (retry=$retryCount)")
                delay(backoff)
            }
            val token = tokenManager.getValidToken(acc).getOrNull()
            if (token == null) {
                logW("TossRealtimeManager: 토큰 획득 실패")
                scheduleReconnect()
                return@launch
            }
            openSocket(token)
        }
    }

    private fun openSocket(token: String) {
        intentionalClose = false
        val request = Request.Builder()
            .url(WS_URL)
            .header("Authorization", "Bearer $token")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (webSocket !== this@TossRealtimeManager.webSocket) return
                logI("TossRealtimeManager: onOpen kr=${krCodes.size} us=${usCodes.size}")
                scope.launch {
                    mutex.withLock {
                        connected = true
                        retryCount = 0
                        declareLocked()
                        startPingLocked()
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (webSocket !== this@TossRealtimeManager.webSocket) return
                handleMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleClosed(webSocket, "closed $code $reason", null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleClosed(webSocket, t.message ?: "failure", response?.code)
            }
        })
    }

    private fun handleClosed(closed: WebSocket, reason: String, httpCode: Int?) {
        scope.launch {
            mutex.withLock {
                // 이미 교체/정리된 이전 소켓의 콜백은 무시
                if (closed !== webSocket) return@withLock
                connected = false
                pingJob?.cancel()
                pingJob = null
                if (intentionalClose) return@withLock
                logE("TossRealtimeManager: 연결 종료 ($reason, http=$httpCode) — 재연결")
                // 토큰 만료/무효면 다음 연결 전에 갱신
                if (httpCode == 401) account?.let { tokenManager.invalidateToken(it.id) }
                scheduleReconnectLocked()
            }
        }
    }

    private fun scheduleReconnect() {
        scope.launch { mutex.withLock { scheduleReconnectLocked() } }
    }

    private fun scheduleReconnectLocked() {
        if (account == null || (krCodes.isEmpty() && usCodes.isEmpty())) return
        retryCount++
        connectLocked()
    }

    /** 현재 구독 전체를 선언한다 (full-replace). */
    private fun declareLocked() {
        val ws = webSocket ?: return
        val kr = krCodes.take(MAX_TOPICS)
        val us = usCodes.take((MAX_TOPICS - kr.size).coerceAtLeast(0))
        val declaration = buildJsonArray {
            if (kr.isNotEmpty()) add(buildJsonObject {
                put("type", "trade:kr")
                put("codes", JsonArray(kr.map { JsonPrimitive(it) }))
            })
            if (us.isNotEmpty()) add(buildJsonObject {
                put("type", "trade:us")
                put("codes", JsonArray(us.map { JsonPrimitive(it) }))
            })
        }
        logD("TossRealtimeManager: 구독 선언 kr=${kr.size} us=${us.size}")
        ws.send(declaration.toString())
    }

    private fun startPingLocked() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (true) {
                delay(PING_INTERVAL_MS)
                webSocket?.send("PING")
            }
        }
    }

    private fun handleMessage(text: String) {
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "message" -> handleTrade(obj)
            "subscriptions" -> {
                val rejected = obj["rejected"]?.jsonArray
                if (rejected != null && rejected.isNotEmpty()) {
                    logW("TossRealtimeManager: 구독 거부 $rejected")
                }
            }
            "error" -> {
                val code = obj["error"]?.jsonObject?.get("code")?.jsonPrimitive?.contentOrNull
                logE("TossRealtimeManager: 서버 에러 $code")
                if (code == "rate-limit-exceeded") {
                    // 약 1초 대기 후 재선언
                    scope.launch {
                        delay(1000)
                        mutex.withLock { if (connected) declareLocked() }
                    }
                }
            }
            "pong" -> Unit
        }
    }

    private fun handleTrade(obj: JsonObject) {
        val trade = TossRealTimeTrade.fromFrame(obj) ?: return
        priceMap[trade.symbol] = trade
        _tradeFlow.tryEmit(trade)
    }
}
