package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.model.dto.toss.TossDailyCandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TossPrevCloseCandleTest {
    // 최신순: 금요일(10/2) 233.95, 목요일(10/1) 230.00
    private val candles = listOf(
        TossDailyCandle("2026-10-02T00:00:00-04:00", "233.95"),
        TossDailyCandle("2026-10-01T00:00:00-04:00", "230.00"),
    )
    private val et = ZoneId.of("America/New_York")

    @Test
    fun `일요일 저녁 데이마켓 개장 직후에는 금요일 종가`() {
        // 일요일 20:41 ET → +4h 로 월요일 거래일
        val date = TossMarketRepository.currentUsTradingDate(ZonedDateTime.of(2026, 10, 4, 20, 41, 0, 0, et))
        assertEquals(LocalDate.of(2026, 10, 5), date)
        assertEquals(233.95, TossMarketRepository.prevCloseFromCandles(candles, date)!!, 0.0)
    }

    @Test
    fun `정규장 중에는 오늘 봉을 건너뛰고 직전 봉 종가`() {
        val withToday = listOf(TossDailyCandle("2026-10-05T00:00:00-04:00", "235.00")) + candles
        val date = TossMarketRepository.currentUsTradingDate(ZonedDateTime.of(2026, 10, 5, 11, 0, 0, 0, et))
        assertEquals(LocalDate.of(2026, 10, 5), date)
        assertEquals(233.95, TossMarketRepository.prevCloseFromCandles(withToday, date)!!, 0.0)
    }

    @Test
    fun `정규장 마감 후 20시 이전에는 오늘 종가가 아닌 어제 종가를 유지하고 20시 이후에 오늘 종가로 넘어간다`() {
        val withToday = listOf(TossDailyCandle("2026-10-05T00:00:00-04:00", "235.00")) + candles
        val evening = TossMarketRepository.currentUsTradingDate(ZonedDateTime.of(2026, 10, 5, 17, 0, 0, 0, et))
        assertEquals(233.95, TossMarketRepository.prevCloseFromCandles(withToday, evening)!!, 0.0)
        val night = TossMarketRepository.currentUsTradingDate(ZonedDateTime.of(2026, 10, 5, 20, 30, 0, 0, et))
        assertEquals(235.00, TossMarketRepository.prevCloseFromCandles(withToday, night)!!, 0.0)
    }

    @Test
    fun `데이터가 없거나 형식이 잘못되면 null`() {
        val date = LocalDate.of(2026, 10, 5)
        assertNull(TossMarketRepository.prevCloseFromCandles(emptyList(), date))
        assertNull(TossMarketRepository.prevCloseFromCandles(listOf(TossDailyCandle("bad", "1")), date))
    }
}
