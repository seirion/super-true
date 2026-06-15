package com.trueedu.tong.ui.views.watch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.data.realtime.InitialPrice
import com.trueedu.tong.data.realtime.KisRealPriceManager
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.StockInfoLocal
import com.trueedu.tong.model.WatchlistItem
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.WatchlistRepository
import com.trueedu.tong.repository.local.StockLocal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchViewModel @Inject constructor(
    private val watchlistRepo: WatchlistRepository,
    private val stockLocal: StockLocal,
    private val kisRealPriceManager: KisRealPriceManager,
    private val brokerAccountRepo: BrokerAccountRepository,
) : ViewModel() {

    // 관심종목 목록
    val watchlist: StateFlow<List<WatchlistItem>> = watchlistRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 실시간 체결가
    val realtimePrices: StateFlow<Map<String, KisRealTimeTrade>> = kisRealPriceManager.tradeFlow
        .map { kisRealPriceManager.priceMap.toMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // 초기 현재가 (REST fallback)
    val initialPrices: StateFlow<Map<String, InitialPrice>> = kisRealPriceManager.initialPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // 검색 화면 표시 여부
    var showSearch by mutableStateOf(false)

    // 종목 검색
    private var allStocks: List<StockInfoLocal> = emptyList()
    var searchQuery by mutableStateOf(""); private set
    var searchResults: List<StockInfoLocal> by mutableStateOf(emptyList()); private set

    init {
        // 관심종목 목록이 바뀌면 실시간 시세 구독 갱신
        viewModelScope.launch {
            watchlist.collect { items ->
                if (items.isNotEmpty()) startRealtimeIfKis(items.map { it.code })
            }
        }
    }

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

    /** 검색 화면 진입 시 호출 — 종목 목록 캐시 로드 */
    fun loadStocksForSearch() {
        if (allStocks.isNotEmpty()) return
        viewModelScope.launch {
            allStocks = stockLocal.getAllStocks()
            applySearch()
        }
    }

    fun onSearchQueryChange(q: String) {
        searchQuery = q
        applySearch()
    }

    private fun applySearch() {
        val q = searchQuery.trim()
        searchResults = if (q.isBlank()) {
            allStocks
        } else {
            allStocks.filter { it.nameKr.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true) }
        }
    }

    fun openSearch() { showSearch = true }

    fun closeSearch() {
        showSearch = false
        onSearchQueryChange("")
    }

    fun addToWatchlist(code: String, nameKr: String) {
        viewModelScope.launch {
            watchlistRepo.add(code, nameKr)
        }
    }

    fun removeFromWatchlist(code: String) {
        viewModelScope.launch {
            watchlistRepo.remove(code)
        }
    }
}
