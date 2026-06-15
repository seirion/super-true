package com.trueedu.tong.ui.views.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.data.realtime.KisRealPriceManager
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.CandleData
import com.trueedu.tong.model.CandlePeriod
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.CredentialStorage
import com.trueedu.tong.repository.local.Local
import com.trueedu.tong.repository.remote.CandleRepository
import com.trueedu.tong.utils.logD
import com.trueedu.tong.utils.logW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CandleViewModel @Inject constructor(
    private val candleRepo: CandleRepository,
    private val brokerAccountRepo: BrokerAccountRepository,
    private val credentialStorage: CredentialStorage,
    private val kisRealPriceManager: KisRealPriceManager,
    private val local: Local,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(
            val candles: List<CandleData>,
            val broker: BrokerType,
            val isRealtimeUpdate: Boolean = false,
        ) : State()
        object NoAccount : State()
        data class Error(val msg: String) : State()
    }

    var state by mutableStateOf<State>(State.Idle); private set

    // 현재 선택된 기간 (기간 변경 시 재조회에 사용)
    var currentPeriod by mutableStateOf(CandlePeriod.DAY); private set
    // 분봉 간격 (1, 3, 5, 10, 30, 60분) — 앱 재시작 시 복원
    var minuteInterval by mutableStateOf(1); private set

    // 마지막으로 로드한 종목코드 (중복 호출 방지)
    private var loadedCode: String? = null

    // 외부에서 현재 종목코드 접근 (기간 변경 재조회 등)
    val currentCode: String get() = loadedCode ?: ""

    // 실시간 체결 가격 (KIS WebSocket)
    val realtimePrice get() = kisRealPriceManager.priceMap[currentCode]

    // 키움 → LS → KIS 우선순위
    private val priority = listOf(BrokerType.KIWOOM, BrokerType.LS, BrokerType.KIS)

    init {
        // 저장된 분봉 간격 복원
        minuteInterval = local.lastMinuteInterval.takeIf { it in listOf(1, 3, 5, 10, 30, 60) } ?: 1
        // KIS WebSocket 실시간 체결 → 차트 갱신
        kisRealPriceManager.tradeFlow
            .onEach { trade -> updateWithRealtimeTrade(trade) }
            .launchIn(viewModelScope)
    }

    /** KIS 실시간 체결 데이터로 마지막 캔들 갱신 (또는 분봉 신규 추가) */
    private fun updateWithRealtimeTrade(trade: KisRealTimeTrade) {
        val s = state as? State.Success ?: return
        // 현재 보고 있는 종목과 다르면 무시
        if (trade.code != loadedCode) return
        if (trade.price <= 0.0) return

        val candles = s.candles.toMutableList()
        val last = candles.lastOrNull() ?: return
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        when (currentPeriod) {
            CandlePeriod.MINUTE -> {
                // 분봉: 현재 분(HHmm) 비교
                val currentMin = if (trade.time.length >= 4) trade.time.substring(0, 4) else return
                val lastMin = if (last.datetime.length >= 12) last.datetime.substring(8, 12)
                             else if (last.datetime.length >= 4) last.datetime.substring(0, 4)
                             else return
                if (currentMin != lastMin) {
                    // 새 분봉 추가
                    val newDatetime = "${today}${trade.time.padEnd(6, '0')}"
                    candles.add(CandleData(newDatetime, trade.price, trade.price, trade.price, trade.price, 0L))
                    logD("CandleViewModel: 새 분봉 추가 $newDatetime")
                } else {
                    // 현재 분봉 갱신
                    candles[candles.lastIndex] = last.copy(
                        high = maxOf(last.high, trade.price),
                        low = if (last.low > 0) minOf(last.low, trade.price) else trade.price,
                        close = trade.price,
                        volume = trade.volume.toLong(),
                    )
                }
            }
            CandlePeriod.DAY -> {
                // 일봉: 오늘 날짜 캔들 갱신 (시가는 KIS WebSocket의 open 필드 사용)
                if (last.datetime.take(8) == today) {
                    candles[candles.lastIndex] = last.copy(
                        open = if (trade.open > 0 && last.open == 0.0) trade.open else last.open,
                        high = maxOf(last.high, trade.high.takeIf { it > 0 } ?: trade.price),
                        low = if (last.low > 0) minOf(last.low, trade.low.takeIf { it > 0 } ?: trade.price) else trade.price,
                        close = trade.price,
                        volume = trade.volume.toLong(),
                    )
                }
            }
            CandlePeriod.WEEK, CandlePeriod.MONTH -> {
                // 주/월봉: 마지막 봉의 close/high/low만 갱신
                candles[candles.lastIndex] = last.copy(
                    high = maxOf(last.high, trade.price),
                    low = if (last.low > 0) minOf(last.low, trade.price) else trade.price,
                    close = trade.price,
                )
            }
        }
        state = State.Success(candles, s.broker, isRealtimeUpdate = true)
    }

    /**
     * 캔들 데이터 로드. 우선순위대로 appKey 가 있는 계좌를 찾아 호출한다.
     * 기간이 바뀌면 강제로 재조회한다.
     */
    fun changeMinuteInterval(interval: Int) {
        if (interval == minuteInterval && currentPeriod == CandlePeriod.MINUTE) return
        minuteInterval = interval
        local.lastMinuteInterval = interval  // 저장
        if (currentCode.isNotBlank()) load(currentCode, CandlePeriod.MINUTE, force = true)
    }

    fun load(code: String, period: CandlePeriod = currentPeriod, force: Boolean = false) {
        val target = code.removePrefix("A")
        if (target.isBlank()) {
            state = State.Idle
            return
        }
        val periodChanged = period != currentPeriod
        currentPeriod = period
        val mustReload = force || periodChanged
        if (!mustReload && target == loadedCode && state is State.Success) return
        loadedCode = target

        viewModelScope.launch {
            state = State.Loading
            val accounts = brokerAccountRepo.getAll().first()
                .filter { it.isActive && credentialStorage.getAppKey(it.id).isNotBlank() }

            if (accounts.isEmpty()) {
                state = State.NoAccount
                return@launch
            }

            var lastError: String? = null
            for (broker in priority) {
                val account = accounts.firstOrNull { it.brokerType == broker } ?: continue
                logD("CandleViewModel: $target 조회 시도 - ${broker.displayName}")
                val result = fetch(broker, account, target, currentPeriod, minuteInterval)
                result
                    .onSuccess {
                        logD("CandleViewModel: ${broker.displayName} 성공 - ${it.size}개 캔들")
                        state = State.Success(it, broker)
                        return@launch
                    }
                    .onFailure {
                        lastError = it.message
                        logW("CandleViewModel: ${broker.displayName} 실패 - ${it.message}")
                    }
            }
            state = State.Error(lastError ?: "캔들 데이터를 가져오지 못했습니다")
        }
    }

    /**
     * 추가(과거) 데이터 로드. 차트 스크롤이 끝에 도달했을 때 호출된다.
     *
     * TODO: 실제 연속조회는 증권사별 연속조회 키(키움 next-key, LS tr_cont_key,
     *  KIS cts_date 등) 관리가 필요하다. 현재는 중복 호출만 방지하는 stub.
     */
    fun loadMore() {
        if (state is State.Loading) return
        // 연속조회 미구현: 추가 데이터 없음
        logD("CandleViewModel: loadMore 호출 (연속조회 미구현)")
    }

    private suspend fun fetch(
        broker: BrokerType,
        account: BrokerAccount,
        code: String,
        period: CandlePeriod,
        interval: Int = 1,
    ): Result<List<CandleData>> = when (broker) {
        BrokerType.KIWOOM -> candleRepo.fetchKiwoom(account, code, period, interval)
        BrokerType.LS -> candleRepo.fetchLs(account, code, period, interval)
        BrokerType.KIS -> candleRepo.fetchKis(account, code, period)
        BrokerType.TOSS -> Result.failure(UnsupportedOperationException("토스증권 미지원"))
    }
}
