package com.trueedu.tong.ui.views.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.dto.order.FilledOrderItem
import com.trueedu.tong.model.dto.order.PnlDateRange
import com.trueedu.tong.model.dto.order.RealizedPnlItem
import com.trueedu.tong.model.dto.order.RealizedPnlSummary
import com.trueedu.tong.model.dto.order.UnfilledOrderItem
import com.trueedu.tong.repository.BrokerAccountRepository
import com.trueedu.tong.repository.local.Local
import com.trueedu.tong.repository.remote.kis.KisOrderStatusRepository
import com.trueedu.tong.repository.remote.kiwoom.KiwoomOrderStatusRepository
import com.trueedu.tong.repository.remote.ls.LsOrderStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class OrderStatusViewModel @Inject constructor(
    private val brokerAccountRepo: BrokerAccountRepository,
    private val kisStatusRepo: KisOrderStatusRepository,
    private val kiwoomStatusRepo: KiwoomOrderStatusRepository,
    private val lsStatusRepo: LsOrderStatusRepository,
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

    // 실현손익 관련 상태
    var pnlDateRange by mutableStateOf(PnlDateRange.TODAY); private set
    var customStartDate by mutableStateOf(today()); private set
    var customEndDate by mutableStateOf(today()); private set
    var pnlSummary by mutableStateOf<RealizedPnlSummary?>(null); private set
    var pnlLoading by mutableStateOf(false); private set
    var groupByStock by mutableStateOf(false); private set

    private fun dateRangeFor(range: PnlDateRange): Pair<String, String> {
        val fmt = DateTimeFormatter.ofPattern("yyyyMMdd")
        val now = LocalDate.now()
        return when (range) {
            PnlDateRange.TODAY -> now.format(fmt) to now.format(fmt)
            PnlDateRange.THIS_MONTH -> now.withDayOfMonth(1).format(fmt) to now.format(fmt)
            PnlDateRange.THIS_YEAR -> now.withDayOfYear(1).format(fmt) to now.format(fmt)
            PnlDateRange.CUSTOM -> customStartDate to customEndDate
        }
    }

    fun loadPnl() {
        if (pnlLoading) return  // 이미 로딩 중이면 중복 호출 방지
        viewModelScope.launch {
            pnlLoading = true
            val accountId = local.selectedOrderAccountId
            val acc = brokerAccountRepo.getAll().first().find { it.id == accountId }
                ?: brokerAccountRepo.getAll().first().firstOrNull()
                ?: run { pnlLoading = false; return@launch }

            val (start, end) = dateRangeFor(pnlDateRange)
            val result = when (acc.brokerType) {
                BrokerType.KIS -> kisStatusRepo.getRealizedPnl(acc, start, end)
                BrokerType.KIWOOM -> kiwoomStatusRepo.getRealizedPnl(acc, start, end)
                BrokerType.LS -> lsStatusRepo.getRealizedPnl(acc, start, end)
                else -> null
            }
            pnlSummary = result?.getOrNull()
            pnlLoading = false
        }
    }

    fun onDateRangeChange(range: PnlDateRange) {
        pnlDateRange = range
        if (range != PnlDateRange.CUSTOM) loadPnl()
    }

    fun onCustomDateChange(start: String, end: String) {
        customStartDate = start
        customEndDate = end
        pnlDateRange = PnlDateRange.CUSTOM
        loadPnl()
    }

    // 종목별 집계: code 기준으로 RealizedPnlItem 합산
    val groupedItems: List<RealizedPnlItem>
        get() {
            val items = pnlSummary?.items ?: return emptyList()
            return items.groupBy { it.code }.map { (_, group) ->
                RealizedPnlItem(
                    code = group.first().code,
                    name = group.first().name,
                    sellQty = group.sumOf { it.sellQty },
                    sellPrice = if (group.size == 1) group.first().sellPrice else 0L,
                    fee = group.sumOf { it.fee },
                    tax = group.sumOf { it.tax },
                    pnlBeforeCost = group.sumOf { it.pnlBeforeCost },
                    pnlAfterCost = group.sumOf { it.pnlAfterCost },
                    totalSellAmount = group.sumOf { it.totalSellAmount },
                )
            }.sortedByDescending { it.pnlBeforeCost }
        }

    fun onGroupByStockToggle() {
        groupByStock = !groupByStock
    }

    fun load() {
        // pnl은 아직 데이터 없거나 명시적 날짜 변경 시에만 호출 (탭 재진입마다 재조회 방지)
        if (pnlSummary == null && !pnlLoading) loadPnl()
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
                                isBuy = o.sellBuyCode.trim() == "02",
                                ordPrice = o.ordPrice.trim().toLongOrNull() ?: 0L,
                                ordQty = o.ordQty.trim().toLongOrNull() ?: 0L,
                                filledQty = o.filledQty.trim().toLongOrNull() ?: 0L,
                                remainQty = o.remainQty.trim().toLongOrNull() ?: 0L,
                                ordTime = o.ordTime,
                                orgNo = o.orgNo,
                            )
                        },
                        filled = filledResult.getOrDefault(emptyList()).map { o ->
                            FilledOrderItem(
                                code = o.code,
                                name = o.name,
                                isBuy = o.sellBuyCode.trim() == "02",
                                filledPrice = o.avgPrice.trim().toLongOrNull() ?: 0L,
                                filledQty = o.filledQty.trim().toLongOrNull() ?: 0L,
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
                                isBuy = o.ordTypeName.contains("매수"),  // "+매수"/"+매도" 등
                                ordPrice = o.ordPrice.trim().toLongOrNull() ?: 0L,
                                ordQty = o.ordQty.trim().toLongOrNull() ?: 0L,
                                filledQty = o.filledQty.trim().toLongOrNull() ?: 0L,
                                remainQty = o.remainQty.trim().toLongOrNull() ?: 0L,
                                ordTime = o.ordTime,
                                orgNo = o.orgOrdNo,
                                stexTp = o.stexTpTxt.ifBlank { "KRX" },  // 문자열 거래소 코드 사용
                            )
                        },
                        filled = filledResult.getOrDefault(emptyList()).map { o ->
                            FilledOrderItem(
                                code = o.code,
                                name = o.name,
                                isBuy = o.ordTypeName.contains("매수"),  // "+매수"/"+매도" 등
                                filledPrice = o.filledPrice.trim().toLongOrNull() ?: 0L,
                                filledQty = o.filledQty.trim().toLongOrNull() ?: 0L,
                                filledTime = o.filledTime,
                            )
                        },
                    )
                }
                BrokerType.LS -> {
                    val unfilledResult = lsStatusRepo.getUnfilled(acc)
                    val filledResult = lsStatusRepo.getFilled(acc)
                    if (unfilledResult.isFailure) {
                        state = StatusState.Error(unfilledResult.exceptionOrNull()?.message ?: "오류")
                        return@launch
                    }
                    state = StatusState.Success(
                        unfilled = unfilledResult.getOrDefault(emptyList()),
                        filled = filledResult.getOrDefault(emptyList()),
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
                BrokerType.LS -> lsStatusRepo.cancel(acc, order.ordNo, order.code)
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
                BrokerType.LS -> lsStatusRepo.modify(acc, order.ordNo, order.code, newPrice, qty)
                else -> return@launch
            }
            result
                .onSuccess { actionResult = "정정 완료"; load() }
                .onFailure { actionResult = "정정 실패: ${it.message}" }
        }
    }

    fun clearActionResult() { actionResult = null }

    private fun today(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
}
