package com.trueedu.tong.ui.views.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.trueedu.tong.model.dto.order.FilledOrderItem
import com.trueedu.tong.model.dto.order.UnfilledOrderItem
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.utils.NumberFormatter

@Composable
fun OrderScreen(
    vm: OrderViewModel = hiltViewModel(LocalContext.current as androidx.activity.ComponentActivity),
    statusVm: OrderStatusViewModel = hiltViewModel(LocalContext.current as androidx.activity.ComponentActivity),
    stockInfoVm: StockInfoViewModel = hiltViewModel(LocalContext.current as androidx.activity.ComponentActivity),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("주문", "미체결", "체결", "종목정보")

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        if (index == 1 || index == 2) statusVm.load()
                        if (index == 3) stockInfoVm.load(vm.code)
                    },
                    text = { Text(title) },
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> OrderEntryTab(vm, statusVm)
                1 -> UnfilledOrderList(statusVm, onModify = { order ->
                    vm.enterModifyMode(order, vm.account?.id ?: -1L)
                    selectedTab = 0
                })
                2 -> FilledOrderList(statusVm)
                else -> StockInfoScreen(vm = stockInfoVm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderEntryTab(
    vm: OrderViewModel,
    statusVm: OrderStatusViewModel,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSearch by remember { mutableStateOf(false) }

    if (showSearch) {
        StockSearchScreen(vm = vm, onDismiss = { showSearch = false })
        return
    }

    LaunchedEffect(vm.orderState) {
        when (val s = vm.orderState) {
            is OrderViewModel.OrderState.Success -> { snackbarHostState.showSnackbar(s.msg); vm.resetState() }
            is OrderViewModel.OrderState.Error -> { snackbarHostState.showSnackbar("오류: ${s.msg}"); vm.resetState() }
            else -> {}
        }
    }

    val rtPrice = vm.realtimePrice
    val quoteOutput2 = vm.quoteData.value?.output2
    val currentPrice = rtPrice?.price ?: quoteOutput2?.price?.toDoubleOrNull() ?: 0.0
    val delta = rtPrice?.delta ?: quoteOutput2?.delta?.toDoubleOrNull() ?: 0.0
    val rate = rtPrice?.rate ?: quoteOutput2?.rate?.toDoubleOrNull() ?: 0.0
    // 전일종가: priceData의 stck_prdy_clpr 우선, 없으면 현재가-전일대비로 추정
    val prevClose = vm.priceDetail?.close?.toDoubleOrNull()
        ?.let { if (it > 0) it else null }
        ?: if (currentPrice > 0 && delta != 0.0) currentPrice - delta
        else 0.0

    val rtQuote = vm.realtimeQuote.value
    val restQuote = vm.quoteData.value?.output1
    val sells = rtQuote?.sells() ?: restQuote?.sells() ?: emptyList()
    val buys = rtQuote?.buys() ?: restQuote?.buys() ?: emptyList()

    val priceStep = priceStep(currentPrice)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
        Column {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (vm.stockName.isNotBlank()) Text(vm.stockName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(vm.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            vm.account?.let {
                                Text(
                                    text = "계좌: ${it.brokerType.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = NumberFormatter.formatCash(currentPrice) + "원",
                                style = MaterialTheme.typography.titleSmall,
                                color = ChartColor.color(delta),
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${NumberFormatter.formatCashWithSign(delta)} (${NumberFormatter.formatRate(rate)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = ChartColor.color(delta),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "종목 검색")
                    }
                },
                actions = {},
            )
            // HLOCW 한 줄: 시 고 저 량
            val pd = vm.priceDetail
            if (pd != null) {
                val closeValue = pd.close.toDoubleOrNull().let {
                    if (it == null || it == 0.0) pd.price else pd.close
                }
                val prevClose = closeValue.toDoubleOrNull() ?: 0.0
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HlocwItem("시", pd.open, prevClose = prevClose)
                    HlocwItem("고", pd.high, prevClose = prevClose)
                    HlocwItem("저", pd.low, prevClose = prevClose)
                    HlocwItem("량", pd.volume, isVolume = true)
                }
            }
            HorizontalDivider()
        }
    },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val isLoading = vm.orderState is OrderViewModel.OrderState.Loading
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (vm.isModifyMode) {
                    // 수정 모드: 정정 + 취소
                    Button(
                        onClick = { vm.submitModify(statusVm) },
                        enabled = vm.isValid && !isLoading,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Text("정정", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(
                        onClick = { vm.exitModifyMode() },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text("취소", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    // 신규 주문 모드: 매수 + 매도
                    Button(
                        onClick = { vm.placeOrder(true) },
                        enabled = vm.isValid && !isLoading,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChartColor.rise),
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Text("매수", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = { vm.placeOrder(false) },
                        enabled = vm.isValid && !isLoading,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChartColor.fall),
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Text("매도", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // 좌: 호가창
            OrderBookColumn(
                sells = sells,
                buys = buys,
                currentPrice = currentPrice,
                prevClose = prevClose,
                onPriceClick = { vm.setPrice(it) },
                modifier = Modifier.width(160.dp).fillMaxHeight(),
            )
            VerticalDivider()
            // 우: 주문 입력
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !vm.isMarket, onClick = { vm.onMarketToggle(false) }, label = { Text("지정가") })
                    FilterChip(selected = vm.isMarket, onClick = { vm.onMarketToggle(true) }, label = { Text("시장가") })
                }
                OrderInputRow(
                    label = "가격",
                    value = if (vm.isMarket) "시장가" else vm.price,
                    onValueChange = vm::onPriceChange,
                    enabled = !vm.isMarket,
                    onIncrease = { vm.incrementPrice(priceStep) },
                    onDecrease = { vm.decrementPrice(priceStep) },
                )
                OrderInputRow(
                    label = "수량",
                    value = vm.quantity,
                    onValueChange = vm::onQuantityChange,
                    enabled = true,
                    onIncrease = vm::incrementQuantity,
                    onDecrease = vm::decrementQuantity,
                )
                if (!vm.isMarket && vm.orderAmount > 0) {
                    Text(
                        text = "주문금액: ${NumberFormatter.formatCash(vm.orderAmount.toDouble())}원",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderBookColumn(
    sells: List<Pair<Double, Double>>,
    buys: List<Pair<Double, Double>>,
    currentPrice: Double,
    prevClose: Double,
    onPriceClick: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(sells.isNotEmpty()) {
        if (sells.isNotEmpty()) scrollState.scrollTo(scrollState.maxValue / 2)
    }
    Column(modifier = modifier.verticalScroll(scrollState)) {
        sells.forEach { (p, q) ->
            QuoteRow(price = p, qty = q, isSell = true, currentPrice = currentPrice, prevClose = prevClose, onClick = { onPriceClick(p) })
        }
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        buys.forEach { (p, q) ->
            QuoteRow(price = p, qty = q, isSell = false, currentPrice = currentPrice, prevClose = prevClose, onClick = { onPriceClick(p) })
        }
    }
}

@Composable
private fun HlocwItem(
    label: String,
    value: String,
    isVolume: Boolean = false,
    prevClose: Double = 0.0,
) {
    val formatted = if (isVolume) {
        val v = value.toLongOrNull() ?: 0L
        when {
            v >= 1_000_000 -> "${v / 1_000_000}M"
            v >= 1_000 -> "${v / 1_000}K"
            else -> v.toString()
        }
    } else {
        NumberFormatter.formatCash(value.toDoubleOrNull() ?: 0.0)
    }
    val valueColor = if (isVolume || prevClose == 0.0) {
        MaterialTheme.colorScheme.onSurface
    } else {
        ChartColor.color((value.toDoubleOrNull() ?: 0.0) - prevClose)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatted, style = MaterialTheme.typography.labelSmall, color = valueColor)
    }
}

@Composable
private fun QuoteRow(
    price: Double,
    qty: Double,
    isSell: Boolean,
    currentPrice: Double,
    prevClose: Double = 0.0,
    onClick: () -> Unit,
) {
    val bgColor = if (isSell) ChartColor.fall.copy(alpha = 0.08f) else ChartColor.rise.copy(alpha = 0.08f)
    val priceColor = if (isSell) ChartColor.fall else ChartColor.rise
    val isCurrent = price == currentPrice
    val rateStr = if (prevClose > 0) {
        val rate = (price - prevClose) / prevClose * 100
        String.format("%.2f%%", rate)
    } else ""
    val rateColor = if (prevClose > 0) ChartColor.color(price - prevClose) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = NumberFormatter.formatCash(price),
                style = MaterialTheme.typography.bodySmall,
                color = priceColor,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            )
            if (rateStr.isNotEmpty()) {
                Text(
                    text = rateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = rateColor,
                    fontSize = 9.sp,
                )
            }
        }
        Text(
            text = NumberFormatter.formatCash(qty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OrderInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 레이블: 오른쪽 상단
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End).padding(end = 4.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onDecrease, enabled = enabled, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Remove, null, modifier = Modifier.size(20.dp))
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            IconButton(onClick = onIncrease, enabled = enabled, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun priceStep(price: Double): Long = when {
    price < 2_000 -> 1L
    price < 5_000 -> 5L
    price < 20_000 -> 10L
    price < 50_000 -> 50L
    price < 200_000 -> 100L
    price < 500_000 -> 500L
    else -> 1_000L
}

@Composable
private fun BuySellBadge(isBuy: Boolean) {
    val color = if (isBuy) ChartColor.rise else ChartColor.fall
    val label = if (isBuy) "매수" else "매도"
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun StatusMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UnfilledOrderList(
    vm: OrderStatusViewModel,
    onModify: (UnfilledOrderItem) -> Unit = {},
) {
    val context = LocalContext.current
    LaunchedEffect(vm.actionResult) {
        vm.actionResult?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            vm.clearActionResult()
        }
    }

    when (val s = vm.state) {
        is OrderStatusViewModel.StatusState.Loading ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        is OrderStatusViewModel.StatusState.Error -> StatusMessage("오류: ${s.msg}")
        is OrderStatusViewModel.StatusState.Success -> {
            if (s.unfilled.isEmpty()) {
                StatusMessage("미체결 주문이 없습니다")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(s.unfilled) { order ->
                        UnfilledOrderRow(
                            order = order,
                            onCancel = { vm.cancel(order) },
                            onModify = { onModify(order) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        else -> StatusMessage("미체결 목록")
    }

}

@Composable
private fun UnfilledOrderRow(
    order: UnfilledOrderItem,
    onCancel: () -> Unit,
    onModify: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 좌: 종목명 + 배지 + 주문시각
        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BuySellBadge(order.isBuy)
                Text(
                    text = order.name.ifBlank { order.code },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = formatOrderTime(order.ordTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 중앙: 주문가격 + 주문수량/미체결수량
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = NumberFormatter.formatCash(order.ordPrice.toDouble()) + "원",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${order.ordQty} / 미체결 ${order.remainQty}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 우: 취소 + 수정
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.height(32.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            ) {
                Text("취소", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = onModify,
                modifier = Modifier.height(32.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            ) {
                Text("수정", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ModifyPriceDialog(
    order: UnfilledOrderItem,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var priceText by remember { mutableStateOf(order.ordPrice.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("주문 정정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${order.name.ifBlank { order.code }} · 미체결 ${order.remainQty}주",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { v -> if (v.all { it.isDigit() }) priceText = v },
                    label = { Text("정정 가격") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { priceText.toLongOrNull()?.let { onConfirm(it) } },
                enabled = (priceText.toLongOrNull() ?: 0L) > 0,
            ) { Text("정정") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
private fun FilledOrderList(vm: OrderStatusViewModel) {
    when (val s = vm.state) {
        is OrderStatusViewModel.StatusState.Loading ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        is OrderStatusViewModel.StatusState.Error -> StatusMessage("오류: ${s.msg}")
        is OrderStatusViewModel.StatusState.Success -> {
            if (s.filled.isEmpty()) {
                StatusMessage("체결 내역이 없습니다")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(s.filled) { order ->
                        FilledOrderRow(order)
                        HorizontalDivider()
                    }
                }
            }
        }
        else -> StatusMessage("체결 내역")
    }
}

@Composable
private fun FilledOrderRow(order: FilledOrderItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BuySellBadge(order.isBuy)
            Text(
                text = order.name.ifBlank { order.code },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = NumberFormatter.formatCash(order.filledPrice.toDouble()) + "원",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "체결 ${order.filledQty}주",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** HHMMSS -> HH:MM:SS */
private fun formatOrderTime(raw: String): String {
    return if (raw.length == 6) "${raw.substring(0, 2)}:${raw.substring(2, 4)}:${raw.substring(4, 6)}" else raw
}
