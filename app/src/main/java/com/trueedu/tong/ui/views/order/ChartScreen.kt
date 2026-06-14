package com.trueedu.tong.ui.views.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        is CandleViewModel.State.Success ->
            Column(modifier = Modifier.fillMaxSize()) {
                // 실시간 가격 헤더
                RealtimePriceHeader(
                    trade = vm.realtimePrice,
                    fallbackClose = s.candles.lastOrNull()?.close ?: 0.0,
                )
                HorizontalDivider()
                CandleChartView(
                    candles = s.candles,
                    period = vm.currentPeriod,
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

@Composable
private fun RealtimePriceHeader(
    trade: KisRealTimeTrade?,
    fallbackClose: Double,
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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
