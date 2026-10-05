package com.trueedu.tong.model.dto

import com.trueedu.tong.model.dto.toss.TossBuyingPowerResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TossBuyingPowerTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun `매수 가능 금액 응답을 파싱한다`() {
        val krw = json.decodeFromString<TossBuyingPowerResponse>(
            """{"result":{"currency":"KRW","cashBuyingPower":"1250000"}}"""
        )
        assertEquals("KRW", krw.result!!.currency)
        assertEquals(1250000.0, krw.result!!.cashBuyingPower.toDouble(), 0.0)

        val usd = json.decodeFromString<TossBuyingPowerResponse>(
            """{"result":{"currency":"USD","cashBuyingPower":"123.45","extra":"x"}}"""
        )
        assertEquals(123.45, usd.result!!.cashBuyingPower.toDouble(), 0.0)
    }

    @Test
    fun `결과가 없으면 null`() {
        assertNull(json.decodeFromString<TossBuyingPowerResponse>("""{}""").result)
    }
}
