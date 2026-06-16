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
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.platform.LocalContext
import com.trueedu.tong.data.realtime.InitialPrice
import com.trueedu.tong.data.realtime.MarketIndex
import com.trueedu.tong.model.WatchlistItem
import com.trueedu.tong.model.ws.KisRealTimeTrade
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.ui.views.home.BottomNavItem
import com.trueedu.tong.ui.views.order.OrderViewModel
import com.trueedu.tong.utils.NumberFormatter
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    navController: NavController? = null,
    vm: WatchViewModel = hiltViewModel(),
    orderVm: OrderViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val watchlist by vm.watchlist.collectAsStateWithLifecycle()
    val realtimePrices by vm.realtimePrices.collectAsStateWithLifecycle()
    val initialPrices by vm.initialPrices.collectAsStateWithLifecycle()
    val indexMap by vm.indexMap.collectAsStateWithLifecycle()

    // 화면이 처음 그려질 때 실시간 구독 보장
    // (탭 클릭 시 activateRealtime()은 watchlist 로드 전일 수 있으므로 이중 호출)
    LaunchedEffect(Unit) {
        vm.activateRealtime()
    }

    // 삭제 확인 팝업용 상태
    var pendingDeleteCode by remember { mutableStateOf<String?>(null) }
    var pendingDeleteName by remember { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("관심") },
                actions = {
                    if (watchlist.isNotEmpty()) {
                        IconButton(onClick = vm::toggleEditMode) {
                            Icon(
                                if (vm.editMode) Icons.Filled.Done else Icons.Filled.Edit,
                                contentDescription = "편집",
                            )
                        }
                    }
                    IconButton(onClick = vm::openSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "종목 검색")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (watchlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "관심종목을 추가해보세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val lazyListState = rememberLazyListState()
            val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                vm.reorderWatchlist(from.index, to.index)
            }
            // 편집 모드에서는 로컬 임시 순서(editList)를, 평소에는 watchlist를 노출
            val displayList = if (vm.editMode) vm.editList else watchlist

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(displayList, key = { it.code }) { item ->
                    val code = item.code.removePrefix("A")
                    if (vm.editMode) {
                        ReorderableItem(reorderState, key = item.code) { isDragging ->
                            val elevation by animateDpAsState(
                                if (isDragging) 4.dp else 0.dp,
                                label = "drag-elevation",
                            )
                            Surface(shadowElevation = elevation) {
                                WatchlistRow(
                                    item = item,
                                    realtimePrice = realtimePrices[code],
                                    initialPrice = initialPrices[code],
                                    marketIndex = indexMap[code],
                                    editMode = true,
                                    dragHandle = {
                                        Icon(
                                            Icons.Filled.DragHandle,
                                            contentDescription = "순서 변경",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.draggableHandle(),
                                        )
                                    },
                                )
                            }
                        }
                        HorizontalDivider()
                    } else {
                        WatchlistRow(
                            item = item,
                            realtimePrice = realtimePrices[code],
                            initialPrice = initialPrices[code],
                            marketIndex = indexMap[code],
                            onClick = {
                                orderVm.selectStock(item.code, item.nameKr, orderVm.account?.id ?: -1L)
                                navController?.navigate(BottomNavItem.Order) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onLongClick = {
                                pendingDeleteCode = item.code
                                pendingDeleteName = item.nameKr
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (pendingDeleteCode != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteCode = null },
            title = { Text("관심종목 삭제") },
            text = { Text("'$pendingDeleteName'을(를) 관심종목에서 삭제하시겠어요?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteCode?.let { vm.removeFromWatchlist(it) }
                    pendingDeleteCode = null
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCode = null }) {
                    Text("취소")
                }
            },
        )
    }

    if (vm.showSearch) {
        WatchStockSearchScreen(vm = vm, onDismiss = vm::closeSearch)
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
