package com.trueedu.tong.model

import com.trueedu.tong.model.account.HoldingStock
import org.junit.Assert.assertEquals
import org.junit.Test

class HoldingStockQuoteKeyTest {
    private fun holding(code: String, currency: String) =
        HoldingStock(code, code, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, currency)

    @Test
    fun `A 로 시작하는 미국 티커는 접두사를 떼지 않는다`() {
        assertEquals("AAPL", holding("AAPL", "USD").quoteKey)
        assertEquals("AMZN", holding("AMZN", "USD").quoteKey)
        assertEquals("A", holding("A", "USD").quoteKey)
    }

    @Test
    fun `국내 코드는 키움 A 접두사를 뗀다`() {
        assertEquals("005930", holding("A005930", "KRW").quoteKey)
        assertEquals("005930", holding("005930", "KRW").quoteKey)
    }
}
