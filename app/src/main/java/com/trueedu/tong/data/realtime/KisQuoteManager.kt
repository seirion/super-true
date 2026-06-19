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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import retrofit2.Retrofit
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logE
import com.trueedu.tong.utils.logI
import com.trueedu.tong.utils.logW
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KisQuoteManager @Inject constructor(
    @KisRetrofitQualifier private val retrofit: Retrofit,
    private val credentialStorage: CredentialStorage,
    private val tokenManager: TokenManager,
    private val brokerAccountRepo: BrokerAccountRepository,
    private val wsService: KisWebSocketService,
    private val json: kotlinx.serialization.json.Json,
) {
    private val priceService: KisPriceService by lazy { retrofit.create(KisPriceService::class.java) }
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentCode: String? = null

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

    fun onRealtimeQuote(quote: KisRealTimeQuote) {
        if (quote.code == currentCode) {
            scope.launch(Dispatchers.Main) {
                realtimeQuote.value = quote
            }
        }
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
