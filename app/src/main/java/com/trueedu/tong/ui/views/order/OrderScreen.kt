package com.trueedu.tong.ui.views.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onBack: () -> Unit,
    vm: OrderViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm.orderState) {
        when (val s = vm.orderState) {
            is OrderViewModel.OrderState.Success -> {
                snackbarHostState.showSnackbar(s.message)
                vm.resetState()
            }
            is OrderViewModel.OrderState.Error -> {
                snackbarHostState.showSnackbar("오류: ${s.message}")
                vm.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(vm.code, style = MaterialTheme.typography.titleMedium)
                        vm.account?.let {
                            Text(it.brokerType.displayName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // 지정가 / 시장가 토글
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !vm.isMarket,
                    onClick = { vm.onMarketToggle(false) },
                    label = { Text("지정가") },
                )
                FilterChip(
                    selected = vm.isMarket,
                    onClick = { vm.onMarketToggle(true) },
                    label = { Text("시장가") },
                )
            }

            // 수량
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("수량", modifier = Modifier.width(48.dp))
                IconButton(onClick = vm::decrementQuantity) { Icon(Icons.Filled.Remove, null) }
                OutlinedTextField(
                    value = vm.quantity,
                    onValueChange = vm::onQuantityChange,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                IconButton(onClick = vm::incrementQuantity) { Icon(Icons.Filled.Add, null) }
            }

            // 가격
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("가격", modifier = Modifier.width(48.dp))
                IconButton(onClick = { vm.decrementPrice() }, enabled = !vm.isMarket) { Icon(Icons.Filled.Remove, null) }
                OutlinedTextField(
                    value = if (vm.isMarket) "시장가" else vm.price,
                    onValueChange = vm::onPriceChange,
                    modifier = Modifier.weight(1f),
                    enabled = !vm.isMarket,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                IconButton(onClick = { vm.incrementPrice() }, enabled = !vm.isMarket) { Icon(Icons.Filled.Add, null) }
            }

            // 주문금액
            if (!vm.isMarket) {
                Text(
                    text = "주문금액: ${NumberFormatter.formatCash(vm.orderAmount.toDouble())}원",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.weight(1f))

            // 매수 / 매도 버튼
            val isLoading = vm.orderState is OrderViewModel.OrderState.Loading
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { vm.placeOrder(true) },
                    enabled = vm.isValid && !isLoading,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChartColor.rise),
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("매수", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { vm.placeOrder(false) },
                    enabled = vm.isValid && !isLoading,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChartColor.fall),
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("매도", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
