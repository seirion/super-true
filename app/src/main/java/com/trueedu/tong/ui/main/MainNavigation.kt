package com.trueedu.tong.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.trueedu.tong.ui.views.account.AddAccountScreen
import com.trueedu.tong.ui.views.home.BottomNavItem
import com.trueedu.tong.ui.views.home.HomeScreen
import com.trueedu.tong.ui.views.menu.AccountTransferScreen
import com.trueedu.tong.ui.views.menu.MenuScreen
import com.trueedu.tong.ui.views.order.OrderScreen
import kotlinx.serialization.Serializable

@Serializable
data class AddAccount(val accountId: Long = -1L)  // -1L = 신규 추가

@Serializable
data object AccountTransfer

@Composable
fun MainNavigation(
    navController: NavHostController,
    innerPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        composable<BottomNavItem.Home> {
            HomeScreen(navController = navController)
        }
        composable<BottomNavItem.Watch> {
            PlaceholderScreen("관심 화면")
        }
        composable<BottomNavItem.Order> {
            OrderScreen()
        }
        composable<BottomNavItem.Menu> {
            MenuScreen(navController = navController)
        }
        composable<AddAccount> {
            AddAccountScreen(onBack = { navController.popBackStack() })
        }
        composable<AccountTransfer> {
            AccountTransferScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text)
    }
}
