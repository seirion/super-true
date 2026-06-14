package com.trueedu.tong.ui.chart

import com.trueedu.tong.model.CandleData

object ChartMath {
    /** 이동평균선 계산. 데이터 부족 구간은 null */
    fun ma(candles: List<CandleData>, period: Int): List<Double?> =
        candles.mapIndexed { i, _ ->
            if (i < period - 1) null
            else candles.subList(i - period + 1, i + 1).map { it.close }.average()
        }

    /** 가격 → Y 픽셀 변환 */
    fun priceToY(price: Double, minPrice: Double, maxPrice: Double, chartHeight: Float): Float {
        if (maxPrice == minPrice) return chartHeight / 2f
        return chartHeight * (1f - ((price - minPrice) / (maxPrice - minPrice)).toFloat())
    }

    /** 표시 범위 내 최고/최저가 계산 (고가/저가 기준) */
    fun priceRange(candles: List<CandleData>, startIdx: Int, endIdx: Int): Pair<Double, Double> {
        val visible = candles.subList(
            maxOf(0, startIdx),
            minOf(candles.size, endIdx + 1)
        )
        if (visible.isEmpty()) return 0.0 to 1.0
        val min = visible.minOf { it.low }
        val max = visible.maxOf { it.high }
        val padding = (max - min) * 0.05
        return (min - padding) to (max + padding)
    }

    /** Y축 가격 눈금 계산 (보기 좋은 간격) */
    fun priceGridLines(min: Double, max: Double, count: Int = 5): List<Double> {
        val range = max - min
        if (range <= 0) return emptyList()
        val rawStep = range / count
        val magnitude = Math.pow(10.0, Math.floor(Math.log10(rawStep)))
        val step = when {
            rawStep / magnitude <= 1.5 -> magnitude
            rawStep / magnitude <= 3.5 -> magnitude * 2
            rawStep / magnitude <= 7.5 -> magnitude * 5
            else -> magnitude * 10
        }
        val firstLine = Math.ceil(min / step) * step
        val lines = mutableListOf<Double>()
        var line = firstLine
        while (line <= max) {
            lines.add(line)
            line += step
        }
        return lines
    }
}
