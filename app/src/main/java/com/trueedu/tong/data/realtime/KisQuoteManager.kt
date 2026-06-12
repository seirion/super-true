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
import kotlinx.serialization.encodeToString
import retrofit2.Retrofit
import timber.log.Timber
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

    // approval key는 KisRealPriceManager에서 관리 — 여기서는 setter로 받아서 사용
    var approvalKey: String = ""

    fun start(code: String) {
        currentCode = code
        quoteData.value = null
        realtimeQuote.value = null
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
        if (quote.code == currentCode) realtimeQuote.value = quote
    }

    private suspend fun fetchInitialQuote(account: BrokerAccount, code: String) {
        try {
            val token = tokenManager.getValidToken(account).getOrElse { return }
            val headers = mapOf(
                "authorization" to "Bearer $token",
                "appkey" to credentialStorage.getAppKey(account.id),
                "appsecret" to credentialStorage.getAppSecret(account.id),
                "tr_id" to "FHKST01010200",
                "custtype" to "P",
            )
            val queries = mapOf("FID_COND_MRKT_DIV_CODE" to "J", "FID_INPUT_ISCD" to code)
            val resp = priceService.getQuote(headers, queries)
            if (code == currentCode) quoteData.value = resp.body()
        } catch (e: Exception) { Timber.e(e, "KisQuoteManager: 호가 조회 실패") }
    }
}
