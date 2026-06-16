package com.trueedu.tong.data.realtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.utils.logD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 코스피/코스닥 실시간 지수 통합 관리자(Facade).
 *
 * KIS: [KisRealPriceManager]의 기존 WebSocket에 H0UPCNT0/H0NXUPC0 TR을 추가 구독
 * 키움: [KiwoomMarketIndexManager]를 통해 별도 WebSocket 구독
 */
@Singleton
class MarketIndexManager @Inject constructor(
    private val kisRealPriceManager: KisRealPriceManager,
    private val kiwoomManager: KiwoomMarketIndexManager,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var activeBroker: BrokerType? = null

    private val _kospiFlow = MutableSharedFlow<MarketIndex>(extraBufferCapacity = 16)
    val kospiFlow = _kospiFlow.asSharedFlow()

    private val _kosdaqFlow = MutableSharedFlow<MarketIndex>(extraBufferCapacity = 16)
    val kosdaqFlow = _kosdaqFlow.asSharedFlow()

    var kospi: MarketIndex? by mutableStateOf(null)
        private set
    var kosdaq: MarketIndex? by mutableStateOf(null)
        private set

    init {
        // KisRealPriceManager의 indexFlow 구독
        scope.launch {
            kisRealPriceManager.indexFlow.collect { dispatch(it) }
        }
        // 키움 매니저의 indexFlow 구독
        scope.launch {
            kiwoomManager.indexFlow.collect { dispatch(it) }
        }
    }

    private fun dispatch(index: MarketIndex) {
        when {
            MarketIndex.isKospi(index.code) -> {
                val normalized = index.copy(code = MarketIndex.KIS_KOSPI)
                kospi = normalized
                scope.launch { _kospiFlow.emit(normalized) }
            }
            MarketIndex.isKosdaq(index.code) -> {
                val normalized = index.copy(code = MarketIndex.KIS_KOSDAQ)
                kosdaq = normalized
                scope.launch { _kosdaqFlow.emit(normalized) }
            }
        }
    }

    fun start(account: BrokerAccount) {
        logD("MarketIndexManager: start (${account.brokerType})")
        when (account.brokerType) {
            BrokerType.KIWOOM -> {
                if (activeBroker == BrokerType.KIS) kisRealPriceManager.unsubscribeIndex()
                activeBroker = BrokerType.KIWOOM
                kiwoomManager.start(account)
            }
            else -> {
                if (activeBroker == BrokerType.KIWOOM) kiwoomManager.stop()
                activeBroker = BrokerType.KIS
                // 기존 WebSocket 세션에 지수 TR 추가 구독
                kisRealPriceManager.subscribeIndex()
            }
        }
    }

    fun stop() {
        when (activeBroker) {
            BrokerType.KIWOOM -> kiwoomManager.stop()
            else -> kisRealPriceManager.unsubscribeIndex()
        }
        activeBroker = null
        kospi = null
        kosdaq = null
    }

    fun pause() {
        when (activeBroker) {
            BrokerType.KIWOOM -> kiwoomManager.pause()
            else -> Unit // KIS는 KisRealPriceManager.pause()가 WebSocket을 처리함
        }
    }

    fun resume() {
        when (activeBroker) {
            BrokerType.KIWOOM -> kiwoomManager.resume()
            else -> {
                // KisRealPriceManager.resume() 후 연결되면 onOpen에서 지수도 재구독됨
                Unit
            }
        }
    }
}
