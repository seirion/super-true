package com.trueedu.tong.ui.views.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.trueedu.tong.model.dto.order.ScheduleOrderItem
import com.trueedu.tong.ui.main.ScheduleAdd
import com.trueedu.tong.ui.theme.ChartColor
import com.trueedu.tong.utils.NumberFormatter
import com.trueedu.tong.utils.formatYmd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleOrderScreen(
    backStack: SnapshotStateList<Any>,
    vm: ScheduleOrderViewModel = hiltViewModel(
        androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity
    ),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var modifyTarget by remember { mutableStateOf<ScheduleOrderItem?>(null) }
    var cancelTarget by remember { mutableStateOf<ScheduleOrderItem?>(null) }

    LaunchedEffect(Unit) { vm.load() }

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
                    title = {
                        Column {
                            Text("예약주문")
                            vm.account?.let {
                                Text(
                                    text = "${it.brokerType.displayName} · ${it.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { backStack.removeLastOrNull() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                    actions = {
                        val enabled = vm.state is ScheduleOrderViewModel.State.Success
                        IconButton(
                            onClick = { backStack.add(ScheduleAdd()) },
                            enabled = enabled,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "예약 추가")
                        }
                    },
                )
                HorizontalDivider()
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = vm.state) {
                is ScheduleOrderViewModel.State.Loading ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                is ScheduleOrderViewModel.State.NoAccount ->
                    CenterMessage("등록된 계좌가 없습니다", "홈 화면에서 계좌를 추가해주세요")

                is ScheduleOrderViewModel.State.Unsupported ->
                    CenterMessage(
                        "${s.brokerName}은 예약주문을 지원하지 않습니다",
                        "한국투자증권 계좌를 선택해주세요",
                    )

                is ScheduleOrderViewModel.State.Error ->
                    CenterMessage("예약주문 조회 실패", s.msg)

                is ScheduleOrderViewModel.State.Success -> {
                    if (s.items.isEmpty()) {
                        CenterMessage("예약된 주문이 없습니다", "우측 상단 + 버튼으로 예약을 추가하세요")
                    } else {
                        Column {
                            Text(
                                text = "예약 주문 ${s.items.size}건",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            HorizontalDivider()
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(s.items, key = { it.seq }) { item ->
                                    ScheduleOrderRow(
                                        item = item,
                                        onClick = {
                                            if (item.disabled) vm.notifyDisabled()
                                            else modifyTarget = item
                                        },
                                        onCancel = { cancelTarget = item },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    modifyTarget?.let { target ->
        ModifyScheduleDialog(
            item = target,
            onDismiss = { modifyTarget = null },
            onConfirm = { price, quantity ->
                vm.modify(target, price, quantity)
                modifyTarget = null
            },
        )
    }

    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("예약 취소") },
            text = {
                Text(
                    "${target.name.ifBlank { target.code }} " +
                        "${if (target.isBuy) "매수" else "매도"} " +
                        "${NumberFormatter.formatCash(target.price.toDouble())}원 " +
                        "${target.quantity}주 예약을 취소할까요?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancel(target)
                    cancelTarget = null
                }) { Text("취소하기") }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) { Text("닫기") }
            },
        )
    }
}

@Composable
private fun ScheduleOrderRow(
    item: ScheduleOrderItem,
    onClick: () -> Unit,
    onCancel: () -> Unit,
) {
    val contentColor =
        if (item.disabled) MaterialTheme.colorScheme.outline
        else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BuySellBadge(item.isBuy, item.disabled)
            Text(
                text = item.name.ifBlank { item.code },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${NumberFormatter.formatCash(item.price.toDouble())}원",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                textAlign = TextAlign.End,
            )
            Text(
                text = "${item.quantity}주",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
            IconButton(onClick = onCancel, enabled = !item.disabled) {
                Icon(
                    imageVector = Icons.Outlined.RemoveCircle,
                    contentDescription = "예약 취소",
                    tint = if (item.disabled) MaterialTheme.colorScheme.surfaceDim
                    else MaterialTheme.colorScheme.error,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "종료 ${formatYmd(item.endDate)} · 처리 ${item.processResult.ifBlank { "-" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (item.rejectReason.isNotBlank()) {
                Text(
                    text = item.rejectReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun BuySellBadge(isBuy: Boolean, disabled: Boolean) {
    val color = when {
        disabled -> MaterialTheme.colorScheme.outline
        isBuy -> ChartColor.rise
        else -> ChartColor.fall
    }
    Text(
        text = if (isBuy) "매수" else "매도",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun ModifyScheduleDialog(
    item: ScheduleOrderItem,
    onDismiss: () -> Unit,
    onConfirm: (price: Long, quantity: Long) -> Unit,
) {
    var priceText by remember { mutableStateOf(item.price.toString()) }
    var quantityText by remember { mutableStateOf(item.quantity.toString()) }
    val valid = (priceText.toLongOrNull() ?: 0L) > 0 && (quantityText.toLongOrNull() ?: 0L) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("예약 정정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${item.name.ifBlank { item.code }} · ${if (item.isBuy) "매수" else "매도"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { v -> if (v.all { it.isDigit() }) priceText = v },
                    label = { Text("가격") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { v -> if (v.all { it.isDigit() }) quantityText = v },
                    label = { Text("수량") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(priceText.toLong(), quantityText.toLong())
                },
                enabled = valid,
            ) { Text("정정") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
    )
}

@Composable
private fun CenterMessage(title: String, subtitle: String? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
