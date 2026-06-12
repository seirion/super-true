package com.trueedu.tong.ui.views.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.dto.kis.KisFilledOrder
import com.trueedu.tong.model.dto.kis.KisUnfilledOrder
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.Local
import com.trueedu.tong.repository.remote.kis.KisOrderStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderStatusViewModel @Inject constructor(
    private val brokerAccountRepo: BrokerAccountRepository,
    private val kisStatusRepo: KisOrderStatusRepository,
    private val local: Local,
) : ViewModel() {

    sealed class StatusState {
        object Idle : StatusState()
        object Loading : StatusState()
        data class Success(
            val unfilled: List<KisUnfilledOrder>,
            val filled: List<KisFilledOrder>,
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

            // 선택된 계좌가 KIS가 아니면 미지원
            if (acc.brokerType != com.trueedu.tong.model.BrokerType.KIS) {
                state = StatusState.Error("${acc.brokerType.displayName}은 미체결/체결 조회를 지원하지 않습니다")
                return@launch
            }
            val kisAcc = acc

            val unfilledResult = kisStatusRepo.getUnfilled(kisAcc)
            val filledResult = kisStatusRepo.getFilled(kisAcc)

            if (unfilledResult.isFailure) {
                state = StatusState.Error(unfilledResult.exceptionOrNull()?.message ?: "오류")
                return@launch
            }
            state = StatusState.Success(
                unfilled = unfilledResult.getOrDefault(emptyList()),
                filled = filledResult.getOrDefault(emptyList()),
            )
        }
    }

    fun cancel(order: KisUnfilledOrder) {
        viewModelScope.launch {
            val accountId = local.selectedOrderAccountId
            val kisAcc = brokerAccountRepo.getAll().first().find { it.id == accountId }
                ?: return@launch
            kisStatusRepo.cancel(kisAcc, order.orgNo, order.ordNo, order.code)
                .onSuccess { actionResult = "취소 완료"; load() }
                .onFailure { actionResult = "취소 실패: ${it.message}" }
        }
    }

    fun modify(order: KisUnfilledOrder, newPrice: Long) {
        viewModelScope.launch {
            val accountId = local.selectedOrderAccountId
            val kisAcc = brokerAccountRepo.getAll().first().find { it.id == accountId }
                ?: return@launch
            val qty = order.remainQty.toLongOrNull() ?: 1L
            kisStatusRepo.modify(kisAcc, order.orgNo, order.ordNo, order.code, newPrice, qty)
                .onSuccess { actionResult = "정정 완료"; load() }
                .onFailure { actionResult = "정정 실패: ${it.message}" }
        }
    }

    fun clearActionResult() { actionResult = null }
}
