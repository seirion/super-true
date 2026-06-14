package com.trueedu.tong.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trueedu.tong.model.CandleData
import com.trueedu.tong.model.CandlePeriod
import timber.log.Timber

/**
 * 재사용 가능한 캔들차트 컴포저블.
 *
 * @param candles OHLCV 데이터 리스트 (최신 → 과거 순서)
 * @param period 현재 표시 기간
 * @param config 색상/두께 등 설정
 * @param onLoadMore 스크롤 끝(과거) 도달 시 추가 데이터 요청 콜백
 * @param onPeriodChange 기간 선택 버튼 클릭 시 콜백 (데이터 재조회 트리거)
 * @param modifier
 */
@Composable
fun CandleChartView(
    candles: List<CandleData>,
    period: CandlePeriod = CandlePeriod.DAY,
    config: ChartConfig = ChartConfig(),
    onLoadMore: () -> Unit = {},
    onPeriodChange: (CandlePeriod) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state = remember { CandleChartState() }
    var needSnap by remember { mutableStateOf(true) }

    // 입력은 최신→과거 순서이므로 차트 내부에서는 과거→최신(좌→우)으로 뒤집어 사용
    LaunchedEffect(candles) {
        Timber.tag("CandleChartView").d("candles updated: size=${candles.size}")
        state.loadCandles(candles.asReversed())
        needSnap = true
    }
    LaunchedEffect(period) { state.period = period }

    Column(modifier.background(config.backgroundColor)) {
        ChartControlBar(
            state = state,
            selected = state.period,
            config = config,
            onPeriodChange = { p ->
                state.period = p
                onPeriodChange(p)
            },
        )

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state.candles.isEmpty()) {
                Timber.tag("CandleChartView").d("state.candles is EMPTY - showing '데이터 없음'")
                Text(
                    text = "데이터 없음",
                    color = Color(0xFF888899),
                    modifier = Modifier.align(Alignment.Center),
                )
                return@BoxWithConstraints
            }
            Timber.tag("CandleChartView").d("rendering ${state.candles.size} candles")

            val density = LocalDensity.current
            val priceAxisPx = with(density) { config.priceAxisWidth.toPx() }
            val dateAxisPx = with(density) { config.dateAxisHeight.toPx() }
            val minPx = with(density) { config.candleMinWidth.toPx() }
            val maxPx = with(density) { config.candleMaxWidth.toPx() }
            val basePx = with(density) { 8.dp.toPx() }
            val candleWidth = (basePx * state.zoom).coerceIn(minPx, maxPx)

            val totalWidthPx = constraints.maxWidth.toFloat()
            val totalHeightPx = constraints.maxHeight.toFloat()
            val plotWidth = totalWidthPx - priceAxisPx
            val totalPlotHeight = totalHeightPx - dateAxisPx
            // 캔들 영역 80% / 거래량 영역 20%
            val volumeRatio = 0.2f
            val volumeHeight = totalPlotHeight * volumeRatio
            val dividerPx = with(density) { 4.dp.toPx() }
            val plotHeight = totalPlotHeight * (1f - volumeRatio) - dividerPx // 4dp 구분 여백

            // 신규 데이터 로드 시 최신(우측 끝)으로 스냅
            // plotWidth가 확정된 BoxWithConstraints 안에서 즉시 처리
            if (needSnap && plotWidth > 0f && state.candles.isNotEmpty()) {
                val total = candleWidth * state.candles.size
                state.scrollOffset = maxOf(0f, total - plotWidth)
                needSnap = false
            }

            val maList = remember(state.candles, config.maPeriods) {
                config.maPeriods.map { ChartMath.ma(state.candles, it) }
            }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .candleChartGestures(
                        state = state,
                        candleWidth = candleWidth,
                        viewportWidth = plotWidth,
                        onLoadMore = onLoadMore,
                    )
            ) {
                val range = state.visibleRange(candleWidth, plotWidth)
                if (range.isEmpty()) return@Canvas
                val (minPrice, maxPrice) =
                    ChartMath.priceRange(state.candles, range.first, range.last)

                // 가격 그리드 라벨은 plotWidth 오른쪽에 그려지므로 clipRect 밖에서 먼저 그림
                drawPriceGrid(minPrice, maxPrice, plotWidth, plotHeight, config)

                // 캔들 + MA는 캔들 영역으로만 클리핑 (거래량 영역/가격축 침범 방지)
                clipRect(left = 0f, top = 0f, right = plotWidth, bottom = plotHeight) {
                    for (i in range) {
                        val cx = i * candleWidth - state.scrollOffset + candleWidth / 2f
                        if (cx < -candleWidth || cx > plotWidth + candleWidth) continue
                        drawCandle(
                            state.candles[i], cx, candleWidth, minPrice, maxPrice, plotHeight, config
                        )
                    }

                    if (state.showMa) {
                        maList.forEachIndexed { idx, values ->
                            drawMaLine(
                                maValues = values,
                                range = range,
                                candleWidth = candleWidth,
                                scrollOffset = state.scrollOffset,
                                minPrice = minPrice,
                                maxPrice = maxPrice,
                                color = config.maColors[idx % config.maColors.size],
                                plotHeight = plotHeight,
                            )
                        }
                    }
                }

                // 거래량 막대 차트
                val volumeTop = plotHeight + dividerPx
                drawVolumeChart(
                    candles = state.candles,
                    range = range,
                    candleWidth = candleWidth,
                    scrollOffset = state.scrollOffset,
                    volumeTop = volumeTop,
                    volumeHeight = volumeHeight,
                    config = config,
                )

                drawDateAxis(
                    candles = state.candles,
                    range = range,
                    candleWidth = candleWidth,
                    scrollOffset = state.scrollOffset,
                    period = state.period,
                    plotWidth = plotWidth,
                    plotHeight = totalPlotHeight,
                    config = config,
                )

                state.crosshairPos?.let { drawCrosshair(it, plotWidth, plotHeight, config) }
            }

            CrosshairOverlay(state = state, candleWidth = candleWidth)
        }
    }
}

/** 십자선이 가리키는 캔들의 OHLCV 오버레이 */
@Composable
private fun CrosshairOverlay(
    state: CandleChartState,
    candleWidth: Float,
) {
    val pos = state.crosshairPos ?: return
    if (candleWidth <= 0f) return
    val idx = ((pos.x + state.scrollOffset) / candleWidth).toInt()
    val candle = state.candles.getOrNull(idx) ?: return

    Box(
        Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = formatDate(candle.datetime, state.period),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            OhlcvRow("시", candle.open)
            OhlcvRow("고", candle.high)
            OhlcvRow("저", candle.low)
            OhlcvRow("종", candle.close)
            Text(
                text = "량 ${"%,d".format(candle.volume)}",
                color = Color(0xFFB0B0C0),
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun OhlcvRow(label: String, value: Double) {
    Text(
        text = "$label ${"%,.0f".format(value)}",
        color = Color.White,
        fontSize = 10.sp,
    )
}

/** [분봉] [일봉] [주봉] [월봉] | MA 토글 */
@Composable
private fun ChartControlBar(
    state: CandleChartState,
    selected: CandlePeriod,
    config: ChartConfig,
    onPeriodChange: (CandlePeriod) -> Unit,
) {
    val periods = listOf(
        CandlePeriod.MINUTE to "분봉",
        CandlePeriod.DAY to "일봉",
        CandlePeriod.WEEK to "주봉",
        CandlePeriod.MONTH to "월봉",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        periods.forEach { (p, label) ->
            PeriodChip(
                label = label,
                selected = p == selected,
                onClick = { onPeriodChange(p) },
            )
            Spacer(Modifier.width(6.dp))
        }
        Spacer(Modifier.weight(1f))
        // MA 토글 (전체 on/off)
        Row(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { state.showMa = !state.showMa }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            config.maPeriods.forEachIndexed { idx, period ->
                Text(
                    text = "MA$period",
                    color = if (state.showMa) {
                        config.maColors[idx % config.maColors.size]
                    } else {
                        Color(0xFF55555F)
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (selected) Color.White else Color(0xFF8888A0),
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Color(0xFF3A3A5C) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
