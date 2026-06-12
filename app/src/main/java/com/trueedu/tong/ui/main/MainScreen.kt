package com.trueedu.tong.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trueedu.tong.ui.navigation.bottomNavItemOrNull
import com.trueedu.tong.ui.views.home.BottomNavItem
import com.trueedu.tong.ui.views.home.HomeBottomNavigation
import com.trueedu.tong.ui.views.home.HomeDrawer
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
                    HomeBottomNavigation(
                        navController = navController,
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
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
