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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trueedu.tong.ui.main.AccountTransfer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    navController: androidx.navigation.NavController? = null,
    vm: MenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val downloading by vm.downloading.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(title = { Text("더보기") })
                HorizontalDivider()
            }
        },
    ) { innerPadding ->
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
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("계좌 가져오기 / 내보내기") },
                    supportingContent = { Text("계좌 정보를 JSON으로 내보내거나 가져옵니다") },
                    leadingContent = { Icon(Icons.Filled.ImportExport, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.clickable {
                        navController?.navigate(AccountTransfer)
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
