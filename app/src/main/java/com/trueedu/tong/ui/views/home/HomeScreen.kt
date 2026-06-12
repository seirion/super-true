package com.trueedu.tong.ui.views.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueedu.tong.data.realtime.InitialPrice
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.account.HoldingStock
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.utils.NumberFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: androidx.navigation.NavController? = null,
    vm: HomeViewModel = hiltViewModel(),
) {
    val selectedAccount by vm.selectedAccount.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val realtimePrices by vm.realtimePrices.collectAsStateWithLifecycle()
    val initialPrices by vm.initialPrices.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("홈") },
                actions = {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        FilterChip(
                            selected = vm.marketPriceMode,
                            onClick = { if (!vm.marketPriceMode) vm.toggleMode() },
                            label = { Text("시세") },
                        )
                        Spacer(Modifier.width(4.dp))
                        FilterChip(
                            selected = !vm.marketPriceMode,
                            onClick = { if (vm.marketPriceMode) vm.toggleMode() },
                            label = { Text("평가") },
                        )
                    }
                    IconButton(onClick = vm::toggleSummary) {
                        Icon(
                            imageVector = if (vm.summaryExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (vm.summaryExpanded) "요약 접기" else "요약 펼치기",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                selectedAccount == null -> {
                    item { EmptyHome("계좌를 선택하세요") }
                }

                uiState is HomeViewModel.UiState.Loading -> {
                    item { LoadingHome() }
                }

                uiState is HomeViewModel.UiState.Error -> {
                    val message = (uiState as HomeViewModel.UiState.Error).message
                    item { ErrorHome(message = message, onRetry = vm::refresh) }
                }

                uiState is HomeViewModel.UiState.Success -> {
                    val summary = (uiState as HomeViewModel.UiState.Success).summary
                    item {
                        AccountInfoSection(
                            summary = summary,
                            marketPriceMode = vm.marketPriceMode,
                            realtimePrices = realtimePrices,
                            initialPrices = initialPrices,
                            expanded = vm.summaryExpanded,
                            onRefresh = vm::refresh,
                        )
                        HorizontalDivider()
                    }
                    items(summary.holdings, key = { it.code }) { holding ->
                        HoldingStockItem(
                            holding = holding,
                            marketPriceMode = vm.marketPriceMode,
                            realtimePrice = realtimePrices[holding.code.removePrefix("A")],
                            initialPrice = initialPrices[holding.code.removePrefix("A")],
                            onClick = {
                                selectedAccount?.let { acc ->
                                    vm.selectForOrder(holding.code, acc.id)
                                    navController?.navigate(com.trueedu.tong.ui.views.home.BottomNavItem.Order) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHome(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingHome() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorHome(
    message: String,
    onRetry: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRetry) {
                Text("재시도")
            }
            OutlinedButton(onClick = {
                clipboard.setText(AnnotatedString(message))
            }) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "복사",
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text("복사")
            }
        }
    }
}

@Composable
private fun AccountInfoSection(
    summary: AccountSummary,
    marketPriceMode: Boolean,
    realtimePrices: Map<String, KisRealTimeTrade>,
    initialPrices: Map<String, InitialPrice>,
    expanded: Boolean,
    onRefresh: () -> Unit,
) {
    // 실시간 데이터가 없으면 초기 현재가(REST)로 fallback
    val hasPrices = realtimePrices.isNotEmpty() || initialPrices.isNotEmpty()
    // 시세 모드: 실시간 총자산/일간 수익 계산
    val (displayAsset, displayProfit, displayProfitRate) = if (marketPriceMode && hasPrices) {
        // 총 평가금액 = Σ(현재가 × 수량)
        val realtimeStockTotal = summary.holdings.sumOf { holding ->
            val code = holding.code.removePrefix("A")
            val price = realtimePrices[code]?.price
                ?: initialPrices[code]?.price
                ?: holding.currentPrice ?: holding.avgPrice
            price * holding.quantity
        }
        val deposit2 = summary.depositD2 ?: summary.deposit
        val totalAsset = realtimeStockTotal + deposit2

        // 일간 수익 = Σ(전일대비등락 × 수량)
        val dailyProfit = summary.holdings.sumOf { holding ->
            val code = holding.code.removePrefix("A")
            val delta = realtimePrices[code]?.delta ?: initialPrices[code]?.delta ?: 0.0
            delta * holding.quantity
        }
        // 일간 수익률 = 일간 수익 / 전일 총자산
        val prevAsset = totalAsset - dailyProfit
        val dailyRate = if (prevAsset > 0) dailyProfit / prevAsset * 100 else 0.0
        Triple(totalAsset, dailyProfit, dailyRate)
    } else {
        Triple(summary.totalAsset, summary.totalProfitAmount, summary.totalProfitRate)
    }

    val profitLabel = if (marketPriceMode && hasPrices) "일간 " else ""
    val profitText = "$profitLabel${NumberFormatter.formatCashWithSign(displayProfit)}원 " +
        "(${NumberFormatter.formatRate(displayProfitRate)})"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (expanded) 16.dp else 8.dp),
    ) {
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${NumberFormatter.formatCash(displayAsset)}원",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "새로고침",
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = profitText,
                style = MaterialTheme.typography.bodyMedium,
                color = ChartColor.color(displayProfit),
            )
        } else {
            // 접힘: 수익/수익률만 한 줄
            Text(
                text = profitText,
                style = MaterialTheme.typography.bodyMedium,
                color = ChartColor.color(displayProfit),
            )
        }

        if (expanded) {

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                DepositColumn(
                    label = "예수금",
                    value = summary.deposit,
                    modifier = Modifier.weight(1f),
                )
                DepositColumn(
                    label = "D+1 예수금",
                    value = summary.depositD1,
                    modifier = Modifier.weight(1f),
                )
                DepositColumn(
                    label = "D+2 예수금",
                    value = summary.depositD2,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DepositColumn(
    label: String,
    value: Double?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (value != null) NumberFormatter.formatCash(value) else "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HoldingStockItem(
    holding: HoldingStock,
    marketPriceMode: Boolean,
    realtimePrice: KisRealTimeTrade?,
    initialPrice: InitialPrice?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = holding.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${NumberFormatter.formatCash(holding.avgPrice)}원 • ${holding.quantity}주",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (marketPriceMode) {
                // 시세 모드: 현재가 / 일간등락 / 등락률
                // 실시간 > 초기 현재가(REST) > 잔고 현재가 순으로 fallback
                val currentPrice = realtimePrice?.price ?: initialPrice?.price ?: holding.currentPrice
                val delta = realtimePrice?.delta ?: initialPrice?.delta
                val rate = realtimePrice?.rate ?: initialPrice?.rate
                Text(
                    text = if (currentPrice != null) {
                        "${NumberFormatter.formatCash(currentPrice)}원"
                    } else {
                        "-"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (delta != null && rate != null) {
                        "${NumberFormatter.formatCashWithSign(delta)} " +
                            "(${NumberFormatter.formatRate(rate)})"
                    } else {
                        "-"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ChartColor.color(delta ?: 0.0),
                )
            } else {
                // 평가 모드: 평가금액 / 손익금액 (손익률)
                Text(
                    text = "${NumberFormatter.formatCash(holding.evaluationAmount)}원",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${NumberFormatter.formatCashWithSign(holding.profitAmount)} " +
                        "(${NumberFormatter.formatRate(holding.profitRate)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChartColor.color(holding.profitAmount),
                )
            }
        }
    }
}
