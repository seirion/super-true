package com.trueedu.tong.ui.views.watch

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.trueedu.tong.data.realtime.InitialPrice
import com.trueedu.tong.data.realtime.MarketIndex
import com.trueedu.tong.model.WatchlistItem
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.model.ws.TossRealTimeTrade
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.ui.views.home.BottomNavItem
import com.trueedu.tong.ui.views.order.OrderViewModel
import com.trueedu.tong.utils.NumberFormatter
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    backStack: SnapshotStateList<Any>? = null,
    vm: WatchViewModel = hiltViewModel(),
    orderVm: OrderViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val krList by vm.krWatchlist.collectAsStateWithLifecycle()
    val usList by vm.usWatchlist.collectAsStateWithLifecycle()
    val realtimePrices by vm.realtimePrices.collectAsStateWithLifecycle()
    val initialPrices by vm.initialPrices.collectAsStateWithLifecycle()
    val indexMap by vm.indexMap.collectAsStateWithLifecycle()
    val tossPrices by vm.tossPrices.collectAsStateWithLifecycle()
    val usLastPrices by vm.usLastPrices.collectAsStateWithLifecycle()
    val usPrevCloses by vm.usPrevCloses.collectAsStateWithLifecycle()
    val hasTossAccount by vm.hasTossAccount.collectAsStateWithLifecycle()

    // 화면이 처음 그려질 때 실시간 구독 보장
    // (탭 클릭 시 activateRealtime()은 watchlist 로드 전일 수 있으므로 이중 호출)
    LaunchedEffect(Unit) {
        vm.activateRealtime()
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val currentMarket = if (pagerState.currentPage == 0) WatchlistItem.MARKET_KR else WatchlistItem.MARKET_US
    val currentList = if (currentMarket == WatchlistItem.MARKET_KR) krList else usList

    // 삭제 확인 팝업용 상태
    var pendingDelete by remember { mutableStateOf<WatchlistItem?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (currentMarket == WatchlistItem.MARKET_KR) "관심 · 한국" else "관심 · 미국")
                },
                actions = {
                    if (currentList.isNotEmpty() || vm.editMode) {
                        IconButton(onClick = { vm.toggleEditMode(currentMarket) }) {
                            Icon(
                                if (vm.editMode) Icons.Filled.Done else Icons.Filled.Edit,
                                contentDescription = "편집",
                            )
                        }
                    }
                    IconButton(onClick = { vm.openSearch(currentMarket) }, enabled = !vm.editMode) {
                        Icon(Icons.Filled.Search, contentDescription = "종목 검색")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !vm.editMode,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                if (page == 0) {
                    WatchlistPage(
                        market = WatchlistItem.MARKET_KR,
                        items = krList,
                        vm = vm,
                        emptyText = "관심종목을 추가해보세요",
                        onLongClick = { pendingDelete = it },
                    ) { item, editMode, dragHandle ->
                        val code = item.code.removePrefix("A")
                        WatchlistRow(
                            item = item,
                            realtimePrice = realtimePrices[code],
                            initialPrice = initialPrices[code],
                            marketIndex = indexMap[code],
                            editMode = editMode,
                            dragHandle = dragHandle,
                            onClick = {
                                orderVm.selectStock(item.code, item.nameKr, orderVm.account?.id ?: -1L)
                                backStack?.let { bs ->
                                    if (bs.lastOrNull() != BottomNavItem.Order) {
                                        bs.clear()
                                        bs.add(BottomNavItem.Order)
                                    }
                                }
                            },
                            onLongClick = { pendingDelete = item },
                        )
                    }
                } else {
                    WatchlistPage(
                        market = WatchlistItem.MARKET_US,
                        items = usList,
                        vm = vm,
                        emptyText = if (hasTossAccount) "미국 관심종목을 추가해보세요"
                            else "토스증권 계좌를 등록하면\n미국 종목을 추가할 수 있어요",
                        onLongClick = { pendingDelete = it },
                    ) { item, editMode, dragHandle ->
                        UsWatchlistRow(
                            item = item,
                            tossPrice = tossPrices[item.code],
                            lastPrice = usLastPrices[item.code],
                            prevClose = usPrevCloses[item.code],
                            editMode = editMode,
                            dragHandle = dragHandle,
                            // 미국 주식은 아직 거래를 지원하지 않아 탭 동작이 없다
                            onLongClick = { pendingDelete = item },
                        )
                    }
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("관심종목 삭제") },
            text = { Text("'${target.nameKr}'을(를) 관심종목에서 삭제하시겠어요?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.removeFromWatchlist(target.code, target.market)
                    pendingDelete = null
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("취소")
                }
            },
        )
    }

    if (vm.showSearch) {
        WatchStockSearchScreen(vm = vm, onDismiss = vm::closeSearch)
    }
}

/** 한 시장의 관심종목 목록. 편집(순서 변경) 중이면 편집 대상 시장의 임시 순서를 노출한다. */
@Composable
private fun WatchlistPage(
    market: String,
    items: List<WatchlistItem>,
    vm: WatchViewModel,
    emptyText: String,
    onLongClick: (WatchlistItem) -> Unit,
    row: @Composable (item: WatchlistItem, editMode: Boolean, dragHandle: (@Composable () -> Unit)?) -> Unit,
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        vm.reorderWatchlist(from.index, to.index)
    }
    val editingThis = vm.editMode && vm.editMarket == market
    val displayList = if (editingThis) vm.editList else items

    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
        items(displayList, key = { it.code }) { item ->
            if (editingThis) {
                ReorderableItem(reorderState, key = item.code) { isDragging ->
                    val elevation by animateDpAsState(
                        if (isDragging) 4.dp else 0.dp,
                        label = "drag-elevation",
                    )
                    Surface(shadowElevation = elevation) {
                        row(item, true) {
                            Icon(
                                Icons.Filled.DragHandle,
                                contentDescription = "순서 변경",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.draggableHandle(),
                            )
                        }
                    }
                }
            } else {
                row(item, false, null)
            }
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UsWatchlistRow(
    item: WatchlistItem,
    tossPrice: TossRealTimeTrade?,
    lastPrice: Double?,
    prevClose: Double?,
    editMode: Boolean,
    dragHandle: (@Composable () -> Unit)?,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (editMode) Modifier
                else Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nameKr,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (editMode) {
            dragHandle?.invoke()
            return@Row
        }

        // 실시간 체결이 있으면 그것을, 없으면 REST 현재가를 쓴다. 등락은 전일 종가 대비.
        val price = tossPrice?.price ?: lastPrice
        val delta = if (price != null && prevClose != null) price - prevClose else null
        val rate = if (delta != null && prevClose != null && prevClose > 0) delta / prevClose * 100 else null
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = price?.let { NumberFormatter.formatMoney(it, "USD") } ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (delta != null && rate != null) {
                    "${NumberFormatter.formatMoneyWithSign(delta, "USD")} (${NumberFormatter.formatRate(rate)})"
                } else {
                    "-"
                },
                style = MaterialTheme.typography.bodySmall,
                color = ChartColor.color(delta ?: 0.0),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WatchlistRow(
    item: WatchlistItem,
    realtimePrice: KisRealTimeTrade?,
    initialPrice: InitialPrice?,
    marketIndex: MarketIndex? = null,
    editMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    dragHandle: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // 편집 모드에서는 클릭/롱클릭 비활성화
                if (editMode) Modifier
                else Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 좌측: 종목명 + 코드
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nameKr,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 편집 모드: 우측에 드래그 핸들 표시
        if (editMode) {
            dragHandle?.invoke()
            return@Row
        }

        // 우측: 현재가 + 등락금액(등락률) 두 줄
        // 지수(코스피/코스닥)는 marketIndex 우선 사용
        val currentPrice = marketIndex?.price ?: realtimePrice?.price ?: initialPrice?.price
        val delta = marketIndex?.delta ?: realtimePrice?.delta ?: initialPrice?.delta
        val rate = marketIndex?.rate ?: realtimePrice?.rate ?: initialPrice?.rate
        val isIndex = marketIndex != null

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = when {
                    currentPrice == null -> "-"
                    isIndex -> NumberFormatter.formatIndex(currentPrice)
                    else -> "${NumberFormatter.formatCash(currentPrice)}원"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (delta != null && rate != null) {
                    val deltaText = if (isIndex) NumberFormatter.formatIndexWithSign(delta)
                        else NumberFormatter.formatCashWithSign(delta)
                    "$deltaText (${NumberFormatter.formatRate(rate)})"
                } else {
                    "-"
                },
                style = MaterialTheme.typography.bodySmall,
                color = ChartColor.color(delta ?: 0.0),
            )
        }
    }
}
