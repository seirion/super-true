package com.trueedu.tong.model.ws

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TossRealTimeTradeTest {
    private fun parse(text: String) =
        TossRealTimeTrade.fromFrame(Json.parseToJsonElement(text).jsonObject, receivedAtMs = 1L)

    @Test
    fun `미국 체결 프레임을 파싱한다`() {
        val t = parse(
            """{"type":"message","topic":"trade:us:AAPL","data":{"price":"333.69","volume":"12","timestamp":"2026-10-06T22:30:00Z","currency":"USD"}}"""
        )
        assertNotNull(t)
        assertEquals("AAPL", t!!.symbol)
        assertEquals(333.69, t.price, 0.0)
        assertEquals(12.0, t.volume, 0.0)
        assertEquals("USD", t.currency)
    }

    @Test
    fun `국내 체결 프레임은 통화가 없으면 KRW`() {
        val t = parse("""{"type":"message","topic":"trade:kr:005930","data":{"price":"71000","volume":"3"}}""")
        assertEquals("005930", t!!.symbol)
        assertEquals("KRW", t.currency)
        assertEquals(71000.0, t.price, 0.0)
    }

    @Test
    fun `호가 채널이나 가격 없는 프레임은 무시한다`() {
        assertNull(parse("""{"type":"message","topic":"orderbook:us:AAPL","data":{"asks":[],"bids":[]}}"""))
        assertNull(parse("""{"type":"message","topic":"trade:us:AAPL","data":{"volume":"1"}}"""))
        assertNull(parse("""{"type":"message","topic":"trade:us","data":{"price":"1"}}"""))
    }
}
