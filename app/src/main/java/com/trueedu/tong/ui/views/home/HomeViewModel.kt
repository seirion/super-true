package com.trueedu.tong.ui.views.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.data.realtime.KisRealPriceManager
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.repository.AccountCacheRepository
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.remote.AccountSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    // 표시 모드: false=평가, true=시세
    var marketPriceMode by mutableStateOf(false)
        private set

    fun toggleMode() {
        marketPriceMode = !marketPriceMode
    }

    // KIS 실시간 체결가 (종목코드 → 최신 체결). 체결 발생 시 갱신.
    // 일단 expose만 — UI 반영은 다음 단계.
    val realtimePrices: StateFlow<Map<String, KisRealTimeTrade>> = kisRealPriceManager.tradeFlow
        .map { kisRealPriceManager.priceMap.toMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        // 선택된 계좌가 바뀌면 자동으로 데이터 로딩 (캐시 우선)
        viewModelScope.launch {
            selectedAccount.collectLatest { account ->
                // 계좌 전환 시 이전 실시간 연결 정리
                kisRealPriceManager.stop()
                if (account != null) loadFromCacheOrFetch(account)
                else _uiState.value = UiState.Idle
            }
        }
    }

    private suspend fun loadFromCacheOrFetch(account: BrokerAccount) {
        // 캐시 먼저 시도
        val cached = cacheRepo.load(account.id)
        if (cached != null) {
            _uiState.value = UiState.Success(cached)
            startRealtimeIfKis(account, cached)
            return
        }
        // 캐시 없으면 API 호출
        fetchAndCache(account)
    }

    // KIS 계좌이면 보유 종목 코드로 실시간 시세 구독 시작
    private fun startRealtimeIfKis(account: BrokerAccount, summary: AccountSummary) {
        if (account.brokerType == BrokerType.KIS) {
            val codes = summary.holdings.map { it.code }
            kisRealPriceManager.start(account, codes)
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
                startRealtimeIfKis(account, it)
            }
            .onFailure { _uiState.value = UiState.Error(it.message ?: "오류가 발생했습니다") }
    }

    override fun onCleared() {
        super.onCleared()
        kisRealPriceManager.stop()
    }
}
