package com.trueedu.tong.ui.views.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.data.realtime.InitialPrice
import com.trueedu.tong.data.realtime.KisRealPriceManager
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.repository.AccountCacheRepository
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.Local
import com.trueedu.tong.repository.remote.AccountSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val brokerAccountRepo: BrokerAccountRepository,
    private val accountSummaryUseCase: AccountSummaryUseCase,
    private val cacheRepo: AccountCacheRepository,
    private val kisRealPriceManager: KisRealPriceManager,
    private val local: Local,
) : ViewModel() {

    // 선택된 계좌
    val selectedAccount: StateFlow<BrokerAccount?> = brokerAccountRepo.getAll()
        .map { it.firstOrNull { acc -> acc.isSelected } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI 상태
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val summary: AccountSummary) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 표시 모드: false=평가, true=시세 (SharedPreferences에 영속 저장)
    var marketPriceMode by mutableStateOf(local.marketPriceMode)
        private set

    fun toggleMode() {
        marketPriceMode = !marketPriceMode
        local.marketPriceMode = marketPriceMode
    }

    // 요약 섹션 접기/펼치기 (SharedPreferences에 영속 저장)
    var summaryExpanded by mutableStateOf(local.summaryExpanded)
        private set

    fun toggleSummary() {
        summaryExpanded = !summaryExpanded
        local.summaryExpanded = summaryExpanded
    }



    // KIS 실시간 체결가 (종목코드 → 최신 체결). 체결 발생 시 갱신.
    // 일단 expose만 — UI 반영은 다음 단계.
    val realtimePrices: StateFlow<Map<String, KisRealTimeTrade>> = kisRealPriceManager.tradeFlow
        .map { kisRealPriceManager.priceMap.toMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // KIS 초기 현재가 (WebSocket 첫 체결 전 fallback)
    val initialPrices: StateFlow<Map<String, InitialPrice>> = kisRealPriceManager.initialPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * 현재 거래 시간대
     * - KRX: 정규장 09:00~15:30
     * - NXT: 장외 08:00~09:00, 15:30~20:00
     * - null: 거래 없음 (20:00~08:00)
     */
    enum class MarketSession { KRX, NXT }

    var marketSession by mutableStateOf(currentMarketSession())
        private set

    private fun currentMarketSession(): MarketSession? {
        val cal = java.util.Calendar.getInstance()
        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        val total = h * 60 + m
        return when {
            total in 9 * 60 until 15 * 60 + 30 -> MarketSession.KRX
            total in 8 * 60 until 9 * 60 || total in 15 * 60 + 30 until 20 * 60 -> MarketSession.NXT
            else -> null
        }
    }

    init {
        // 1분마다 거래 시간 갱신
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                marketSession = currentMarketSession()
            }
        }
        // 선택된 계좌가 바뀌면 자동으로 데이터 로딩 (캐시 우선)
        viewModelScope.launch {
            selectedAccount.collectLatest { account ->
                if (account != null) loadFromCacheOrFetch(account)
                else {
                    // 로그아웃 등 계좌 없는 경우에만 완전 정리
                    kisRealPriceManager.stop()
                    _uiState.value = UiState.Idle
                }
            }
        }
    }

    private suspend fun loadFromCacheOrFetch(account: BrokerAccount) {
        // 캐시 먼저 시도
        val cached = cacheRepo.load(account.id)
        if (cached != null) {
            _uiState.value = UiState.Success(cached)
            startRealtimeIfKis(cached.holdings.map { it.code })
            return
        }
        // 캐시 없으면 API 호출
        fetchAndCache(account)
    }

    // 선택된 계좌와 무관하게, KIS 계좌가 하나라도 있으면 해당 계좌로 실시간 시세 구독
    // 종목코드 정규화: 키움 등은 "A000660" 형식 → KIS WebSocket은 "000660" 형식
    private fun startRealtimeIfKis(codes: List<String>) {
        viewModelScope.launch {
            val allAccounts = brokerAccountRepo.getAll().first()
            val kisAccount = allAccounts.firstOrNull { it.brokerType == BrokerType.KIS }
            if (kisAccount != null && codes.isNotEmpty()) {
                val normalizedCodes = codes.map { it.removePrefix("A") }
                kisRealPriceManager.start(kisAccount, normalizedCodes)
            }
        }
    }

    /** 홈 탭 활성화 시 호출 — 보유 종목으로 실시간 시세 구독 교체 */
    fun activateRealtime() {
        val success = uiState.value
        if (success is UiState.Success) {
            startRealtimeIfKis(success.summary.holdings.map { it.code })
        }
    }

    fun refresh() {
        viewModelScope.launch {
            selectedAccount.value?.let { fetchAndCache(it) }
        }
    }

    private suspend fun fetchAndCache(account: BrokerAccount) {
        _uiState.value = UiState.Loading
        accountSummaryUseCase.fetch(account)
            .onSuccess {
                cacheRepo.save(it)
                _uiState.value = UiState.Success(it)
                startRealtimeIfKis(it.holdings.map { h -> h.code })
            }
            .onFailure { _uiState.value = UiState.Error(it.message ?: "오류가 발생했습니다") }
    }

    override fun onCleared() {
        super.onCleared()
        kisRealPriceManager.stop()
    }
}
