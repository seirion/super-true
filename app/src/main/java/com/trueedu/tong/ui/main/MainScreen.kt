package com.trueedu.tong.ui.main

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.trueedu.tong.ui.views.order.OrderViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trueedu.tong.ui.navigation.bottomNavItemOrNull
import com.trueedu.tong.ui.views.home.BottomNavItem
import com.trueedu.tong.ui.views.home.HomeBottomNavigation
import com.trueedu.tong.ui.views.home.HomeDrawer
import com.trueedu.tong.ui.views.home.HomeViewModel
import com.trueedu.tong.ui.views.watch.WatchViewModel
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    mainNavigation: @Composable (navController: NavHostController, innerPadding: PaddingValues) -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var currentTab by remember { mutableStateOf<BottomNavItem?>(BottomNavItem.Home) }
    currentTab = navBackStackEntry.bottomNavItemOrNull() ?: currentTab

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentTab == BottomNavItem.Home,
        drawerContent = {
            HomeDrawer(
                onAddAccount = { navController.navigate(AddAccount()) },
                onEditAccount = { id ->
                    navController.navigate(AddAccount(accountId = id))
                    scope.launch { drawerState.close() }
                },
                close = { scope.launch { drawerState.close() } },
            )
        },
        content = {
            Scaffold(
                bottomBar = {
                    val orderVm: OrderViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
                    val homeVm: HomeViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
                    val watchVm: WatchViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
                    val selectedAccount by homeVm.selectedAccount.collectAsState()
                    HomeBottomNavigation(
                        navController = navController,
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        onOrderTabClicked = {
                            // 주문 탭 진입 시 현재 선택된 계좌로 업데이트
                            selectedAccount?.let { acc ->
                                orderVm.onOrderTabEntered(acc.id)
                            } ?: orderVm.onOrderTabEntered()
                        },
                        onTabActivated = { tab ->
                            // 이전 탭 비활성화
                            if (tab != BottomNavItem.Watch) watchVm.deactivate()
                            // 탭 전환 시 해당 탭의 종목으로 실시간 시세 구독 교체
                            when (tab) {
                                BottomNavItem.Home -> homeVm.activateRealtime()
                                BottomNavItem.Watch -> watchVm.activateRealtime()
                                BottomNavItem.Order -> orderVm.activateRealtime()
                                else -> Unit
                            }
                        },
                    )
                },
            ) { innerPadding ->
                // 탭 영역 제외하고 화면이 그려지도록
                val padding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                mainNavigation(navController, padding)
            }
        }
    )
}
