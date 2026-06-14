package com.trueedu.tong.ui.views.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.trueedu.tong.model.BrokerType
import com.trueedu.tong.model.StockInfo
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.utils.NumberFormatter

@Composable
fun StockInfoScreen(
    vm: StockInfoViewModel,
) {
    when (val s = vm.state) {
        is StockInfoViewModel.State.Idle ->
            CenterMessage("종목을 선택해 주세요")
        is StockInfoViewModel.State.Loading ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        is StockInfoViewModel.State.NoAccount ->
            CenterMessage(
                title = "계좌 정보를 입력해 주세요",
                subtitle = "종목정보를 조회하려면 증권사 계좌(앱키)를 먼저 등록해야 합니다.",
            )
        is StockInfoViewModel.State.Error ->
            CenterMessage(
                title = "종목정보를 불러오지 못했습니다",
                subtitle = s.msg,
            )
        is StockInfoViewModel.State.Success ->
            StockInfoContent(info = s.info, broker = s.broker)
    }
}

@Composable
private fun StockInfoContent(info: StockInfo, broker: BrokerType) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        // 헤더: 종목명 + 현재가
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (info.name.isNotBlank()) {
                    Text(
                        text = info.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = info.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = NumberFormatter.formatCash(info.currentPrice) + "원",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChartColor.color(info.delta),
                    )
                    Text(
                        text = "${NumberFormatter.formatCashWithSign(info.delta)} (${NumberFormatter.formatRate(info.rate)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChartColor.color(info.delta),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }

        // 1. 시세
        section("시세") {
            cash("시가", info.open)
            cash("고가", info.high)
            cash("저가", info.low)
            if (info.volume > 0) row("거래량", NumberFormatter.formatCash(info.volume.toDouble()))
        }

        // 2. 투자지표
        section("투자지표") {
            info.marketCap?.let { row("시가총액", "${NumberFormatter.formatCash(it)}억") }
            info.per?.let { row("PER", NumberFormatter.formatCash(it)) }
            info.pbr?.let { row("PBR", NumberFormatter.formatCash(it)) }
            info.eps?.let { row("EPS", NumberFormatter.formatCash(it)) }
            info.bps?.let { row("BPS", NumberFormatter.formatCash(it)) }
            info.roe?.let { row("ROE", NumberFormatter.formatCash(it)) }
            info.ev?.let { row("EV", NumberFormatter.formatCash(it)) }
        }

        // 3. 재무정보
        section("재무정보") {
            info.salesAmount?.let { row("매출액", "${NumberFormatter.formatCash(it)}억") }
            info.operatingProfit?.let { row("영업이익", "${NumberFormatter.formatCash(it)}억") }
            info.netProfit?.let { row("당기순이익", "${NumberFormatter.formatCash(it)}억") }
        }

        // 4. 종목기본정보
        section("종목기본정보") {
            info.parValue?.let { row("액면가", NumberFormatter.formatCash(it) + "원") }
            info.capital?.let { row("자본금", "${NumberFormatter.formatCash(it)}억") }
            info.listedShares?.let { row("상장주식수", NumberFormatter.formatCash(it.toDouble())) }
            info.settlementMonth?.let { row("결산월", it) }
        }

        // 5. 기타
        section("기타") {
            info.week52High?.let { cash("52주 최고", it) }
            info.week52Low?.let { cash("52주 최저", it) }
            info.upperLimit?.let { cash("상한가", it) }
            info.lowerLimit?.let { cash("하한가", it) }
            info.foreignExhaustionRate?.let { row("외인소진률", NumberFormatter.formatCash(it) + "%") }
            info.creditRate?.let { row("신용비율", NumberFormatter.formatCash(it) + "%") }
        }

        // 데이터 출처
        item {
            Text(
                text = "출처: ${broker.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }
}

/** 섹션 빌더: 내부 아이템이 하나라도 있으면 헤더 + 구분선 + 아이템들을 출력 */
private fun LazyListScope.section(title: String, content: SectionScope.() -> Unit) {
    val scope = SectionScope().apply(content)
    if (scope.items.isEmpty()) return
    item {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            HorizontalDivider()
            scope.items.forEach { (label, value) ->
                InfoRow(label, value)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
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

// --- 섹션 DSL 보조 ---

private class SectionScope {
    val items = mutableListOf<Pair<String, String>>()
    fun row(label: String, value: String) { items.add(label to value) }
    fun cash(label: String, value: Double) { items.add(label to (NumberFormatter.formatCash(value) + "원")) }
}
