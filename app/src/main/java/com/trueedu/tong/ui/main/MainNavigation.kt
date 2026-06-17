package com.trueedu.tong.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.trueedu.tong.ui.views.account.AddAccountScreen
import com.trueedu.tong.ui.views.home.BottomNavItem
import com.trueedu.tong.ui.views.home.HomeScreen
import com.trueedu.tong.ui.views.menu.AccountTransferScreen
import com.trueedu.tong.ui.views.menu.MenuScreen
import com.trueedu.tong.ui.views.order.OrderScreen
import com.trueedu.tong.ui.views.watch.WatchScreen
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AddAccount(val accountId: Long = -1L) : Parcelable  // -1L = 신규 추가

@Parcelize
@Serializable
data object AccountTransfer : Parcelable

@Composable
fun MainNavigation(
    backStack: SnapshotStateList<Any>,
    innerPadding: PaddingValues,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        entryProvider = entryProvider {
            entry<BottomNavItem.Home> {
                HomeScreen(backStack = backStack)
            }
            entry<BottomNavItem.Watch> {
                WatchScreen(backStack = backStack)
            }
            entry<BottomNavItem.Order> {
                OrderScreen()
            }
            entry<BottomNavItem.Menu> {
                MenuScreen(backStack = backStack)
            }
            entry<AddAccount> {
                AddAccountScreen(onBack = { backStack.removeLastOrNull() })
            }
            entry<AccountTransfer> {
                AccountTransferScreen(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
