package com.trueedu.tong.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ChartColor {
    val rise = Color(0xFFE53935)   // 빨강 (상승)
    val fall = Color(0xFF1E88E5)   // 파랑 (하락)
    val flat = Color(0xFF9E9E9E)   // 회색 (보합)

    @Composable
    fun color(value: Double): Color = when {
        value > 0 -> rise
        value < 0 -> fall
        else -> MaterialTheme.colorScheme.onSurface
    }
}
