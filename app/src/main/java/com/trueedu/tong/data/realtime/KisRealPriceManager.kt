package com.trueedu.tong.data.realtime

import androidx.compose.runtime.mutableStateMapOf
import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.auth.KisApprovalKeyRequest
import com.trueedu.tong.model.ws.KisRealTimeTrade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Retrofit
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import com.trueedu.tong.utils.logI
import com.trueedu.tong.utils.logW
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisRealPriceManager @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    private val wsService: KisWebSocketService,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
    private val quoteManager: KisQuoteManager,
    private val json: Json,
) {
    private val authService: KisAuthService by lazy { retrofit.create(KisAuthService::class.java) }
    private val priceService: KisPriceService by lazy { retrofit.create(KisPriceService::class.java) }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val mutex = Mutex()
    private var approvalKey: String = ""
    private var connected = false
    private var intentionalDisconnect = false  // pause/stop 등 의도적 해제 중인지
    private var lastTradeTrId: String = ""     // 마지막 체결 TR ID (KRX↔NXT 전환 감지용)
    private var account: BrokerAccount? = null
    private val subscribedCodes = mutableSetOf<String>()
    private var connectJob: kotlinx.coroutines.Job? = null  // 진행 중인 connect 코루틴 (중복 방지)
    private var currentSubscribedTrId: String = ""          // 현재 구독 중인 체결 TR ID (H0STCNT0/H0NXCNT0/H0STANC0)
    private var transitionJob: kotlinx.coroutines.Job? = null  // 동시호가 ↔ 실시간 체결 전환 스케줄러

    // 실시간 체결가 스트림
    private val _tradeFlow = MutableSharedFlow<KisRealTimeTrade>(extraBufferCapacity = 64)
    val tradeFlow = _tradeFlow.asSharedFlow()

    // 종목코드 → 최신 체결 데이터
    val priceMap = mutableStateMapOf<String, KisRealTimeTrade>()

    // 종목코드 → REST API 초기 현재가 (WebSocket 첫 체결 전 fallback)
    val initialPriceMap = mutableStateMapOf<String, InitialPrice>()
    private val _initialPriceFlow = MutableSharedFlow<Map<String, InitialPrice>>(replay = 1)
    val initialPriceFlow = _initialPriceFlow.asSharedFlow()

    // 업종지수 (코스피/코스닥)
    private val _indexFlow = MutableSharedFlow<MarketIndex>(extraBufferCapacity = 16)
    val indexFlow = _indexFlow.asSharedFlow()
    val indexMap = mutableStateMapOf<String, MarketIndex>()
    private var indexSubscribed = false

    fun start(account: BrokerAccount, codes: List<String>) {
        scope.launch {
            mutex.withLock {
                // 진행 중인 connect 코루틴 취소 (중복 연결 방지)
                connectJob?.cancel()
                connectJob = null

                // 41건 한도: 호가 1슬롯 예약 → 체결가 최대 40종목
                val newCodes = codes.take(MAX_REALTIME_SYMBOLS).toSet()
                if (codes.size > MAX_REALTIME_SYMBOLS) {
                    logW("KisRealPriceManager: 종목 ${codes.size}개 중 ${MAX_REALTIME_SYMBOLS}개만 구독 (KIS 한도)")
                }
                this@KisRealPriceManager.account = account

                if (connected && approvalKey.isNotEmpty()) {
                    // WebSocket이 이미 연결된 상태 → 종목 구독만 교체 (재연결 불필요)
                    logD("KisRealPriceManager: 연결 유지 — 종목 교체 (기존 ${subscribedCodes.size}개 → 신규 ${newCodes.size}개)")
                    val toUnsubscribe = subscribedCodes - newCodes
                    val toSubscribe = newCodes - subscribedCodes
                    toUnsubscribe.forEach { code ->
                        subscribedCodes.remove(code)
                        wsService.send(makeRequest(code, subscribe = false))
                    }
                    toSubscribe.forEach { code ->
                        subscribedCodes.add(code)
                        wsService.send(makeRequest(code, subscribe = true))
                    }
                    priceMap.clear()
                    launch { fetchInitialPrices(account, newCodes.toList()) }
                    if (indexSubscribed) launch { fetchInitialIndexPrices(account) }
                } else {
                    // 연결이 없는 경우 → 새로 연결
                    logD("KisRealPriceManager: 신규 연결 시작 (${newCodes.size}개 종목)")
                    subscribedCodes.clear()
                    launch { fetchInitialPrices(account, newCodes.toList()) }
                    if (indexSubscribed) launch { fetchInitialIndexPrices(account) }
                    connectJob = launch {
                        val key = fetchApprovalKey(account) ?: return@launch
                        approvalKey = key
                        quoteManager.approvalKey = key
                        connect(newCodes.toList())
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
                transitionJob?.cancel()
                transitionJob = null
                intentionalDisconnect = true
                subscribedCodes.clear()
                currentSubscribedTrId = ""
                wsService.disconnect()
                connected = false
                priceMap.clear()
                initialPriceMap.clear()
            }
        }
    }

    /** 백그라운드 진입 시: WebSocket만 끊고 구독 목록/계좌는 유지 */
    fun pause() {
        scope.launch {
            mutex.withLock {
                if (!connected) return@withLock
                logD("KisRealPriceManager: pause")
                connectJob?.cancel()
                connectJob = null
                transitionJob?.cancel()
                transitionJob = null
                intentionalDisconnect = true
                wsService.disconnect()
                connected = false
            }
        }
    }

    /** 포그라운드 복귀 시: 기존 구독 목록으로 재연결 + 초기값 재조회 */
    fun resume() {
        scope.launch {
            mutex.withLock {
                val currentAccount = account ?: return@withLock
                val codes = subscribedCodes.toList()
                if (codes.isEmpty()) return@withLock
                logD("KisRealPriceManager: resume (${codes.size}종목)")
                connectJob?.cancel()
                launch { fetchInitialPrices(currentAccount, codes) }
                connectJob = launch {
                    val key = fetchApprovalKey(currentAccount) ?: return@launch
                    approvalKey = key
                    quoteManager.approvalKey = key
                    connect(codes)
                }
            }
        }
    }

    fun subscribe(codes: List<String>) {
        scope.launch {
            mutex.withLock {
                if (!connected || approvalKey.isEmpty()) return@withLock
                codes.forEach { code ->
                    if (subscribedCodes.size >= MAX_REALTIME_SYMBOLS) {
                        logW("KisRealPriceManager: 구독 한도(${MAX_REALTIME_SYMBOLS}) 초과 — $code 구독 스킵")
                        return@forEach
                    }
                    if (subscribedCodes.add(code)) {
                        wsService.send(makeRequest(code, subscribe = true))
                    }
                }
            }
        }
    }

    fun unsubscribe(codes: List<String>) {
        scope.launch {
            mutex.withLock {
                codes.forEach { code ->
                    if (subscribedCodes.remove(code)) {
                        wsService.send(makeRequest(code, subscribe = false))
                    }
                }
            }
        }
    }

    /** 호가 구독 (H0STASP0 or H0NXASP0) */
    fun subscribeQuote(code: String) {
        if (!connected || approvalKey.isEmpty()) return
        val req = KisWsRequest(
            header = KisWsHeader(approvalKey = approvalKey, transactionType = "1"),
            body = KisWsBody(input = KisWsBodyInput(transactionId = quoteTransactionId(), transactionKey = code))
        )
        wsService.send(json.encodeToString(req))
    }

    /** 코스피/코스닥 업종지수 구독 (기존 WebSocket 세션에 추가) + 초기값 즉시 조회 */
    fun subscribeIndex() {
        scope.launch {
            mutex.withLock {
                logD("KisRealPriceManager: subscribeIndex")
                // WebSocket 구독 (연결된 경우)
                if (connected && approvalKey.isNotEmpty() && !indexSubscribed) {
                    wsService.send(makeIndexRequest(MarketIndex.KIS_KOSPI, subscribe = true))
                    wsService.send(makeIndexRequest(MarketIndex.KIS_KOSDAQ, subscribe = true))
                }
                indexSubscribed = true
                // 계좌 있으면 REST로 초기값 즉시 조회 (장 마감 후에도 종가 반환)
                account?.let { launch { fetchInitialIndexPrices(it) } }
            }
        }
    }

    /** 코스피/코스닥 업종지수 구독 해제 */
    fun unsubscribeIndex() {
        scope.launch {
            mutex.withLock {
                if (!indexSubscribed) return@withLock
                logD("KisRealPriceManager: unsubscribeIndex")
                wsService.send(makeIndexRequest(MarketIndex.KIS_KOSPI, subscribe = false))
                wsService.send(makeIndexRequest(MarketIndex.KIS_KOSDAQ, subscribe = false))
                indexSubscribed = false
            }
        }
    }

    /** 호가 구독 해제 */
    fun unsubscribeQuote(code: String) {
        if (approvalKey.isEmpty()) return
        val req = KisWsRequest(
            header = KisWsHeader(approvalKey = approvalKey, transactionType = "2"),
            body = KisWsBody(input = KisWsBodyInput(transactionId = quoteTransactionId(), transactionKey = code))
        )
        wsService.send(json.encodeToString(req))
    }

    private suspend fun fetchApprovalKey(account: BrokerAccount): String? {
        return try {
            val resp = authService.getApprovalKey(
                KisApprovalKeyRequest(
                    appKey = credentialStorage.getAppKey(account.id),
                    secretKey = credentialStorage.getAppSecret(account.id),
                )
            )
            resp.body()?.approvalKey?.also { logD("KIS approval key 발급: $it") }
        } catch (e: Exception) {
            logE(e, "KIS approval key 발급 실패")
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
            logE(it, "KIS 초기 현재가: 토큰 발급 실패")
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
                    logW("KIS 초기 현재가 실패: code=$code, msg=${body?.msg1}")
                }
            } catch (e: Exception) {
                logE(e, "KIS 초기 현재가 조회 오류: code=$code")
            }
            delay(60) // 초당 20건 제한 대응
        }
    }

    /**
     * 코스피/코스닥 업종지수 초기값 REST API로 즉시 조회.
     * WebSocket 이벤트가 오기 전, 장 마감 후에도 종가를 표시하기 위해 사용.
     * tr_id: FHKUP03500100, FID_COND_MRKT_DIV_CODE=U
     */
    private suspend fun fetchInitialIndexPrices(account: BrokerAccount) {
        val token = tokenManager.getValidToken(account).getOrElse {
            logE(it, "KIS 업종지수 초기값: 토큰 발급 실패")
            return
        }
        val headers = mapOf(
            "authorization" to "Bearer $token",
            "appkey" to credentialStorage.getAppKey(account.id),
            "appsecret" to credentialStorage.getAppSecret(account.id),
            "tr_id" to "FHKUP03500100",
            "custtype" to "P",
        )
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            .format(java.util.Date())
        listOf(MarketIndex.KIS_KOSPI, MarketIndex.KIS_KOSDAQ).forEach { code ->
            try {
                val queries = mapOf(
                    "FID_COND_MRKT_DIV_CODE" to "U",
                    "FID_INPUT_ISCD" to code,
                    "FID_INPUT_DATE_1" to today,
                    "FID_INPUT_DATE_2" to today,
                    "FID_PERIOD_DIV_CODE" to "D",
                )
                val body = priceService.getIndexPrice(headers, queries).body()
                val output = body?.output1
                if (body?.rtCd == "0" && output != null) {
                    val index = MarketIndex(
                        code = code,
                        price = output.price.toDoubleOrNull() ?: 0.0,
                        delta = output.delta.toDoubleOrNull() ?: 0.0,
                        rate = output.rate.toDoubleOrNull() ?: 0.0,
                    )
                    indexMap[code] = index
                    _indexFlow.emit(index)
                    logD("KisRealPriceManager: 업종지수 초기값 $code = ${index.price}")
                } else {
                    logW("KIS 업종지수 초기값 실패: code=$code, msg=${body?.msg1}")
                }
            } catch (e: Exception) {
                logE(e, "KIS 업종지수 초기값 조회 오류: code=$code")
            }
            delay(60)
        }
    }

    private fun connect(codes: List<String>) {
        wsService.connect(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                intentionalDisconnect = false
                connected = true
                // 현재 시간 기준 올바른 체결 TR로 초기 구독
                currentSubscribedTrId = currentTradeTrId()
                lastTradeTrId = currentSubscribedTrId
                logI("KisRealPriceManager: onOpen trId=$currentSubscribedTrId codes=${codes.size}개")
                // 연결 후 종목 구독
                codes.forEach { code ->
                    subscribedCodes.add(code)
                    wsService.send(makeRequest(code, subscribe = true, trId = currentSubscribedTrId))
                }
                // 지수 구독 복구
                if (indexSubscribed) {
                    wsService.send(makeIndexRequest(MarketIndex.KIS_KOSPI, subscribe = true))
                    wsService.send(makeIndexRequest(MarketIndex.KIS_KOSDAQ, subscribe = true))
                }
                // 동시호가 ↔ 실시간 체결 전환 스케줄러 시작
                startTransitionScheduler()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
                // pause()/stop() 등 의도적 해제로 인한 onFailure는 재연결하지 않음
                if (intentionalDisconnect) {
                    logD("KisRealPriceManager: intentional disconnect, skip reconnect")
                    intentionalDisconnect = false
                    return
                }
                logE(t, "KisRealPriceManager: onFailure — reconnecting")
                val currentAccount = account ?: return
                val codesToReconnect = subscribedCodes.toList()
                if (codesToReconnect.isEmpty()) return
                // 재연결 시 approval key 재발급 (ALREADY IN USE 오류 방지)
                scope.launch {
                    delay(2000)
                    val newKey = fetchApprovalKey(currentAccount) ?: return@launch
                    approvalKey = newKey
                    quoteManager.approvalKey = newKey
                    connect(codesToReconnect)
                }
            }
        })
    }

    /**
     * 동시호가 시간대(08:50~09:00, 15:20~15:30) 진입/이탈 시점에 체결 구독 TR을
     * 실시간 체결(H0STCNT0) ↔ 예상체결(H0STANC0)로 자동 전환한다.
     *
     * 전환 경계(08:50, 09:00, 15:20, 15:30)까지 delay 후,
     * 목표 TR이 현재 구독 TR과 다르면 기존 구독을 모두 해제하고 새 TR로 재구독한다.
     */
    private fun startTransitionScheduler() {
        transitionJob?.cancel()
        transitionJob = scope.launch {
            while (true) {
                val waitMillis = millisUntilNextTransition().coerceAtLeast(0)
                logD("KisRealPriceManager: 다음 체결 TR 전환까지 ${waitMillis / 1000}초")
                delay(waitMillis)
                mutex.withLock {
                    if (!connected || approvalKey.isEmpty()) return@withLock
                    val targetTrId = currentTradeTrId()
                    if (targetTrId == currentSubscribedTrId) return@withLock
                    val oldTrId = currentSubscribedTrId
                    val codes = subscribedCodes.toList()
                    val enteringSimultaneous = targetTrId == "H0STANC0"
                    val leavingSimultaneous = oldTrId == "H0STANC0"
                    logI("KisRealPriceManager: 체결 TR 전환 $oldTrId → $targetTrId (${codes.size}종목)")
                    if (leavingSimultaneous) {
                        // 동시호가 이탈: 기존 연결 끊고 재연결 (swap 시 서버 측 구독 불안정 문제 방지)
                        logI("KisRealPriceManager: 동시호가 이탈 → WebSocket 재연결")
                        intentionalDisconnect = true
                        connected = false
                        currentSubscribedTrId = targetTrId
                        lastTradeTrId = targetTrId
                        wsService.disconnect()
                        val currentAccount = account ?: return@withLock
                        delay(500)
                        val newKey = fetchApprovalKey(currentAccount) ?: return@withLock
                        approvalKey = newKey
                        quoteManager.approvalKey = newKey
                        connect(codes)
                        quoteManager.resumeAfterSimultaneousQuote()
                    } else {
                        // 동시호가 진입: 호가 구독 중단 후 TR swap
                        if (enteringSimultaneous) quoteManager.pauseForSimultaneousQuote()
                        codes.forEach { code ->
                            logD("KisRealPriceManager: $code 구독 해제($oldTrId) → 구독($targetTrId)")
                            wsService.send(makeRequest(code, subscribe = false, trId = oldTrId))
                            wsService.send(makeRequest(code, subscribe = true, trId = targetTrId))
                        }
                        currentSubscribedTrId = targetTrId
                        lastTradeTrId = targetTrId
                        logI("KisRealPriceManager: TR 전환 완료 → $targetTrId")
                    }
                }
            }
        }
    }

    private fun handleMessage(text: String) {
        when {
            text.startsWith("0|") -> {
                // 실시간 데이터 (비암호화) - "0|tr_id|count|data"
                val parts = text.split("|")
                if (parts.size < 4) return
                val trId = parts[1]
                when (trId) {
                    "H0STCNT0", "H0NXCNT0" -> {
                        val trade = KisRealTimeTrade.from(parts[3])
                        priceMap[trade.code] = trade
                        scope.launch { _tradeFlow.emit(trade) }
                        // 체결 TR ID가 바뀌면(KRX↔NXT 전환) 호가 구독도 갱신
                        if (trId != lastTradeTrId) {
                            lastTradeTrId = trId
                            quoteManager.refreshSubscription()
                        }
                    }
                    "H0STANC0" -> {
                        // 동시호가 시간대 예상체결
                        logI("KisRealPriceManager: H0STANC0 수신 raw=${parts[3].take(80)}")
                        val trade = KisRealTimeTrade.fromExpected(parts[3])
                        logI("KisRealPriceManager: 예상체결 code=${trade.code} price=${trade.price} rate=${trade.rate}")
                        priceMap[trade.code] = trade
                        scope.launch { _tradeFlow.emit(trade) }
                    }
                    "H0STASP0", "H0NXASP0" -> {
                        val quote = com.trueedu.tong.model.ws.KisRealTimeQuote.from(parts[3])
                        quoteManager.onRealtimeQuote(quote)
                    }
                    "H0UPCNT0", "H0NXUPC0" -> {
                        val index = MarketIndex.fromKis(parts[3])
                        if (index.code.isNotEmpty()) {
                            indexMap[index.code] = index
                            scope.launch { _indexFlow.emit(index) }
                        }
                    }
                }
            }
            text.contains("PINGPONG") -> {
                // PingPong 응답
                wsService.send(text)
            }
            text.startsWith("1|") -> {
                // 암호화 실시간 데이터 — 복호화 미지원, 수신 여부만 로깅
                val parts = text.split("|")
                val trId = parts.getOrNull(1) ?: "?"
                logI("KisRealPriceManager: 암호화 데이터 수신 trId=$trId (복호화 미지원, 원문=${text.take(60)})")
            }
            else -> {
                logD("KisRealPriceManager: system msg: $text")
                // 구독 응답(ACK) 로깅 — 특히 H0STANC0 구독 성공/실패 확인용
                if (text.contains("H0STANC0")) {
                    logI("KisRealPriceManager: H0STANC0 구독 응답: $text")
                }
            }
        }
    }

    private fun makeRequest(
        code: String,
        subscribe: Boolean,
        trId: String = currentSubscribedTrId.ifEmpty { tradeTransactionId() },
    ): String {
        val req = KisWsRequest(
            header = KisWsHeader(
                approvalKey = approvalKey,
                transactionType = if (subscribe) "1" else "2",
            ),
            body = KisWsBody(
                input = KisWsBodyInput(
                    transactionId = trId,
                    transactionKey = code,
                )
            )
        )
        return json.encodeToString(req)
    }

    private fun makeIndexRequest(code: String, subscribe: Boolean): String {
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
         * KIS WebSocket 1세션 최대 등록 건수: 41건
         * 체결가(H0STCNT0/H0NXCNT0) + 호가(H0STASP0/H0NXASP0) + 예상체결 + 체결통보 합산.
         * 호가는 화면 진입 시 1종목만 사용하므로 체결가용으로 40종목 예약.
         */
        const val MAX_REALTIME_SYMBOLS = 40

        /**
         * NXT 운영 시간: 08:00~08:50, 15:30~20:00
         * 08:50~09:00은 동시호가(예상체결) 시간이므로 NXT 구독에서 제외한다.
         */
        fun isNxtTradingHour(): Boolean {
            val cal = java.util.Calendar.getInstance()
            val totalMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            return totalMinutes in 8 * 60 until 8 * 60 + 50 ||
                   totalMinutes in 15 * 60 + 30 until 20 * 60
        }

        /**
         * 동시호가(단일가) 시간: 08:50~09:00(장 시작), 15:20~15:30(장 마감).
         * 이 시간엔 실시간 체결이 없으므로 예상체결(H0STANC0)로 전환한다.
         */
        fun isSimultaneousQuoteTime(): Boolean {
            val cal = java.util.Calendar.getInstance()
            val totalMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            return totalMinutes in 8 * 60 + 50 until 9 * 60 ||
                   totalMinutes in 15 * 60 + 20 until 15 * 60 + 30
        }

        /**
         * 현재 시간 기준 체결 TR ID.
         * 동시호가 → H0STANC0(예상체결), NXT 시간 → H0NXCNT0, 그 외 → H0STCNT0.
         */
        fun currentTradeTrId(): String = when {
            isSimultaneousQuoteTime() -> "H0STANC0"
            isNxtTradingHour() -> "H0NXCNT0"
            else -> "H0STCNT0"
        }

        // 하위 호환: 내부 구현은 currentTradeTrId()로 통일
        fun tradeTransactionId() = currentTradeTrId()
        fun quoteTransactionId() = if (isNxtTradingHour()) "H0NXASP0" else "H0STASP0"
        fun indexTransactionId() = if (isNxtTradingHour()) "H0NXUPC0" else "H0UPCNT0"

        /**
         * 다음 구독 전환 시점까지 남은 밀리초.
         * 전환 경계: 08:50, 09:00, 15:20, 15:30 (분 단위).
         * 경계가 지난 시점에는 다음 날 첫 경계(08:50)까지 대기.
         */
        fun millisUntilNextTransition(): Long {
            val cal = java.util.Calendar.getInstance()
            val nowMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val secondsIntoMinute = cal.get(java.util.Calendar.SECOND)
            val millisIntoSecond = cal.get(java.util.Calendar.MILLISECOND)

            val boundaries = listOf(8 * 60 + 50, 9 * 60, 15 * 60 + 20, 15 * 60 + 30)
            val nextBoundary = boundaries.firstOrNull { it > nowMinutes } ?: (boundaries.first() + 24 * 60)
            val minutesUntil = nextBoundary - nowMinutes
            // (남은 분 - 1)분 + (현재 분에서 남은 초/밀리초)
            return (minutesUntil - 1) * 60_000L + (60 - secondsIntoMinute) * 1_000L - millisIntoSecond
        }
    }
}
