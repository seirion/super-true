package com.trueedu.tong.ui.views.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
sealed class BottomNavItem {
    abstract val title: String

    abstract fun iconSelected(): ImageVector
    abstract fun iconNormal(): ImageVector

    fun icon(selected: Boolean) = if (selected) iconSelected() else iconNormal()

    @Serializable
    data object Home : BottomNavItem() {
        override val title: String = "홈"
        override fun iconSelected(): ImageVector = Icons.Filled.Home
        override fun iconNormal(): ImageVector = Icons.Outlined.Home
    }

    @Serializable
    data object Watch : BottomNavItem() {
        override val title: String = "관심"
        override fun iconSelected(): ImageVector = Icons.Filled.Star
        override fun iconNormal(): ImageVector = Icons.Outlined.StarOutline
    }

    @Serializable
    data object Order : BottomNavItem() {
        override val title: String = "주문"
        override fun iconSelected(): ImageVector = Icons.Filled.ShoppingCart
        override fun iconNormal(): ImageVector = Icons.Outlined.ShoppingCart
    }

    @Serializable
    data object Menu : BottomNavItem() {
        override val title: String = "더보기"
        override fun iconSelected(): ImageVector = Icons.Filled.Menu
        override fun iconNormal(): ImageVector = Icons.Outlined.Menu
    }
}
