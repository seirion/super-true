package com.trueedu.tong.ui.chart

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import com.trueedu.tong.model.CandleData
import com.trueedu.tong.model.CandlePeriod
import java.util.Locale

private val labelColor = Color(0xFFB0B0C0)

/** 캔들 하나 그리기 (centerX = 캔들 중심 X 좌표) */
internal fun DrawScope.drawCandle(
    candle: CandleData,
    centerX: Float,
    candleWidth: Float,
    minPrice: Double,
    maxPrice: Double,
    plotHeight: Float,
    config: ChartConfig,
) {
    val color = when {
        candle.close > candle.open -> config.riseColor
        candle.close < candle.open -> config.fallColor
        else -> config.flatColor
    }
    val highY = ChartMath.priceToY(candle.high, minPrice, maxPrice, plotHeight)
    val lowY = ChartMath.priceToY(candle.low, minPrice, maxPrice, plotHeight)
    val openY = ChartMath.priceToY(candle.open, minPrice, maxPrice, plotHeight)
    val closeY = ChartMath.priceToY(candle.close, minPrice, maxPrice, plotHeight)

    // 꼬리(고가-저가)
    drawLine(
        color = color,
        start = Offset(centerX, highY),
        end = Offset(centerX, lowY),
        strokeWidth = maxOf(1f, candleWidth * 0.1f),
    )

    // 몸통(시가-종가)
    val bodyW = candleWidth * (1f - config.candleSpacingRatio)
    val top = minOf(openY, closeY)
    val bottom = maxOf(openY, closeY)
    val bodyH = maxOf(1f, bottom - top)
    drawRect(
        color = color,
        topLeft = Offset(centerX - bodyW / 2f, top),
        size = Size(bodyW, bodyH),
    )
}

/** 이동평균선 그리기 */
internal fun DrawScope.drawMaLine(
    maValues: List<Double?>,
    range: IntRange,
    candleWidth: Float,
    scrollOffset: Float,
    minPrice: Double,
    maxPrice: Double,
    color: Color,
    plotHeight: Float,
) {
    val path = Path()
    var started = false
    for (i in range) {
        val v = maValues.getOrNull(i) ?: continue
        val cx = i * candleWidth - scrollOffset + candleWidth / 2f
        val y = ChartMath.priceToY(v, minPrice, maxPrice, plotHeight)
        if (!started) {
            path.moveTo(cx, y)
            started = true
        } else {
            path.lineTo(cx, y)
        }
    }
    if (started) drawPath(path, color, style = Stroke(width = 2f))
}

/** 가격 그리드 + 우측 가격축 라벨 */
internal fun DrawScope.drawPriceGrid(
    minPrice: Double,
    maxPrice: Double,
    plotWidth: Float,
    plotHeight: Float,
    config: ChartConfig,
) {
    val paint = Paint().apply {
        color = labelColor.toArgb()
        textSize = 10.sp.toPx()
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }
    val textOffset = paint.textSize / 3f
    ChartMath.priceGridLines(minPrice, maxPrice).forEach { price ->
        val y = ChartMath.priceToY(price, minPrice, maxPrice, plotHeight)
        drawLine(
            color = config.gridColor,
            start = Offset(0f, y),
            end = Offset(plotWidth, y),
            strokeWidth = 1f,
        )
        drawContext.canvas.nativeCanvas.drawText(
            fmtPrice(price),
            plotWidth + 6f,
            y + textOffset,
            paint,
        )
    }
}

/** 하단 날짜축 */
internal fun DrawScope.drawDateAxis(
    candles: List<CandleData>,
    range: IntRange,
    candleWidth: Float,
    scrollOffset: Float,
    period: CandlePeriod,
    plotWidth: Float,
    plotHeight: Float,
    config: ChartConfig,
) {
    if (range.isEmpty()) return
    val paint = Paint().apply {
        color = labelColor.toArgb()
        textSize = 10.sp.toPx()
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val span = range.last - range.first + 1
    val step = maxOf(1, span / 5)
    var i = range.first
    while (i <= range.last) {
        val candle = candles.getOrNull(i)
        if (candle != null) {
            val cx = i * candleWidth - scrollOffset + candleWidth / 2f
            if (cx in 0f..plotWidth) {
                drawContext.canvas.nativeCanvas.drawText(
                    formatDate(candle.datetime, period),
                    cx,
                    plotHeight + paint.textSize + 4f,
                    paint,
                )
            }
        }
        i += step
    }
}

/** 십자선 */
internal fun DrawScope.drawCrosshair(
    pos: Offset,
    plotWidth: Float,
    plotHeight: Float,
    config: ChartConfig,
) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
    val x = pos.x.coerceIn(0f, plotWidth)
    val y = pos.y.coerceIn(0f, plotHeight)
    drawLine(
        color = config.crosshairColor,
        start = Offset(x, 0f),
        end = Offset(x, plotHeight),
        strokeWidth = 1f,
        pathEffect = dash,
    )
    drawLine(
        color = config.crosshairColor,
        start = Offset(0f, y),
        end = Offset(plotWidth, y),
        strokeWidth = 1f,
        pathEffect = dash,
    )
}

private fun fmtPrice(v: Double): String =
    if (kotlin.math.abs(v) >= 1000) String.format(Locale.US, "%,.0f", v)
    else String.format(Locale.US, "%,.1f", v)

/** 날짜 포맷: 일봉=MM/dd, 주봉/월봉=yyyy/MM, 분봉=HH:mm */
internal fun formatDate(dt: String, period: CandlePeriod): String = try {
    when (period) {
        CandlePeriod.MINUTE ->
            if (dt.length >= 12) "${dt.substring(8, 10)}:${dt.substring(10, 12)}" else dt
        CandlePeriod.DAY ->
            if (dt.length >= 8) "${dt.substring(4, 6)}/${dt.substring(6, 8)}" else dt
        CandlePeriod.WEEK, CandlePeriod.MONTH ->
            if (dt.length >= 6) "${dt.substring(0, 4)}/${dt.substring(4, 6)}" else dt
    }
} catch (e: Exception) {
    dt
}
