package com.trueedu.tong.ui.views.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.trueedu.tong.model.StockInfoLocal
import com.trueedu.tong.ui.main.ScheduleAdd
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.utils.NumberFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleAddScreen(
    route: ScheduleAdd,
    backStack: SnapshotStateList<Any>,
    vm: ScheduleOrderViewModel = hiltViewModel(
        androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity
    ),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // 주문 화면에서 종목을 넘겨받으면 검색을 건너뛰고 입력 상태로 시작한다
    var searchMode by remember { mutableStateOf(route.code.isBlank()) }

    LaunchedEffect(route) {
        vm.startAdd(route.code, route.price, route.quantity)
        vm.loadStocksForSearch()
    }

    LaunchedEffect(vm.actionMessage) {
        vm.actionMessage?.let {
            snackbarHostState.showSnackbar(it, actionLabel = "닫기")
            vm.clearActionMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (searchMode) "종목 검색" else "예약주문 등록") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (searchMode && vm.code.isNotBlank()) searchMode = false
                            else backStack.removeLastOrNull()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                )
                HorizontalDivider()
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!searchMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(true, false).forEach { isBuy ->
                        Button(
                            onClick = {
                                vm.submitAdd(isBuy) { backStack.removeLastOrNull() }
                            },
                            enabled = vm.addValid && !vm.submitting,
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBuy) ChartColor.rise else ChartColor.fall
                            ),
                        ) {
                            if (vm.submitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = if (isBuy) "예약 매수" else "예약 매도",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (searchMode) {
                StockSearchBody(vm) { searchMode = false }
            } else {
                ScheduleInputBody(vm) { searchMode = true }
            }
        }
    }
}

@Composable
private fun ScheduleInputBody(
    vm: ScheduleOrderViewModel,
    onSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSearch),
        ) {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "종목 검색")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vm.stockName.ifBlank { "종목을 선택하세요" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (vm.code.isNotBlank()) {
                    Text(
                        text = vm.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider()

        ScheduleInputRow(
            label = "가격",
            value = vm.priceInput,
            onValueChange = vm::onPriceChange,
            onIncrease = vm::incrementPrice,
            onDecrease = vm::decrementPrice,
        )
        ScheduleInputRow(
            label = "수량",
            value = vm.quantityInput,
            onValueChange = vm::onQuantityChange,
            onIncrease = vm::incrementQuantity,
            onDecrease = vm::decrementQuantity,
        )

        if (vm.orderAmount > 0) {
            Text(
                text = "예약금액: ${NumberFormatter.formatCash(vm.orderAmount.toDouble())}원",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "지정가로 예약되며, 예약은 30일 뒤 영업일까지 매일 장 시작 시 주문됩니다.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ScheduleInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "감소", modifier = Modifier.size(20.dp))
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            IconButton(onClick = onIncrease, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "증가", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun StockSearchBody(
    vm: ScheduleOrderViewModel,
    onSelected: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = vm.searchQuery,
            onValueChange = vm::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("종목명 또는 코드 검색") },
            singleLine = true,
            trailingIcon = {
                if (vm.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { vm.onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "지우기")
                    }
                }
            },
        )
        HorizontalDivider()
        val results = vm.searchResults
        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (vm.searchQuery.isBlank()) "종목명을 입력하세요" else "검색 결과가 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.code }) { stock ->
                    StockRow(stock) {
                        vm.selectStock(stock)
                        onSelected()
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StockRow(stock: StockInfoLocal, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stock.nameKr,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stock.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (stock.kospi) "KOSPI" else "KOSDAQ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
