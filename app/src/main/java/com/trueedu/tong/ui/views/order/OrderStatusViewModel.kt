package com.trueedu.tong.ui.views.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.dto.order.FilledOrderItem
import com.trueedu.tong.model.dto.order.UnfilledOrderItem
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.Local
import com.trueedu.tong.repository.remote.kis.KisOrderStatusRepository
import com.trueedu.tong.repository.remote.kiwoom.KiwoomOrderStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderStatusViewModel @Inject constructor(
    private val brokerAccountRepo: BrokerAccountRepository,
    private val kisStatusRepo: KisOrderStatusRepository,
    private val kiwoomStatusRepo: KiwoomOrderStatusRepository,
    private val local: Local,
) : ViewModel() {

    sealed class StatusState {
        object Idle : StatusState()
        object Loading : StatusState()
        data class Success(
            val unfilled: List<UnfilledOrderItem>,
            val filled: List<FilledOrderItem>,
        ) : StatusState()
        data class Error(val msg: String) : StatusState()
    }

    var state by mutableStateOf<StatusState>(StatusState.Idle); private set
    var actionResult by mutableStateOf<String?>(null); private set

    fun load() {
        viewModelScope.launch {
            state = StatusState.Loading
            val accountId = local.selectedOrderAccountId
            val acc = brokerAccountRepo.getAll().first().find { it.id == accountId }
                ?: brokerAccountRepo.getAll().first().firstOrNull()
                ?: run { state = StatusState.Error("계좌 없음"); return@launch }

            when (acc.brokerType) {
                BrokerType.KIS -> {
                    val unfilledResult = kisStatusRepo.getUnfilled(acc)
                    val filledResult = kisStatusRepo.getFilled(acc)
                    if (unfilledResult.isFailure) {
                        state = StatusState.Error(unfilledResult.exceptionOrNull()?.message ?: "오류")
                        return@launch
                    }
                    state = StatusState.Success(
                        unfilled = unfilledResult.getOrDefault(emptyList()).map { o ->
                            UnfilledOrderItem(
                                ordNo = o.ordNo,
                                code = o.code,
                                name = o.name,
                                isBuy = o.sellBuyCode == "02",
                                ordPrice = o.ordPrice.toLongOrNull() ?: 0L,
                                ordQty = o.ordQty.toLongOrNull() ?: 0L,
                                filledQty = o.filledQty.toLongOrNull() ?: 0L,
                                remainQty = o.remainQty.toLongOrNull() ?: 0L,
                                ordTime = o.ordTime,
                                orgNo = o.orgNo,
                            )
                        },
                        filled = filledResult.getOrDefault(emptyList()).map { o ->
                            FilledOrderItem(
                                code = o.code,
                                name = o.name,
                                isBuy = o.sellBuyCode == "02",
                                filledPrice = o.avgPrice.toLongOrNull() ?: 0L,
                                filledQty = o.filledQty.toLongOrNull() ?: 0L,
                                filledTime = o.ordTime,
                            )
                        },
                    )
                }
                BrokerType.KIWOOM -> {
                    val unfilledResult = kiwoomStatusRepo.getUnfilled(acc)
                    val filledResult = kiwoomStatusRepo.getFilled(acc)
                    if (unfilledResult.isFailure) {
                        state = StatusState.Error(unfilledResult.exceptionOrNull()?.message ?: "오류")
                        return@launch
                    }
                    state = StatusState.Success(
                        unfilled = unfilledResult.getOrDefault(emptyList()).map { o ->
                            UnfilledOrderItem(
                                ordNo = o.ordNo,
                                code = o.code,
                                name = o.name,
                                isBuy = o.tradeType == "2",
                                ordPrice = o.ordPrice.toLongOrNull() ?: 0L,
                                ordQty = o.ordQty.toLongOrNull() ?: 0L,
                                filledQty = o.filledQty.toLongOrNull() ?: 0L,
                                remainQty = o.remainQty.toLongOrNull() ?: 0L,
                                ordTime = o.ordTime,
                                orgNo = o.orgOrdNo,
                                stexTp = o.stexTpTxt.ifBlank { "KRX" },  // 문자열 거래소 코드 사용
                            )
                        },
                        filled = filledResult.getOrDefault(emptyList()).map { o ->
                            FilledOrderItem(
                                code = o.code,
                                name = o.name,
                                isBuy = o.tradeType == "2",
                                filledPrice = o.filledPrice.toLongOrNull() ?: 0L,
                                filledQty = o.filledQty.toLongOrNull() ?: 0L,
                                filledTime = o.filledTime,
                            )
                        },
                    )
                }
                else -> {
                    state = StatusState.Error("${acc.brokerType.displayName}은 미체결/체결 조회를 지원하지 않습니다")
                }
            }
        }
    }

    fun cancel(order: UnfilledOrderItem) {
        viewModelScope.launch {
            val accountId = local.selectedOrderAccountId
            val acc = brokerAccountRepo.getAll().first().find { it.id == accountId } ?: return@launch
            val result = when (acc.brokerType) {
                BrokerType.KIS -> kisStatusRepo.cancel(acc, order.orgNo, order.ordNo, order.code)
                BrokerType.KIWOOM -> kiwoomStatusRepo.cancel(acc, order.ordNo, order.code, order.stexTp)
                else -> return@launch
            }
            result
                .onSuccess { actionResult = "취소 완료"; load() }
                .onFailure { actionResult = "취소 실패: ${it.message}" }
        }
    }

    fun modify(order: UnfilledOrderItem, newPrice: Long) {
        viewModelScope.launch {
            val accountId = local.selectedOrderAccountId
            val acc = brokerAccountRepo.getAll().first().find { it.id == accountId } ?: return@launch
            val qty = order.remainQty
            val result = when (acc.brokerType) {
                BrokerType.KIS -> kisStatusRepo.modify(acc, order.orgNo, order.ordNo, order.code, newPrice, qty)
                BrokerType.KIWOOM -> kiwoomStatusRepo.modify(acc, order.ordNo, order.code, newPrice, qty, order.stexTp)
                else -> return@launch
            }
            result
                .onSuccess { actionResult = "정정 완료"; load() }
                .onFailure { actionResult = "정정 실패: ${it.message}" }
        }
    }

    fun clearActionResult() { actionResult = null }
}
