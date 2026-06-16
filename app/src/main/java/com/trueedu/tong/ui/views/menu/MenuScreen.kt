package com.trueedu.tong.ui.views.menu

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueedu.tong.ui.main.AccountTransfer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    backStack: SnapshotStateList<Any>? = null,
    vm: MenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val downloading by vm.downloading.collectAsStateWithLifecycle()
    val view = LocalView.current

    // keepScreenOn 상태가 바뀔 때마다 Window 플래그 갱신
    DisposableEffect(vm.keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (vm.keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {}
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(title = { Text("더보기") })
                HorizontalDivider()
            }
        },
    ) { innerPadding ->
        val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            item {
                ListItem(
                    headlineContent = { Text("버전", style = MaterialTheme.typography.bodyMedium) },
                    trailingContent = {
                        Text(
                            text = com.trueedu.tong.BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = itemColors,
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("화면 항상 켜기") },
                    trailingContent = {
                        Switch(
                            checked = vm.keepScreenOn,
                            onCheckedChange = { vm.onKeepScreenOnChange(it) },
                        )
                    },
                    colors = itemColors,
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("계좌 가져오기 / 내보내기") },
                    supportingContent = { Text("계좌 정보를 JSON으로 내보내거나 가져옵니다") },
                    leadingContent = { Icon(Icons.Filled.ImportExport, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    colors = itemColors,
                    modifier = Modifier.clickable {
                        backStack?.add(AccountTransfer)
                    }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("종목파일 다운로드") },
                    supportingContent = { Text("KIS 마스터 파일을 받아 종목 정보를 갱신합니다") },
                    leadingContent = { Icon(Icons.Filled.Download, null) },
                    trailingContent = {
                        if (downloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    },
                    colors = itemColors,
                    modifier = Modifier.clickable(enabled = !downloading) {
                        vm.downloadStockInfo { success ->
                            val message = if (success) "종목 파일 다운로드 완료" else "다운로드 실패"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
