package com.trueedu.tong.ui.views.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.dto.order.OrderRequest
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.remote.OrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val brokerAccountRepo: BrokerAccountRepository,
    private val orderUseCase: OrderUseCase,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<OrderRoute>()
    val code: String = args.code
    private val accountId: Long = args.accountId

    var account: BrokerAccount? by mutableStateOf(null)
        private set

    var isMarket by mutableStateOf(false)
        private set
    var quantity by mutableStateOf("1")
        private set
    var price by mutableStateOf("0")
        private set

    sealed class OrderState {
        object Idle : OrderState()
        object Loading : OrderState()
        data class Success(val message: String) : OrderState()
        data class Error(val message: String) : OrderState()
    }

    var orderState: OrderState by mutableStateOf(OrderState.Idle)
        private set

    init {
        viewModelScope.launch {
            account = brokerAccountRepo.getAll().first().find { it.id == accountId }
            price = "0"
        }
    }

    fun onMarketToggle(market: Boolean) { isMarket = market }
    fun onQuantityChange(v: String) { if (v.all { it.isDigit() }) quantity = v }
    fun onPriceChange(v: String) { if (v.all { it.isDigit() }) price = v }
    fun incrementQuantity() { quantity = ((quantity.toIntOrNull() ?: 0) + 1).toString() }
    fun decrementQuantity() { quantity = maxOf(1, (quantity.toIntOrNull() ?: 1) - 1).toString() }
    fun incrementPrice(step: Long = 100L) { price = ((price.toLongOrNull() ?: 0L) + step).toString() }
    fun decrementPrice(step: Long = 100L) { price = maxOf(0L, (price.toLongOrNull() ?: 0L) - step).toString() }

    val orderAmount: Long get() = (quantity.toLongOrNull() ?: 0L) * (price.toLongOrNull() ?: 0L)
    val isValid: Boolean get() = (quantity.toIntOrNull() ?: 0) > 0 && (isMarket || (price.toLongOrNull() ?: 0L) > 0)

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
