package com.trueedu.tong.ui.views.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.data.realtime.KisQuoteManager
import com.trueedu.tong.utils.logD
import com.trueedu.tong.data.realtime.KisRealPriceManager
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.StockInfoLocal
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.Local
import com.trueedu.tong.repository.local.StockLocal
import com.trueedu.tong.repository.remote.OrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val brokerAccountRepo: BrokerAccountRepository,
    private val orderUseCase: OrderUseCase,
    private val kisQuoteManager: KisQuoteManager,
    private val kisPriceManager: KisRealPriceManager,
    private val local: Local,
    private val stockLocal: StockLocal,
) : ViewModel() {

    var code: String by mutableStateOf(""); private set

    // 마지막 탭 인덱스 (앱 재시작 시 복원)
    val lastTabIndex: Int get() = local.lastOrderTabIndex
    fun saveTabIndex(index: Int) { local.lastOrderTabIndex = index }
    var stockName: String by mutableStateOf(""); private set
    var account: BrokerAccount? by mutableStateOf(null); private set

    // 홈에서 종목 선택 시 증가 (OrderScreen의 LaunchedEffect key)
    var pendingCode by mutableStateOf(""); private set

    val quoteData get() = kisQuoteManager.quoteData
    val realtimeQuote get() = kisQuoteManager.realtimeQuote
    val priceDetail get() = kisQuoteManager.priceData.value  // HLOCW - Composable에서 직접 읽기 위해 .value 노출
    val realtimePrice get() = kisPriceManager.priceMap[code.removePrefix("A")]

    var isMarket by mutableStateOf(false); private set
    var quantity by mutableStateOf("1"); private set
    var price by mutableStateOf(""); private set

    // 수정 모드: null=신규주문, 값있으면=미체결 주문 수정
    var modifyOrder by mutableStateOf<com.trueedu.tong.model.dto.order.UnfilledOrderItem?>(null); private set
    val isModifyMode get() = modifyOrder != null

    sealed class OrderState {
        object Idle : OrderState()
        object Loading : OrderState()
        data class Success(val msg: String) : OrderState()
        data class Error(val msg: String) : OrderState()
    }
    var orderState: OrderState by mutableStateOf(OrderState.Idle); private set

    // 종목 검색
    private var allStocks: List<StockInfoLocal> = emptyList()
    var searchQuery by mutableStateOf(""); private set
    var searchResults: List<StockInfoLocal> by mutableStateOf(emptyList()); private set

    companion object {
        const val DEFAULT_CODE = "005930" // 삼성전자
    }

    init {
        viewModelScope.launch {
            val savedCode = local.selectedOrderCode.ifBlank { DEFAULT_CODE }
            val savedAccountId = local.selectedOrderAccountId
            val acc = if (savedAccountId != -1L) brokerAccountRepo.getAll().first().find { it.id == savedAccountId }
                      else brokerAccountRepo.getAll().first().firstOrNull()
            if (acc != null) {
                loadOrder(savedCode, acc.id)
            } else {
                code = savedCode
            }
        }
        // priceData 로드 완료 시 stockName 자동 업데이트
        viewModelScope.launch {
            androidx.compose.runtime.snapshotFlow { kisQuoteManager.priceData.value }
                .collect { detail ->
                    if (detail != null && detail.nameKr.isNotBlank() && stockName.isBlank()) {
                        stockName = detail.nameKr
                    }
                }
        }
    }

    /** 주문 탭 진입 시 Local에 저장된 pending 종목 확인 */
    fun checkPendingStock() {
        val pendingCode = local.selectedOrderCode
        val pendingAccountId = local.selectedOrderAccountId
        if (pendingCode.isNotBlank() && pendingAccountId != -1L &&
            (pendingCode != code || pendingAccountId != account?.id)) {
            loadOrder(pendingCode, pendingAccountId)
        }
    }

    /** 홈화면 종목 탭 시 호출 (Local + pendingCode 업데이트) */
    fun selectStock(newCode: String, name: String, accountId: Long) {
        local.selectedOrderCode = newCode
        local.selectedOrderAccountId = accountId
        local.selectedOrderTimestamp = System.currentTimeMillis()
        pendingCode = newCode
        modifyOrder = null  // 홈에서 종목 탭 → 정정 모드 해제
        modifyEnteredFromUnfilled = false
        if (newCode == code && accountId == account?.id) return
        code = newCode      // TopBar 즉시 반영
        stockName = name    // 종목 이름 즉시 반영
        kisQuoteManager.stop()
        loadOrder(newCode, accountId)
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

    private fun loadOrder(newCode: String, accountId: Long) {
        viewModelScope.launch {
            val acc = brokerAccountRepo.getAll().first().find { it.id == accountId } ?: return@launch
            code = newCode
            account = acc
            price = ""
            isMarket = false
            quantity = "1"
            kisQuoteManager.start(newCode.removePrefix("A"))
            val initPrice = kisPriceManager.initialPriceMap[newCode.removePrefix("A")]
            if (initPrice != null) price = initPrice.price.toLong().toString()
        }
    }

    override fun onCleared() {
        super.onCleared()
        kisQuoteManager.stop()
    }

    fun onMarketToggle(v: Boolean) { isMarket = v }
    fun onQuantityChange(v: String) { if (v.all { it.isDigit() }) quantity = v }
    fun onPriceChange(v: String) { if (v.all { it.isDigit() }) price = v }
    fun setPrice(p: Double) { price = p.toLong().toString() }
    fun incrementQuantity() { quantity = ((quantity.toIntOrNull() ?: 0) + 1).toString() }
    fun decrementQuantity() { quantity = maxOf(1, (quantity.toIntOrNull() ?: 1) - 1).toString() }
    fun incrementPrice(step: Long) { price = ((price.toLongOrNull() ?: 0L) + step).toString() }
    fun decrementPrice(step: Long) { price = maxOf(0L, (price.toLongOrNull() ?: 0L) - step).toString() }

    val orderAmount: Long get() = (quantity.toLongOrNull() ?: 0L) * (price.toLongOrNull() ?: 0L)
    val isValid: Boolean get() = code.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0 && (isMarket || (price.toLongOrNull() ?: 0L) > 0)

    fun placeOrder(isBuy: Boolean) {
        val acc = account ?: return
        logD("OrderViewModel.placeOrder: code=$code, brokerType=${acc.brokerType}, accountId=${acc.id}")
        viewModelScope.launch {
            orderState = OrderState.Loading
            val req = OrderRequest(
                code = code,
                quantity = quantity.toIntOrNull() ?: return@launch,
                price = if (isMarket) 0L else price.toLongOrNull() ?: return@launch,
                isBuy = isBuy,
                isMarket = isMarket,
            )
            orderUseCase.placeOrder(acc, req)
                .onSuccess { orderState = OrderState.Success(if (isBuy) "매수 주문 완료" else "매도 주문 완료") }
                .onFailure { orderState = OrderState.Error(it.message ?: "주문 오류") }
        }
    }

    fun resetState() { orderState = OrderState.Idle }

    // 미체결→주문 탭 전환으로 진입했는지 여부 (외부 탭 진입 시 정정 모드 해제 판별용)
    var modifyEnteredFromUnfilled by mutableStateOf(false); private set

    /** 미체결 주문 수정 모드 진입 — 해당 종목 로드 + 가격 세팅 */
    fun enterModifyMode(order: com.trueedu.tong.model.dto.order.UnfilledOrderItem, accountId: Long) {
        modifyEnteredFromUnfilled = true
        modifyOrder = order
        val newCode = order.code
        val newPrice = order.ordPrice
        // 종목 로드 (이미 같은 종목이면 호가만 유지, 다르면 전환)
        if (newCode != code) {
            code = newCode
            stockName = order.name
            kisQuoteManager.start(newCode.removePrefix("A"))
        }
        price = newPrice.toString()
        quantity = order.remainQty.toString()
        isMarket = false
    }

    fun exitModifyMode() {
        modifyOrder = null
        modifyEnteredFromUnfilled = false
    }

    /** 외부(다른 bottom tab)에서 주문 탭으로 진입 시 호출 — 정정 모드 해제 */
    fun onOrderTabEntered(selectedAccountId: Long = -1L) {
        if (modifyEnteredFromUnfilled) {
            modifyEnteredFromUnfilled = false
        } else {
            modifyOrder = null
            // 현재 선택된 계좌가 다르면 account 업데이트
            if (selectedAccountId != -1L && selectedAccountId != account?.id) {
                local.selectedOrderAccountId = selectedAccountId
                viewModelScope.launch {
                    val acc = brokerAccountRepo.getAll().first().find { it.id == selectedAccountId }
                    if (acc != null) {
                        account = acc
                        stockName = ""  // 계좌 바뀌면 종목 이름도 초기화
                    }
                }
            }
        }
    }

    /** 정정 주문 실행 */
    fun submitModify(statusVm: OrderStatusViewModel) {
        val order = modifyOrder ?: return
        val acc = account ?: return
        viewModelScope.launch {
            orderState = OrderState.Loading
            statusVm.modify(order, price.toLongOrNull() ?: 0L)
            modifyOrder = null
            orderState = OrderState.Idle
        }
    }
}
