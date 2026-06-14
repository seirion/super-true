package com.trueedu.tong.ui.chart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ChartConfig(
    val riseColor: Color = Color(0xFFE53935),
    val fallColor: Color = Color(0xFF1E88E5),
    val flatColor: Color = Color(0xFF9E9E9E),
    val maColors: List<Color> = listOf(
        Color(0xFFFFB300),  // MA5: 노랑
        Color(0xFF7B1FA2),  // MA20: 보라
        Color(0xFF00897B),  // MA60: 청록
        Color(0xFFE64A19),  // MA120: 주황
    ),
    val maPeriods: List<Int> = listOf(5, 20, 60, 120),
    val candleMinWidth: Dp = 4.dp,
    val candleMaxWidth: Dp = 40.dp,
    val candleSpacingRatio: Float = 0.3f,  // 캔들 간격 = width * ratio
    val priceAxisWidth: Dp = 60.dp,        // 우측 가격축 너비
    val dateAxisHeight: Dp = 24.dp,        // 하단 날짜축 높이
    val crosshairColor: Color = Color(0x88FFFFFF),
    val gridColor: Color = Color(0x22FFFFFF),
    val backgroundColor: Color = Color(0xFF1A1A2E),
)
