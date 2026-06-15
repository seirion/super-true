package com.trueedu.tong.ui.chart

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.trueedu.tong.model.CandleData
import com.trueedu.tong.model.CandlePeriod

class CandleChartState(
    initialCandles: List<CandleData> = emptyList(),
    initialPeriod: CandlePeriod = CandlePeriod.DAY,
) {
    var candles by mutableStateOf(initialCandles)
    var period by mutableStateOf(initialPeriod)
    var isLoading by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    // 줌: 캔들 너비 배율 (1f = 기본)
    var zoom by mutableFloatStateOf(1f)
    // 스크롤 오프셋 (픽셀, 양수 = 오른쪽으로 밀기 = 과거 보기)
    var scrollOffset by mutableFloatStateOf(0f)
    // 십자선 위치 (null = 숨김)
    var crosshairPos by mutableStateOf<Offset?>(null)
    // 십자선이 가리키는 캔들 인덱스
    var crosshairIdx by mutableIntStateOf(-1)

    // MA 표시 여부
    var showMa by mutableStateOf(true)

    /** 새 데이터로 초기화 (스크롤 리셋 포함) */
    fun loadCandles(data: List<CandleData>) {
        candles = data
        scrollOffset = 0f
        error = null
    }

    /** 실시간 업데이트 — 스크롤/줌 상태 유지 */
    fun updateCandles(data: List<CandleData>) {
        candles = data
    }

    /** 과거 데이터 추가 (앞에 붙이기) */
    fun prependCandles(data: List<CandleData>) {
        candles = data + candles
    }

    /** 가시 범위의 시작/끝 인덱스 계산 */
    fun visibleRange(candleWidth: Float, viewportWidth: Float): IntRange {
        val totalWidth = candleWidth * candles.size
        val offset = scrollOffset.coerceIn(0f, maxOf(0f, totalWidth - viewportWidth))
        val startIdx = (offset / candleWidth).toInt()
        val count = (viewportWidth / candleWidth).toInt() + 2
        return startIdx until minOf(candles.size, startIdx + count)
    }
}

@Composable
fun rememberCandleChartState(
    candles: List<CandleData> = emptyList(),
    period: CandlePeriod = CandlePeriod.DAY,
): CandleChartState = remember(candles, period) {
    CandleChartState(candles, period)
}.also { state ->
    LaunchedEffect(candles) { if (candles.isNotEmpty()) state.candles = candles }
}
