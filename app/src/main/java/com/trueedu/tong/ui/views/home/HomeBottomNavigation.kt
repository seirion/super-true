package com.trueedu.tong.ui.views.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

val HomeBottomNavHeight = 48.dp

// 하단 navigation bar 높이
@Composable
fun navigationBarHeight() = WindowInsets.navigationBars
    .asPaddingValues()
    .calculateBottomPadding()

@Composable
fun HomeBottomNavigation(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    indicatorColor: Color = MaterialTheme.colorScheme.outlineVariant,
    navController: NavHostController,
    currentTab: BottomNavItem?,
    onTabSelected: (BottomNavItem) -> Unit,
    onOrderTabClicked: (() -> Unit)? = null,
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Watch,
        BottomNavItem.Order,
        BottomNavItem.Menu,
    )

    AnimatedVisibility(
        visible = items.contains(currentTab)
    ) {
        NavigationBar(
            modifier = modifier.height(HomeBottomNavHeight + navigationBarHeight()),
        ) {
            items.forEach { item ->
                val selected = currentTab == item
                NavigationBarItem(
                    selected = selected,
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = item.icon(selected),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = "home-item"
                            )
                            val color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                            Text(
                                text = item.title,
                                fontSize = 10.sp,
                                lineHeight = 1.0.em,
                                color = color,
                            )
                        }
                    },
                    onClick = {
                        onTabSelected(item)
                        if (item == BottomNavItem.Order) onOrderTabClicked?.invoke()
                        navController.navigate(item) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors().copy(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        selectedIndicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        }
    }
}
