package com.trueedu.tong.ui.views.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trueedu.tong.ui.main.AccountTransfer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    navController: androidx.navigation.NavController? = null,
) {
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
        }
    }
}
