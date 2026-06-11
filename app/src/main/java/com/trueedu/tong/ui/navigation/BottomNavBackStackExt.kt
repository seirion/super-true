package com.trueedu.tong.ui.navigation

import androidx.navigation.NavBackStackEntry
import com.trueedu.tong.ui.views.home.BottomNavItem

fun NavBackStackEntry?.bottomNavItemOrNull(): BottomNavItem? {
    val entry = this ?: return null
    val route = entry.destination.route ?: return null

    // Typed navigation with object destinations can have zero arguments.
    // In that case, parsing via toRoute<T>() may succeed regardless of the actual destination,
    // which would incorrectly keep the first item (e.g. Home) selected.
    fun matchesRoute(qualifiedName: String?): Boolean {
        if (qualifiedName.isNullOrBlank()) return false
        return route == qualifiedName ||
            route.startsWith("$qualifiedName/") ||
            route.startsWith("$qualifiedName?")
    }

    return when {
        matchesRoute(BottomNavItem.Home::class.qualifiedName) -> BottomNavItem.Home
        matchesRoute(BottomNavItem.Watch::class.qualifiedName) -> BottomNavItem.Watch
        matchesRoute(BottomNavItem.Order::class.qualifiedName) -> BottomNavItem.Order
        matchesRoute(BottomNavItem.Menu::class.qualifiedName) -> BottomNavItem.Menu
        else -> null
    }
}
