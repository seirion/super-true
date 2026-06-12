package com.trueedu.tong.ui.views.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trueedu.tong.model.BrokerType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onBack: () -> Unit,
    vm: AddAccountViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()

    // 저장 완료 시 화면 닫기
    LaunchedEffect(vm.saved) {
        if (vm.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("계좌 추가") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = vm.name,
                onValueChange = vm::onNameChange,
                label = { Text("계좌 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            BrokerTypeDropdown(
                selected = vm.brokerType,
                onSelected = vm::onBrokerTypeChange,
            )

            PasteTextField(
                value = vm.accountNum,
                onValueChange = vm::onAccountNumChange,
                label = "계좌번호",
            )

            PasteTextField(
                value = vm.appKey,
                onValueChange = vm::onAppKeyChange,
                label = "App Key",
            )

            SecretTextField(
                value = vm.appSecret,
                onValueChange = vm::onAppSecretChange,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        vm.saveAccount()
                    }
                },
                enabled = vm.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("저장")
            }
        }
    }
}

@Composable
private fun BrokerTypeDropdown(
    selected: BrokerType,
    onSelected: (BrokerType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("증권사") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        // OutlinedTextField가 클릭을 소비하므로 투명한 오버레이로 메뉴를 연다.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            BrokerType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PasteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    val clipboard = LocalClipboardManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = {
                clipboard.getText()?.text?.let { onValueChange(it) }
            }) {
                Icon(
                    imageVector = Icons.Filled.ContentPaste,
                    contentDescription = "붙여넣기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("App Secret") },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None
            else PasswordVisualTransformation(),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    clipboard.getText()?.text?.let { onValueChange(it) }
                }) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = "붙여넣기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff,
                        contentDescription = if (visible) "숨기기" else "표시",
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
