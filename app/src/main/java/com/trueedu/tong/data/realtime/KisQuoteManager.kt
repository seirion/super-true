package com.trueedu.tong.data.realtime

import androidx.compose.runtime.mutableStateOf
import com.trueedu.tong.di.KisRetrofitQualifier
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.dto.kis.KisQuoteResponse
import com.trueedu.tong.model.ws.KisRealTimeQuote
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.remote.auth.TokenManager
import com.trueedu.tong.repository.remote.kis.KisPriceService
import com.trueedu.tong.repository.remote.kis.KisWebSocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import retrofit2.Retrofit
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisQuoteManager @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
    private val brokerAccountRepo: BrokerAccountRepository,
    private val wsService: KisWebSocketService,
    private val realPriceManager: dagger.Lazy<KisRealPriceManager>,
    private val json: kotlinx.serialization.json.Json,
) {
    private val priceService: KisPriceService by lazy { retrofit.create(KisPriceService::class.java) }
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentCode: String? = null

    // 실시간 호가 스트림 구독 job (메인 스레드에서 collect)
    private var quoteCollectJob: Job? = null

    val quoteData = mutableStateOf<KisQuoteResponse?>(null)
    val realtimeQuote = mutableStateOf<KisRealTimeQuote?>(null)
    val priceData = mutableStateOf<com.trueedu.tong.model.dto.kis.KisPriceDetail?>(null)

    // 마지막 구독 시 사용한 TR ID — 시간대 전환 감지용
    private var lastQuoteTrId: String = ""

    // approval key는 KisRealPriceManager에서 관리 — key 세팅 시 대기 중인 종목 재구독
    var approvalKey: String = ""
        set(value) {
            field = value
            // key가 새로 세팅될 때 이미 start()된 종목이 있으면 구독 재시도
            if (value.isNotEmpty()) {
                currentCode?.let { sendQuoteSubscribe(it, subscribe = true) }
            }
        }

    /**
     * 시간대 전환 시 호가 구독 TR ID 갱신.
     * KisRealPriceManager에서 NXT↔KRX 전환 시 호출한다.
     */
    fun refreshSubscription() {
        val code = currentCode ?: return
        val newTrId = KisRealPriceManager.quoteTransactionId()
        if (newTrId == lastQuoteTrId) return  // TR ID 변경 없으면 불필요
        logD("KisQuoteManager: 호가 TR ID 전환 $lastQuoteTrId → $newTrId")
        // 기존 구독 해제 (이전 TR ID로)
        if (lastQuoteTrId.isNotEmpty() && approvalKey.isNotEmpty()) {
            val unsubReq = com.trueedu.tong.model.ws.KisWsRequest(
                header = com.trueedu.tong.model.ws.KisWsHeader(approvalKey = approvalKey, transactionType = "2"),
                body = com.trueedu.tong.model.ws.KisWsBody(
                    input = com.trueedu.tong.model.ws.KisWsBodyInput(transactionId = lastQuoteTrId, transactionKey = code)
                )
            )
            wsService.send(json.encodeToString(unsubReq))
        }
        // 새 구독 등록
        sendQuoteSubscribe(code, subscribe = true)
    }

    fun start(code: String) {
        currentCode = code
        quoteData.value = null
        realtimeQuote.value = null
        priceData.value = null
        sendQuoteSubscribe(code, subscribe = true)
        startQuoteCollect()
        scope.launch {
            val kisAccount = brokerAccountRepo.getAll().first()
                .firstOrNull { it.brokerType == BrokerType.KIS } ?: return@launch
            fetchInitialQuote(kisAccount, code)
        }
    }

    fun stop() {
        currentCode?.let { sendQuoteSubscribe(it, subscribe = false) }
        currentCode = null
        quoteData.value = null
        realtimeQuote.value = null
        quoteCollectJob?.cancel()
        quoteCollectJob = null
    }

    /**
     * KisRealPriceManager의 실시간 호가 스트림을 구독하여 현재 종목([currentCode])
     * 호가만 [realtimeQuote]에 반영한다. collect는 메인 스레드에서 진행한다.
     */
    private fun startQuoteCollect() {
        if (quoteCollectJob != null) return
        quoteCollectJob = MainScope().launch {
            realPriceManager.get().quoteFlow.collect { quote ->
                if (quote.code == currentCode) {
                    realtimeQuote.value = quote
                }
            }
        }
    }

    /**
     * 동시호가(08:50~09:00, 15:20~15:30) 진입 시 호가 구독 일시 중단.
     * 이 시간대에는 체결이 없으므로 호가 슬롯을 낭비하지 않는다.
     */
    fun pauseForSimultaneousQuote() {
        val code = currentCode ?: return
        logD("KisQuoteManager: 동시호가 진입 — 호가 구독 중단 ($code)")
        sendQuoteSubscribe(code, subscribe = false)
    }

    /**
     * 동시호가 이탈 시 호가 구독 재개.
     */
    fun resumeAfterSimultaneousQuote() {
        val code = currentCode ?: return
        logD("KisQuoteManager: 동시호가 이탈 — 호가 구독 재개 ($code)")
        sendQuoteSubscribe(code, subscribe = true)
    }

    private fun sendQuoteSubscribe(code: String, subscribe: Boolean) {
        if (approvalKey.isEmpty()) return
        val trId = KisRealPriceManager.quoteTransactionId()
        if (subscribe) lastQuoteTrId = trId
        val req = com.trueedu.tong.model.ws.KisWsRequest(
            header = com.trueedu.tong.model.ws.KisWsHeader(
                approvalKey = approvalKey,
                transactionType = if (subscribe) "1" else "2",
            ),
            body = com.trueedu.tong.model.ws.KisWsBody(
                input = com.trueedu.tong.model.ws.KisWsBodyInput(
                    transactionId = trId,
                    transactionKey = code,
                )
            )
        )
        wsService.send(json.encodeToString(req))
    }

    private suspend fun fetchInitialQuote(account: BrokerAccount, code: String) {
        try {
            val token = tokenManager.getValidToken(account).getOrElse { return }
            val commonHeaders = mapOf(
                "authorization" to "Bearer $token",
                "appkey" to credentialStorage.getAppKey(account.id),
                "appsecret" to credentialStorage.getAppSecret(account.id),
                "custtype" to "P",
            )
            // 호가 조회
            val quoteResp = priceService.getQuote(
                commonHeaders + mapOf("tr_id" to "FHKST01010200"),
                mapOf("FID_COND_MRKT_DIV_CODE" to "J", "FID_INPUT_ISCD" to code)
            )
            if (code == currentCode) quoteData.value = quoteResp.body()

            // 현재가/HLOCW 조회
            val priceResp = priceService.getCurrentPrice(
                commonHeaders + mapOf("tr_id" to "FHKST01010100"),
                mapOf("FID_COND_MRKT_DIV_CODE" to "J", "FID_INPUT_ISCD" to code)
            )
            if (code == currentCode) priceData.value = priceResp.body()?.output
        } catch (e: Exception) { logE(e, "KisQuoteManager: 조회 실패") }
    }
}
