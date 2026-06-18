package com.trueedu.tong.ui.views.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.trueedu.tong.model.dto.order.PnlDateRange
import com.trueedu.tong.model.dto.order.RealizedPnlItem
import com.trueedu.tong.model.dto.order.RealizedPnlSummary
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.ui.views.order.OrderStatusViewModel
import com.trueedu.tong.utils.NumberFormatter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealizedPnlScreen(
    backStack: SnapshotStateList<Any>,
    vm: OrderStatusViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        vm.loadPnl()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("실현수익") },
                    navigationIcon = {
                        IconButton(onClick = { backStack.removeLastOrNull() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                )
                HorizontalDivider()
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 실현손익 요약 카드
            RealizedPnlCard(summary = vm.pnlSummary, loading = vm.pnlLoading)
            // 날짜 구간 선택
            PnlDateRangeRow(vm)
            HorizontalDivider()
            // 목록 헤더 + 종목별 합산 토글
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (vm.groupByStock) "종목별 합산" else "매도 건별",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "종목별 합산",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = vm.groupByStock,
                        onCheckedChange = { vm.onGroupByStockToggle() },
                    )
                }
            }
            HorizontalDivider()
            // 목록 본문
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val displayItems =
                    if (vm.groupByStock) vm.groupedItems else vm.pnlSummary?.items ?: emptyList()
                if (vm.pnlLoading && vm.pnlSummary == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (displayItems.isEmpty()) {
                    StatusMessage("매도 내역이 없습니다")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(displayItems) { item ->
                            RealizedPnlRow(item, showSellAmount = true)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RealizedPnlCard(summary: RealizedPnlSummary?, loading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (loading && summary == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Text(
                    "실현손익 조회 중...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }
        val before = summary?.totalPnlBeforeCost ?: 0L
        val after = summary?.totalPnlAfterCost ?: 0L
        val fee = summary?.totalFee ?: 0L
        val tax = summary?.totalTax ?: 0L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "실현손익 (비용후)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = NumberFormatter.formatCashWithSign(after.toDouble()) + "원",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ChartColor.color(after.toDouble()),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "실현손익 (비용전)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = NumberFormatter.formatCashWithSign(before.toDouble()) + "원",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ChartColor.color(before.toDouble()),
                )
            }
        }
        Text(
            text = "수수료 ${NumberFormatter.formatCash(fee.toDouble())}원 · 세금 ${NumberFormatter.formatCash(tax.toDouble())}원",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PnlDateRangeRow(vm: OrderStatusViewModel) {
    var showPicker by remember { mutableStateOf(false) }
    val ranges = listOf(
        PnlDateRange.TODAY to "오늘",
        PnlDateRange.THIS_MONTH to "이번달",
        PnlDateRange.THIS_YEAR to "올해",
        PnlDateRange.CUSTOM to "구간선택",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ranges.forEach { (range, label) ->
            val text =
                if (range == PnlDateRange.CUSTOM && vm.pnlDateRange == PnlDateRange.CUSTOM) {
                    "${formatPnlDate(vm.customStartDate)}~${formatPnlDate(vm.customEndDate)}"
                } else label
            FilterChip(
                selected = vm.pnlDateRange == range,
                onClick = {
                    if (range == PnlDateRange.CUSTOM) showPicker = true
                    else vm.onDateRangeChange(range)
                },
                label = { Text(text) },
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = pickerState.selectedStartDateMillis
                        val end = pickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            vm.onCustomDateChange(millisToYmd(start), millisToYmd(end))
                        }
                        showPicker = false
                    },
                    enabled = pickerState.selectedStartDateMillis != null &&
                        pickerState.selectedEndDateMillis != null,
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("취소") }
            },
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun RealizedPnlRow(item: RealizedPnlItem, showSellAmount: Boolean = false) {
    val sellAmount = item.sellQty * item.sellPrice
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.name.ifBlank { item.code },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (showSellAmount && sellAmount > 0) {
                Text(
                    text = "거래액 ${NumberFormatter.formatCash(sellAmount.toDouble())}원 · ${item.sellQty}주",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "수수료 ${NumberFormatter.formatCash(item.fee.toDouble())} · 세금 ${NumberFormatter.formatCash(item.tax.toDouble())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = NumberFormatter.formatCashWithSign(item.pnlAfterCost.toDouble()) + "원",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ChartColor.color(item.pnlAfterCost.toDouble()),
            )
            Text(
                text = "세전 ${NumberFormatter.formatCashWithSign(item.pnlBeforeCost.toDouble())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun formatPnlDate(raw: String): String =
    if (raw.length == 8) "${raw.substring(0, 4)}.${raw.substring(4, 6)}.${raw.substring(6, 8)}" else raw

internal fun millisToYmd(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
