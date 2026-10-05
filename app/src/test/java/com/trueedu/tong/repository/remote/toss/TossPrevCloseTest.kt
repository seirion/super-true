package com.trueedu.tong.repository.remote.toss

import com.trueedu.tong.model.account.HoldingStock
import com.trueedu.tong.model.dto.toss.TossDailyProfitLoss
import com.trueedu.tong.model.dto.toss.TossHolding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TossPrevCloseTest {
    private fun holding(
        last: String, qty: String, amount: String?, rate: String? = null, currency: String = "USD",
    ) = TossHolding(
        symbol = "X", currency = currency, quantity = qty, lastPrice = last,
        dailyProfitLoss = if (amount != null || rate != null)
            TossDailyProfitLoss(amount = amount ?: "", rate = rate ?: "") else null,
    )

    @Test
    fun `일간 손익 금액으로 전일 종가를 역산한다 (실측 엔비디아)`() {
        // 실측: last 235.55, qty 1.670508, daily amount 2.6728 → 전일 종가 233.95
        assertEquals(233.95, TossAccountRepository.prevClose(holding("235.55", "1.670508", "2.6728"))!!, 0.0)
        // 하락: MSFT last 517.18, qty 1.123707, amount -0.3932 → 517.53
        assertEquals(517.53, TossAccountRepository.prevClose(holding("517.18", "1.123707", "-0.3932"))!!, 0.0)
    }

    @Test
    fun `금액이 없으면 일간 손익률로 역산하고 원화는 정수로 반올림한다`() {
        assertEquals(70000.0, TossAccountRepository.prevClose(holding("71400", "3", null, rate = "0.02", currency = "KRW"))!!, 0.0)
    }

    @Test
    fun `정보가 없으면 null`() {
        assertNull(TossAccountRepository.prevClose(holding("100", "1", null)))
        assertNull(TossAccountRepository.prevClose(holding("", "1", "1")))
    }

    @Test
    fun `전일 종가 대비 등락과 등락률`() {
        val h = HoldingStock("NVDA", "NVDA", 1.0, 1.0, 235.0, 235.0, 0.0, 0.0, "USD", prevClose = 233.95)
        assertEquals(1.60, h.deltaFrom(235.55)!!, 1e-9)
        assertEquals(0.6839, h.rateFrom(235.55)!!, 1e-3)
        assertNull(h.copy(prevClose = null).deltaFrom(235.55))
    }
}
