package com.trueedu.tong.ui.views.watch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.data.realtime.InitialPrice
import com.trueedu.tong.data.realtime.KisRealPriceManager
import com.trueedu.tong.data.realtime.MarketIndex
import com.trueedu.tong.data.realtime.MarketIndexManager
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchViewModel @Inject constructor(
    private val watchlistRepo: WatchlistRepository,
    private val stockLocal: StockLocal,
    private val kisRealPriceManager: KisRealPriceManager,
    private val marketIndexManager: MarketIndexManager,
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

    // 실시간 지수 (코스피/코스닥) — KIS 기준 코드("0001"/"1001") 기준 Map
    val indexMap: StateFlow<Map<String, MarketIndex>> =
        merge(marketIndexManager.kospiFlow, marketIndexManager.kosdaqFlow)
            .map {
                buildMap {
                    marketIndexManager.kospi?.let { put(MarketIndex.KIS_KOSPI, it) }
                    marketIndexManager.kosdaq?.let { put(MarketIndex.KIS_KOSDAQ, it) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // 관심 탭이 활성화된 상태인지
    private var isActive = false

    init {
        // 관심 탭이 활성화된 동안 watchlist 변경 시 구독 자동 갱신
        viewModelScope.launch {
            watchlist
                .distinctUntilChanged { old, new -> old.map { it.code } == new.map { it.code } }
                .collectLatest { items ->
                    if (isActive && items.isNotEmpty()) {
                        startRealtimeIfKis(items.map { it.code })
                    }
                }
        }
    }

    // 검색 화면 표시 여부
    var showSearch by mutableStateOf(false)

    // 종목 검색
    private var allStocks: List<StockInfoLocal> = emptyList()
    var searchQuery by mutableStateOf(""); private set
    var searchResults: List<StockInfoLocal> by mutableStateOf(emptyList()); private set

    /** 관심 탭 활성화 시 호출 — 관심종목으로 실시간 시세 구독 교체 */
    fun activateRealtime() {
        isActive = true
        viewModelScope.launch {
            // watchlist StateFlow가 아직 emptyList()인 경우(구독자 없어 로드 전)
            // 첫 번째 비어있지 않은 값을 기다리거나, 이미 로드됐으면 바로 사용
            val codes = if (watchlist.value.isNotEmpty()) {
                watchlist.value.map { it.code }
            } else {
                watchlistRepo.getAll().first().map { it.code }
            }
            if (codes.isNotEmpty()) {
                startRealtimeIfKis(codes)
            }
        }
    }

    /** 관심 탭 비활성화 시 호출 (다른 탭으로 이동) */
    fun deactivate() {
        isActive = false
        marketIndexManager.stop()
    }

    private suspend fun startRealtimeIfKis(codes: List<String>) {
        val allAccounts = brokerAccountRepo.getAll().first()
        val kisAccount = allAccounts.firstOrNull { it.brokerType == BrokerType.KIS }

        val normalizedCodes = codes.map { it.removePrefix("A") }

        // 일반 종목(지수 제외) 실시간 시세 구독
        val stockCodes = normalizedCodes.filterNot { isIndexCode(it) }
        if (kisAccount != null && stockCodes.isNotEmpty()) {
            kisRealPriceManager.start(kisAccount, stockCodes)
        }

        // 지수(코스피/코스닥) 구독: watchlist에 지수 코드가 있으면 KIS 우선, 없으면 첫 계좌로 시작
        if (normalizedCodes.any { isIndexCode(it) }) {
            val indexAccount = kisAccount ?: allAccounts.firstOrNull()
            if (indexAccount != null) {
                marketIndexManager.start(indexAccount)
            }
        }
    }

    private fun isIndexCode(code: String): Boolean =
        code == MarketIndex.KIS_KOSPI || code == MarketIndex.KIS_KOSDAQ

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
        val base = if (q.isBlank()) {
            allStocks
        } else {
            allStocks.filter { it.nameKr.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true) }
        }
        // 지수(코스피/코스닥) 고정 항목을 상단에 노출
        searchResults = matchingIndexStocks(q) + base
    }

    private fun matchingIndexStocks(q: String): List<StockInfoLocal> {
        if (q.isBlank()) return listOf(KOSPI_STOCK, KOSDAQ_STOCK)
        val lower = q.lowercase()
        val result = mutableListOf<StockInfoLocal>()
        if (KOSPI_KEYWORDS.any { it.contains(lower) }) result.add(KOSPI_STOCK)
        if (KOSDAQ_KEYWORDS.any { it.contains(lower) }) result.add(KOSDAQ_STOCK)
        return result
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

    companion object {
        // 지수 전용 검색 항목 (KIS 기준 코드)
        val KOSPI_STOCK = StockInfoLocal(code = "0001", nameKr = "코스피", attributes = "", kospi = true)
        val KOSDAQ_STOCK = StockInfoLocal(code = "1001", nameKr = "코스닥", attributes = "", kospi = false)

        private val KOSPI_KEYWORDS = listOf("코스피", "kospi", "0001", "001")
        private val KOSDAQ_KEYWORDS = listOf("코스닥", "kosdaq", "1001", "101")
    }
}
