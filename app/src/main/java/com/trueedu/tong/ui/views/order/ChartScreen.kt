package com.trueedu.tong.ui.views.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trueedu.tong.ui.chart.CandleChartView

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
                // 데이터 출처 표시 (우측 상단)
                Text(
                    text = "데이터: ${s.broker.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End).padding(end = 8.dp, top = 4.dp),
                )
                CandleChartView(
                    candles = s.candles,
                    period = vm.currentPeriod,
                    onPeriodChange = { vm.load(vm.currentCode, period = it, force = true) },
                    onLoadMore = { vm.loadMore() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
    }
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
