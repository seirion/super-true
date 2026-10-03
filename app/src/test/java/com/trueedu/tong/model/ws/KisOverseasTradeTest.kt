package com.trueedu.tong.model.ws

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KisOverseasTradeTest {
    // RSYM^SYMB^ZDIV^TYMD^XYMD^XHMS^KYMD^KHMS^OPEN^HIGH^LOW^LAST^SIGN^DIFF^RATE^...
    private fun frame(sign: String, diff: String, rate: String, last: String = "333.69") =
        "DNASAAPL^AAPL^4^20261006^20261006^093000^20261006^223000^330.00^335.00^329.00^$last^$sign^$diff^$rate^333.60^333.70^100^200^5^1000^333000^0^0^100.00^2"

    @Test
    fun `상승 체결을 파싱한다`() {
        val t = KisOverseasTrade.from(frame("2", "2.10", "0.63"))
        assertNotNull(t)
        assertEquals("AAPL", t!!.symbol)
        assertEquals(333.69, t.price, 0.0)
        assertEquals(2.10, t.delta, 0.0)
        assertEquals(0.63, t.rate, 0.0)
        assertEquals("093000", t.localTime)
        assertEquals("223000", t.koreaTime)
    }

    @Test
    fun `하락은 부호를 적용하고 값에 이미 부호가 있어도 중복 적용하지 않는다`() {
        val a = KisOverseasTrade.from(frame("5", "2.10", "0.63"))!!
        assertEquals(-2.10, a.delta, 0.0)
        assertEquals(-0.63, a.rate, 0.0)
        val b = KisOverseasTrade.from(frame("5", "-2.10", "-0.63"))!!
        assertEquals(-2.10, b.delta, 0.0)
        assertEquals(-0.63, b.rate, 0.0)
    }

    @Test
    fun `보합은 0 이고 가격 없는 프레임은 null`() {
        val flat = KisOverseasTrade.from(frame("3", "0.00", "0.00"))!!
        assertEquals(0.0, flat.delta, 0.0)
        assertEquals(0.0, flat.rate, 0.0)
        assertNull(KisOverseasTrade.from("DNASAAPL^AAPL"))
        assertNull(KisOverseasTrade.from(""))
    }
}
