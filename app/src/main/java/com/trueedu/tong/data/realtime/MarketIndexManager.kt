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
 * 활성 계좌의 증권사 종류에 따라 [KisMarketIndexManager] 또는 [KiwoomMarketIndexManager] 를 사용하고,
 * 종목코드를 KIS 기준("0001"/"1001")으로 정규화하여 코스피/코스닥 스트림을 노출한다.
 */
@Singleton
class MarketIndexManager @Inject constructor(
    private val kisManager: KisMarketIndexManager,
    private val kiwoomManager: KiwoomMarketIndexManager,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // 현재 어떤 매니저를 사용 중인지 (pause/resume 분기용)
    private var activeBroker: BrokerType? = null

    private val _kospiFlow = MutableSharedFlow<MarketIndex>(extraBufferCapacity = 16)
    val kospiFlow = _kospiFlow.asSharedFlow()

    private val _kosdaqFlow = MutableSharedFlow<MarketIndex>(extraBufferCapacity = 16)
    val kosdaqFlow = _kosdaqFlow.asSharedFlow()

    // 현재값 (KIS 기준 코드로 정규화하여 보관)
    var kospi: MarketIndex? by mutableStateOf(null)
        private set
    var kosdaq: MarketIndex? by mutableStateOf(null)
        private set

    init {
        // 두 매니저의 스트림을 구독하여 정규화 후 코스피/코스닥으로 분기
        scope.launch {
            kisManager.indexFlow.collect { dispatch(it) }
        }
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
                if (activeBroker == BrokerType.KIS) kisManager.stop()
                activeBroker = BrokerType.KIWOOM
                kiwoomManager.start(account)
            }
            // 코스피/코스닥 지수는 KIS 업종지수로 제공 (KIS/LS/TOSS 계좌 공통)
            else -> {
                if (activeBroker == BrokerType.KIWOOM) kiwoomManager.stop()
                activeBroker = BrokerType.KIS
                kisManager.start(account)
            }
        }
    }

    fun stop() {
        when (activeBroker) {
            BrokerType.KIWOOM -> kiwoomManager.stop()
            else -> kisManager.stop()
        }
        kospi = null
        kosdaq = null
    }

    fun pause() {
        when (activeBroker) {
            BrokerType.KIWOOM -> kiwoomManager.pause()
            else -> kisManager.pause()
        }
    }

    fun resume() {
        when (activeBroker) {
            BrokerType.KIWOOM -> kiwoomManager.resume()
            else -> kisManager.resume()
        }
    }
}
