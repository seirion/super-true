package com.trueedu.tong.ui.views.schedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.StockInfoLocal
import com.trueedu.tong.model.dto.order.ScheduleOrderItem
import com.trueedu.tong.model.dto.order.ScheduleOrderRequest
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.StockLocal
import com.trueedu.tong.repository.remote.ScheduleOrderUseCase
import com.trueedu.tong.repository.remote.StockInfoRepository
import com.trueedu.tong.utils.logD
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleOrderViewModel @Inject constructor(
    private val brokerAccountRepo: BrokerAccountRepository,
    private val scheduleOrderUseCase: ScheduleOrderUseCase,
    private val stockInfoRepo: StockInfoRepository,
    private val stockLocal: StockLocal,
) : ViewModel() {

    sealed class State {
        object Loading : State()
        data class Success(val items: List<ScheduleOrderItem>) : State()
        object NoAccount : State()
        /** 선택된 계좌의 증권사가 예약주문을 지원하지 않음 */
        data class Unsupported(val brokerName: String) : State()
        data class Error(val msg: String) : State()
    }

    var state by mutableStateOf<State>(State.Loading); private set

    /** 목록/정정/취소를 수행하는 계좌 — 홈 drawer 에서 선택한 계좌 기준 */
    var account: BrokerAccount? by mutableStateOf(null); private set

    /**
     * 등록 화면의 계좌.
     * 주문 화면에서 진입하면 그 화면에 표시된 계좌를, 그 외에는 선택 계좌를 쓴다.
     */
    var addAccount: BrokerAccount? by mutableStateOf(null); private set

    /** 등록 화면의 계좌가 예약주문을 지원하는지 */
    val addSupported: Boolean
        get() = addAccount?.let { scheduleOrderUseCase.isSupported(it.brokerType) } == true

    /** 등록/정정/취소 결과 메시지 (스낵바) */
    var actionMessage: String? by mutableStateOf(null); private set

    var submitting by mutableStateOf(false); private set

    /** [accountId] 를 주면 그 계좌를, 없거나 못 찾으면 선택 계좌를 반환한다 */
    private suspend fun resolveAccount(accountId: Long = -1L): BrokerAccount? {
        val accounts = brokerAccountRepo.getAll().first()
        return accounts.firstOrNull { it.id == accountId }
            ?: accounts.firstOrNull { it.isSelected }
            ?: accounts.firstOrNull()
    }

    /**
     * 예약주문 목록 조회.
     * [target] 을 주면 그 계좌로, 없으면 선택 계좌로 조회한다.
     */
    fun load(target: BrokerAccount? = null) {
        viewModelScope.launch {
            state = State.Loading
            val acc = target ?: resolveAccount()
            account = acc
            if (acc == null) {
                state = State.NoAccount
                return@launch
            }
            if (!scheduleOrderUseCase.isSupported(acc.brokerType)) {
                state = State.Unsupported(acc.brokerType.displayName)
                return@launch
            }
            scheduleOrderUseCase.getList(acc)
                .onSuccess { state = State.Success(it) }
                .onFailure { state = State.Error(it.message ?: "예약주문을 가져오지 못했습니다") }
        }
    }

    fun clearActionMessage() { actionMessage = null }

    /** '처리' 상태 예약을 탭했을 때 안내 */
    fun notifyDisabled() { actionMessage = "처리 완료된 예약입니다" }

    // ---------------------------------------------------------------
    // 등록
    // ---------------------------------------------------------------

    var code by mutableStateOf(""); private set
    var stockName by mutableStateOf(""); private set
    var priceInput by mutableStateOf(""); private set
    var quantityInput by mutableStateOf("1"); private set

    /**
     * 등록 화면 진입 시 초기화. 주문 화면에서 넘어오면 종목/가격/수량이 채워진다.
     *
     * 계좌는 진입할 때마다 다시 resolve 한다. 화면이 activity 스코프 ViewModel 을 쓰기 때문에
     * 캐시된 계좌를 재사용하면 drawer 에서 계좌를 바꾼 뒤에도 이전 계좌로 예약이 등록된다.
     */
    fun startAdd(code: String, price: String, quantity: String, accountId: Long = -1L) {
        val target = code.removePrefix("A")
        this.code = target
        priceInput = price
        quantityInput = quantity.ifBlank { "1" }
        stockName = ""
        addAccount = null
        viewModelScope.launch {
            addAccount = resolveAccount(accountId)
            if (target.isNotBlank()) {
                launch { fillStockName(target) }
                if (price.isBlank()) launch { fillCurrentPrice(target) }
            }
        }
    }

    fun selectStock(stock: StockInfoLocal) {
        code = stock.code.removePrefix("A")
        stockName = stock.nameKr
        if (priceInput.isBlank()) {
            viewModelScope.launch { fillCurrentPrice(code) }
        }
    }

    private suspend fun fillStockName(target: String) {
        stockName = stockLocal.getAllStocks().firstOrNull { it.code.removePrefix("A") == target }?.nameKr ?: ""
    }

    /** 종목 선택 시 현재가를 기본 주문가로 채운다. 실패하면 사용자가 직접 입력한다. */
    private suspend fun fillCurrentPrice(target: String) {
        val acc = addAccount ?: return
        if (!scheduleOrderUseCase.isSupported(acc.brokerType)) return
        stockInfoRepo.fetchKis(acc, target)
            .onSuccess { info ->
                if (code == target) {
                    if (stockName.isBlank()) stockName = info.name
                    if (priceInput.isBlank() && info.currentPrice > 0) {
                        priceInput = info.currentPrice.toLong().toString()
                    }
                }
            }
            .onFailure { logD("ScheduleOrderViewModel: 현재가 조회 실패 - ${it.message}") }
    }

    fun onPriceChange(v: String) { if (v.all { it.isDigit() }) priceInput = v }
    fun onQuantityChange(v: String) { if (v.all { it.isDigit() }) quantityInput = v }
    fun incrementPrice() { priceInput = (price + priceStep(price.toDouble())).toString() }
    fun decrementPrice() { priceInput = maxOf(0L, price - priceStep(price.toDouble())).toString() }
    fun incrementQuantity() { quantityInput = (quantity + 1).toString() }
    fun decrementQuantity() { quantityInput = maxOf(1L, quantity - 1).toString() }

    private val price: Long get() = priceInput.toLongOrNull() ?: 0L
    private val quantity: Long get() = quantityInput.toLongOrNull() ?: 0L

    val orderAmount: Long get() = price * quantity
    val addValid: Boolean get() = code.isNotBlank() && price > 0 && quantity > 0

    fun submitAdd(isBuy: Boolean, onSuccess: () -> Unit) {
        if (!addValid || submitting) return
        val acc = addAccount ?: run {
            actionMessage = "등록된 계좌가 없습니다"
            return
        }
        if (!scheduleOrderUseCase.isSupported(acc.brokerType)) {
            actionMessage = "${acc.brokerType.displayName}은 예약주문을 지원하지 않습니다"
            return
        }
        viewModelScope.launch {
            submitting = true
            scheduleOrderUseCase.place(
                acc,
                ScheduleOrderRequest(code = code, isBuy = isBuy, price = price, quantity = quantity),
            )
                .onSuccess {
                    actionMessage = if (isBuy) "예약 매수 등록 완료" else "예약 매도 등록 완료"
                    submitting = false
                    // 등록한 계좌가 목록 계좌와 다를 수 있어 등록 계좌 기준으로 목록을 갱신한다
                    load(acc)
                    onSuccess()
                }
                .onFailure {
                    actionMessage = it.message ?: "예약주문 등록 실패"
                    submitting = false
                }
        }
    }

    // ---------------------------------------------------------------
    // 정정 / 취소
    // ---------------------------------------------------------------

    fun cancel(item: ScheduleOrderItem) {
        if (item.disabled) {
            actionMessage = "처리 완료된 예약입니다"
            return
        }
        val acc = account ?: return
        viewModelScope.launch {
            scheduleOrderUseCase.cancel(acc, item.seq)
                .onSuccess { actionMessage = "예약 취소 완료"; load(acc) }
                .onFailure { actionMessage = it.message ?: "예약 취소 실패" }
        }
    }

    fun modify(item: ScheduleOrderItem, newPrice: Long, newQuantity: Long) {
        val acc = account ?: return
        viewModelScope.launch {
            scheduleOrderUseCase.modify(
                acc,
                item.seq,
                ScheduleOrderRequest(
                    code = item.code,
                    isBuy = item.isBuy,
                    price = newPrice,
                    quantity = newQuantity,
                ),
            )
                .onSuccess { actionMessage = "예약 정정 완료"; load(acc) }
                .onFailure { actionMessage = it.message ?: "예약 정정 실패" }
        }
    }

    // ---------------------------------------------------------------
    // 종목 검색 (등록 화면)
    // ---------------------------------------------------------------

    private var allStocks: List<StockInfoLocal> = emptyList()
    var searchQuery by mutableStateOf(""); private set
    var searchResults: List<StockInfoLocal> by mutableStateOf(emptyList()); private set

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

    companion object {
        fun priceStep(price: Double): Long = when {
            price < 2_000 -> 1L
            price < 5_000 -> 5L
            price < 20_000 -> 10L
            price < 50_000 -> 50L
            price < 200_000 -> 100L
            price < 500_000 -> 500L
            else -> 1_000L
        }
    }
}
