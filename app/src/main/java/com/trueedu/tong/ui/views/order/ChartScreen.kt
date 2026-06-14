package com.trueedu.tong.ui.views.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trueedu.tong.model.CandlePeriod
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.ui.chart.CandleChartView
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.utils.NumberFormatter

@Composable
fun ChartScreen(vm: CandleViewModel) {
    when (val s = vm.state) {
        is CandleViewModel.State.Idle ->
            CenterMessage("종목을 선택해 주세요")
        is CandleViewModel.State.Loading ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        is CandleViewModel.State.NoAccount ->
            CenterMessage(
                title = "계좌 정보를 입력해 주세요",
                subtitle = "차트를 표시하려면 증권사 계좌(앱키)를 먼저 등록해야 합니다.",
            )
        is CandleViewModel.State.Error ->
            CenterMessage(
                title = "차트 데이터를 불러오지 못했습니다",
                subtitle = s.msg,
            )
        is CandleViewModel.State.Success -> {
            var showIntervalDialog by remember { mutableStateOf(false) }

            if (showIntervalDialog) {
                MinuteIntervalDialog(
                    current = vm.minuteInterval,
                    onSelect = { vm.changeMinuteInterval(it); showIntervalDialog = false },
                    onDismiss = { showIntervalDialog = false },
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // 실시간 가격 헤더
                RealtimePriceHeader(
                    trade = vm.realtimePrice,
                    fallbackClose = s.candles.lastOrNull()?.close ?: 0.0,
                    showSettings = vm.currentPeriod == CandlePeriod.MINUTE,
                    minuteInterval = vm.minuteInterval,
                    onSettingsClick = { showIntervalDialog = true },
                )
                HorizontalDivider()
                CandleChartView(
                    candles = s.candles,
                    period = vm.currentPeriod,
                    minuteInterval = vm.minuteInterval,
                    onPeriodChange = { vm.load(vm.currentCode, period = it, force = true) },
                    onLoadMore = { vm.loadMore() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                // 데이터 출처 표시
                Text(
                    text = "데이터: ${s.broker.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MinuteIntervalDialog(
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(1, 3, 5, 10, 30, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("분봉 단위 선택") },
        text = {
            Column {
                options.forEach { min ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(min) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${min}분봉",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (min == current) FontWeight.Bold else FontWeight.Normal,
                            color = if (min == current) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                        if (min == current) {
                            Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (min != options.last()) HorizontalDivider()
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
private fun RealtimePriceHeader(
    trade: KisRealTimeTrade?,
    fallbackClose: Double,
    showSettings: Boolean = false,
    minuteInterval: Int = 1,
    onSettingsClick: () -> Unit = {},
) {
    val price = trade?.price ?: fallbackClose
    val delta = trade?.delta ?: 0.0
    val rate = trade?.rate ?: 0.0
    val volume = trade?.volume ?: 0.0
    val color = ChartColor.color(delta)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 현재가 (크게)
        Text(
            text = NumberFormatter.formatCash(price) + "원",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // 변동 금액 + 변동률
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = NumberFormatter.formatCashWithSign(delta),
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                )
                Text(
                    text = "(${NumberFormatter.formatRate(rate)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                )
            }
            // 거래량
            if (volume > 0) {
                Text(
                    text = "거래량 ${formatVolume(volume.toLong())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // 분봉 설정 아이콘 (분봉 탭일 때만 표시)
        if (showSettings) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "분봉 설정",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatVolume(vol: Long): String = when {
    vol >= 100_000_000 -> "${vol / 100_000_000}억"
    vol >= 10_000 -> "${vol / 10_000}만"
    else -> NumberFormatter.formatCash(vol.toDouble())
}

@Composable
private fun CenterMessage(title: String, subtitle: String? = null) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
