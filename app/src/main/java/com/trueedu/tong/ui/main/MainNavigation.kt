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
import com.trueedu.tong.ui.views.menu.RealizedPnlScreen
import com.trueedu.tong.ui.views.order.OrderScreen
import com.trueedu.tong.ui.views.schedule.ScheduleAddScreen
import com.trueedu.tong.ui.views.schedule.ScheduleOrderScreen
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

@Parcelize
@Serializable
data object RealizedPnl : Parcelable

@Parcelize
@Serializable
data object ScheduleOrder : Parcelable

/**
 * 예약주문 등록. 주문 화면에서 진입하면 종목/가격/수량이 채워진다.
 *
 * [accountId] 가 -1L 이면 홈 drawer 에서 선택한 계좌를 쓴다.
 */
@Parcelize
@Serializable
data class ScheduleAdd(
    val code: String = "",
    val price: String = "",
    val quantity: String = "",
    val accountId: Long = -1L,
) : Parcelable

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
                OrderScreen(backStack = backStack)
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
            entry<RealizedPnl> {
                RealizedPnlScreen(backStack = backStack)
            }
            entry<ScheduleOrder> {
                ScheduleOrderScreen(backStack = backStack)
            }
            entry<ScheduleAdd> { route ->
                ScheduleAddScreen(route = route, backStack = backStack)
            }
        },
    )
}
