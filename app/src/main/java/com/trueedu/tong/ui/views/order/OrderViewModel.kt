package com.trueedu.tong.ui.views.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.data.realtime.KisQuoteManager
import com.trueedu.tong.data.realtime.KisRealPriceManager
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.Local
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
) : ViewModel() {

    var code: String by mutableStateOf(""); private set
    var stockName: String by mutableStateOf(""); private set
    var account: BrokerAccount? by mutableStateOf(null); private set

    // 홈에서 종목 선택 시 증가 (OrderScreen의 LaunchedEffect key)
    var pendingCode by mutableStateOf(""); private set

    val quoteData get() = kisQuoteManager.quoteData
    val realtimeQuote get() = kisQuoteManager.realtimeQuote
    val realtimePrice get() = kisPriceManager.priceMap[code.removePrefix("A")]

    var isMarket by mutableStateOf(false); private set
    var quantity by mutableStateOf("1"); private set
    var price by mutableStateOf(""); private set

    sealed class OrderState {
        object Idle : OrderState()
        object Loading : OrderState()
        data class Success(val msg: String) : OrderState()
        data class Error(val msg: String) : OrderState()
    }
    var orderState: OrderState by mutableStateOf(OrderState.Idle); private set

    companion object {
        const val DEFAULT_CODE = "005930" // 삼성전자
    }

    init {
        viewModelScope.launch {
            val savedCode = local.selectedOrderCode.ifBlank { DEFAULT_CODE }
            val savedAccountId = local.selectedOrderAccountId
            // 계좌가 없어도 종목 코드는 세팅 (호가만 표시)
            val acc = if (savedAccountId != -1L) brokerAccountRepo.getAll().first().find { it.id == savedAccountId }
                      else brokerAccountRepo.getAll().first().firstOrNull()
            if (acc != null) {
                loadOrder(savedCode, acc.id)
            } else {
                // 계좌 없어도 종목코드는 저장
                code = savedCode
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
        if (newCode == code && accountId == account?.id) return
        code = newCode      // TopBar 즉시 반영
        stockName = name    // 종목 이름 즉시 반영
        kisQuoteManager.stop()
        loadOrder(newCode, accountId)
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
}
