package com.trueedu.tong.ui.views.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.utils.NumberFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    vm: OrderViewModel = hiltViewModel(androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity),
) {
    val snackbarHostState = remember { SnackbarHostState() }

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

    val rtQuote = vm.realtimeQuote.value
    val restQuote = vm.quoteData.value?.output1
    val sells = rtQuote?.sells() ?: restQuote?.sells() ?: emptyList()
    val buys = rtQuote?.buys() ?: restQuote?.buys() ?: emptyList()

    val priceStep = priceStep(currentPrice)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vm.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            vm.account?.let {
                                Text(it.brokerType.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                navigationIcon = {},
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val isLoading = vm.orderState is OrderViewModel.OrderState.Loading
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // 좌: 호가창
            OrderBookColumn(
                sells = sells,
                buys = buys,
                currentPrice = currentPrice,
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
    onPriceClick: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(sells.isNotEmpty()) {
        if (sells.isNotEmpty()) scrollState.scrollTo(scrollState.maxValue / 2)
    }
    Column(modifier = modifier.verticalScroll(scrollState)) {
        sells.forEach { (p, q) ->
            QuoteRow(price = p, qty = q, isSell = true, currentPrice = currentPrice, onClick = { onPriceClick(p) })
        }
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        buys.forEach { (p, q) ->
            QuoteRow(price = p, qty = q, isSell = false, currentPrice = currentPrice, onClick = { onPriceClick(p) })
        }
    }
}

@Composable
private fun QuoteRow(
    price: Double,
    qty: Double,
    isSell: Boolean,
    currentPrice: Double,
    onClick: () -> Unit,
) {
    val bgColor = if (isSell) ChartColor.fall.copy(alpha = 0.08f) else ChartColor.rise.copy(alpha = 0.08f)
    val priceColor = if (isSell) ChartColor.fall else ChartColor.rise
    val isCurrent = price == currentPrice
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
        Text(
            text = NumberFormatter.formatCash(price),
            style = MaterialTheme.typography.bodySmall,
            color = priceColor,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        )
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp))
        IconButton(onClick = onDecrease, enabled = enabled, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Remove, null, modifier = Modifier.size(16.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        IconButton(onClick = onIncrease, enabled = enabled, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
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
