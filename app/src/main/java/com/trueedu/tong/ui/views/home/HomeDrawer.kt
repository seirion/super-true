package com.trueedu.tong.ui.views.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueedu.tong.model.BrokerAccount

@Composable
fun HomeDrawer(
    vm: HomeDrawerViewModel = hiltViewModel(),
    onAddAccount: () -> Unit,
    onEditAccount: (Long) -> Unit,
    close: () -> Unit,
) {
    val accounts by vm.accounts.collectAsStateWithLifecycle()

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = 60.dp)
            .navigationBarsPadding()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Header
            item {
                Text(
                    text = "계좌 목록",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
                HorizontalDivider()
            }

            // Account list
            if (accounts.isEmpty()) {
                item {
                    Text(
                        text = "등록된 계좌가 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    AccountItem(
                        account = account,
                        onSelect = {
                            vm.selectAccount(account.id)
                            close()
                        },
                        onEdit = { onEditAccount(account.id) },
                        onDelete = { vm.deleteAccount(account) },
                    )
                }
            }

            // Add button at the bottom
            item {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                TextButton(
                    onClick = {
                        onAddAccount()
                        close()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("계좌 추가")
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: BrokerAccount,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val bgModifier = if (account.isSelected) {
        Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            )
    } else {
        Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .then(bgModifier)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (account.isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${account.brokerType.displayName} · ${account.accountNum}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "수정",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "삭제",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
