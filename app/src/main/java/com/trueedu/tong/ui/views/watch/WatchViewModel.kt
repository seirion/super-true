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
import com.trueedu.tong.data.realtime.TossRealtimeManager
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.StockInfoLocal
import com.trueedu.tong.model.UsStockLocal
import com.trueedu.tong.model.WatchlistItem
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.model.ws.TossRealTimeTrade
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.UsStockRepository
import com.trueedu.tong.repository.WatchlistRepository
import com.trueedu.tong.repository.remote.toss.TossMarketRepository
import com.trueedu.tong.repository.local.StockLocal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val tossRealtimeManager: TossRealtimeManager,
    private val tossMarket: TossMarketRepository,
    private val usStockRepo: UsStockRepository,
) : ViewModel() {

    // 관심종목 전체 목록 (한국 + 미국)
    val watchlist: StateFlow<List<WatchlistItem>> = watchlistRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 시장별 목록
    val krWatchlist: StateFlow<List<WatchlistItem>> = watchlist
        .map { list -> list.filter { it.market == WatchlistItem.MARKET_KR } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val usWatchlist: StateFlow<List<WatchlistItem>> = watchlist
        .map { list -> list.filter { it.market == WatchlistItem.MARKET_US } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 토스 계좌 등록 여부 (미국 종목 시세/검색에 필요)
    val hasTossAccount: StateFlow<Boolean> = brokerAccountRepo.getAll()
        .map { accounts -> accounts.any { it.brokerType == BrokerType.TOSS } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // 미국 종목 실시간 체결 (토스 웹소켓). 키: 티커
    val tossPrices: StateFlow<Map<String, TossRealTimeTrade>> = tossRealtimeManager.tradeFlow
        .map { tossRealtimeManager.priceMap.toMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // 미국 종목 REST 현재가/전일 종가 (실시간 체결 전 fallback과 등락 계산용). 키: 티커
    private val _usLastPrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val usLastPrices: StateFlow<Map<String, Double>> = _usLastPrices
    private val _usPrevCloses = MutableStateFlow<Map<String, Double>>(emptyMap())
    val usPrevCloses: StateFlow<Map<String, Double>> = _usPrevCloses

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
            krWatchlist
                .distinctUntilChanged { old, new -> old.map { it.code } == new.map { it.code } }
                .collectLatest { items ->
                    if (isActive && items.isNotEmpty()) {
                        startRealtimeIfKis(items.map { it.code })
                    }
                }
        }
        viewModelScope.launch {
            usWatchlist
                .distinctUntilChanged { old, new -> old.map { it.code } == new.map { it.code } }
                .collectLatest { items ->
                    if (isActive) startUsRealtime(items.map { it.code })
                }
        }
    }

    // 편집(순서 변경) 모드. 편집은 한 시장(editMarket)의 목록에만 적용된다.
    var editMode by mutableStateOf(false)
        private set
    var editMarket by mutableStateOf(WatchlistItem.MARKET_KR)
        private set

    // 편집 중 임시 순서. 드래그 중에는 DB 대신 이 리스트만 갱신하고, 완료 시 한 번에 저장한다.
    var editList: List<WatchlistItem> by mutableStateOf(emptyList())
        private set

    fun toggleEditMode(market: String) {
        if (editMode) {
            // 편집 완료 → 현재 순서를 저장
            val ordered = editList.map { it.code }
            val savedMarket = editMarket
            editMode = false
            viewModelScope.launch {
                watchlistRepo.saveOrder(savedMarket, ordered)
            }
        } else {
            // 편집 시작 → 해당 시장의 현재 목록을 스냅샷
            editMarket = market
            editList = watchlist.value.filter { it.market == market }
            editMode = true
        }
    }

    /** 드래그&드랍 이동마다 호출 — 로컬 임시 순서만 갱신 */
    fun reorderWatchlist(from: Int, to: Int) {
        val current = editList.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        current.add(to, current.removeAt(from))
        editList = current
    }

    // 검색 화면 표시 여부와 대상 시장
    var showSearch by mutableStateOf(false)
    var searchMarket by mutableStateOf(WatchlistItem.MARKET_KR); private set

    // 미국 종목 검색 (토스 종목 유니버스 로컬 캐시)
    sealed class UsSearchState {
        object Idle : UsSearchState()
        object Loading : UsSearchState()
        object Ready : UsSearchState()
        object NoAccount : UsSearchState()   // 토스 계좌도 없고 캐시도 없음
        object Error : UsSearchState()
    }
    var usSearchState: UsSearchState by mutableStateOf(UsSearchState.Idle); private set
    private var allUsStocks: List<UsStockLocal> = emptyList()
    var usSearchResults: List<UsStockLocal> by mutableStateOf(emptyList()); private set

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
            val all = if (watchlist.value.isNotEmpty()) watchlist.value else watchlistRepo.getAll().first()
            val krCodes = all.filter { it.market == WatchlistItem.MARKET_KR }.map { it.code }
            val usCodes = all.filter { it.market == WatchlistItem.MARKET_US }.map { it.code }
            if (krCodes.isNotEmpty()) {
                startRealtimeIfKis(krCodes)
            }
            startUsRealtime(usCodes)
        }
    }

    /** 관심 탭 비활성화 시 호출 (다른 탭으로 이동) */
    fun deactivate() {
        isActive = false
        marketIndexManager.stop()
        tossRealtimeManager.stop(TossRealtimeManager.OWNER_WATCH)
    }

    /** 토스 계좌: 선택된 계좌가 토스면 그것, 아니면 등록된 첫 토스 계좌 */
    private suspend fun tossAccount(): BrokerAccount? {
        val accounts = brokerAccountRepo.getAll().first()
        return accounts.firstOrNull { it.isSelected && it.brokerType == BrokerType.TOSS }
            ?: accounts.firstOrNull { it.brokerType == BrokerType.TOSS }
    }

    /** 미국 관심종목: 토스 웹소켓 구독 + REST 현재가/전일 종가 조회 */
    private suspend fun startUsRealtime(symbols: List<String>) {
        if (symbols.isEmpty()) {
            tossRealtimeManager.stop(TossRealtimeManager.OWNER_WATCH)
            return
        }
        val account = tossAccount() ?: return
        tossRealtimeManager.start(account, emptyList(), symbols, TossRealtimeManager.OWNER_WATCH)
        loadUsQuotes(account, symbols)
    }

    private suspend fun loadUsQuotes(account: BrokerAccount, symbols: List<String>) {
        val prices = tossMarket.fetchLastPrices(account, symbols)
        if (prices.isNotEmpty()) _usLastPrices.value = _usLastPrices.value + prices
        // 전일 종가는 종목마다 일봉을 조회해야 하므로 아직 모르는 종목만 (한도: 초당 20회)
        for (symbol in symbols.filter { it !in _usPrevCloses.value }) {
            tossMarket.fetchPrevClose(account, symbol)?.let { prev ->
                _usPrevCloses.value = _usPrevCloses.value + (symbol to prev)
            }
            delay(60)
        }
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
        applyUsSearch()
    }

    /** 미국 종목 유니버스 로드 (캐시가 오래됐으면 토스에서 갱신) */
    fun loadUsStocksForSearch() {
        if (usSearchState == UsSearchState.Ready || usSearchState == UsSearchState.Loading) return
        viewModelScope.launch {
            usSearchState = UsSearchState.Loading
            val account = tossAccount()
            val cached = usStockRepo.getAll()
            val available = when {
                account != null -> usStockRepo.refreshIfStale(account)
                else -> cached.isNotEmpty()
            }
            if (!available) {
                usSearchState = if (account == null) UsSearchState.NoAccount else UsSearchState.Error
                return@launch
            }
            allUsStocks = usStockRepo.getAll()
            usSearchState = UsSearchState.Ready
            applyUsSearch()
        }
    }

    private fun applyUsSearch() {
        usSearchResults = UsStockRepository.search(allUsStocks, searchQuery)
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

    fun openSearch(market: String) {
        searchMarket = market
        showSearch = true
    }

    fun closeSearch() {
        showSearch = false
        onSearchQueryChange("")
    }

    fun addToWatchlist(code: String, nameKr: String, market: String = WatchlistItem.MARKET_KR) {
        viewModelScope.launch {
            watchlistRepo.add(code, nameKr, market)
        }
    }

    fun removeFromWatchlist(code: String, market: String = WatchlistItem.MARKET_KR) {
        viewModelScope.launch {
            watchlistRepo.remove(code, market)
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
