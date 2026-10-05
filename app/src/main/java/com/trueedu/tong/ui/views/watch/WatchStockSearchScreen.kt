package com.trueedu.tong.ui.views.watch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueedu.tong.model.StockInfoLocal
import com.trueedu.tong.model.UsStockLocal
import com.trueedu.tong.model.WatchlistItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchStockSearchScreen(
    vm: WatchViewModel,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val isUs = vm.searchMarket == WatchlistItem.MARKET_US
    LaunchedEffect(isUs) {
        if (isUs) vm.loadUsStocksForSearch() else vm.loadStocksForSearch()
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val watchlist by vm.watchlist.collectAsStateWithLifecycle()
    val watchedCodes = watchlist.filter { it.market == vm.searchMarket }.map { it.code }.toSet()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            TopAppBar(
                title = { Text("종목 검색") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
            )
            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = vm::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(if (isUs) "종목명 또는 티커 검색" else "종목명 또는 코드 검색") },
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
            if (isUs) {
                UsSearchContent(vm = vm, watchedCodes = watchedCodes, onDismiss = onDismiss)
            } else {
                val results = vm.searchResults
                if (results.isEmpty()) {
                    SearchMessage(if (vm.searchQuery.isBlank()) "종목명을 입력하세요" else "검색 결과가 없습니다")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(results, key = { it.code }) { stock ->
                            WatchStockSearchRow(
                                stock = stock,
                                added = stock.code in watchedCodes,
                                onClick = {
                                    vm.addToWatchlist(stock.code, stock.nameKr)
                                    onDismiss()
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsSearchContent(
    vm: WatchViewModel,
    watchedCodes: Set<String>,
    onDismiss: () -> Unit,
) {
    when (vm.usSearchState) {
        WatchViewModel.UsSearchState.Idle,
        WatchViewModel.UsSearchState.Loading -> SearchMessage("종목 목록을 불러오는 중이에요")
        WatchViewModel.UsSearchState.NoAccount ->
            SearchMessage("토스증권 계좌를 등록하면\n미국 종목을 검색할 수 있어요")
        WatchViewModel.UsSearchState.Error ->
            SearchMessage("종목 목록을 불러오지 못했어요\n잠시 후 다시 시도해 주세요")
        WatchViewModel.UsSearchState.Ready -> {
            val results = vm.usSearchResults
            if (results.isEmpty()) {
                SearchMessage(if (vm.searchQuery.isBlank()) "종목명 또는 티커를 입력하세요" else "검색 결과가 없습니다")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.symbol }) { stock ->
                        UsSearchRow(
                            stock = stock,
                            added = stock.symbol in watchedCodes,
                            onClick = {
                                vm.addToWatchlist(stock.symbol, stock.nameKr, WatchlistItem.MARKET_US)
                                onDismiss()
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
private fun SearchMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun UsSearchRow(
    stock: UsStockLocal,
    added: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !added, onClick = onClick)
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
                text = stock.symbol,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (added) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "추가됨",
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = stock.market,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun WatchStockSearchRow(
    stock: StockInfoLocal,
    added: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !added, onClick = onClick)
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
        if (added) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "추가됨",
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = if (stock.kospi) "KOSPI" else "KOSDAQ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
