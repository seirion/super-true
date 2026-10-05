package com.trueedu.tong.repository

import com.trueedu.tong.model.UsStockLocal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsStockSearchTest {
    private val stocks = listOf(
        UsStockLocal("AAPL", "애플", "NASDAQ", "STOCK"),
        UsStockLocal("AAP", "어드밴스 오토 파츠", "NYSE", "STOCK"),
        UsStockLocal("MSFT", "마이크로소프트", "NASDAQ", "STOCK"),
        UsStockLocal("QQQ", "인베스코 QQQ 트러스트", "NASDAQ", "ETF"),
        UsStockLocal("PLTR", "팔란티어 테크놀로지스", "NASDAQ", "STOCK"),
        UsStockLocal("SNAP", "스냅", "NYSE", "STOCK"),
    )

    @Test
    fun `검색어가 비면 빈 목록`() {
        assertTrue(UsStockRepository.search(stocks, "").isEmpty())
        assertTrue(UsStockRepository.search(stocks, "   ").isEmpty())
    }

    @Test
    fun `티커 일치가 먼저 나오고 접두 일치가 그 다음`() {
        val r = UsStockRepository.search(stocks, "aap").map { it.symbol }
        assertEquals(listOf("AAP", "AAPL"), r) // 정확 일치 AAP → 접두 AAPL
    }

    @Test
    fun `한글 이름으로 검색하고 대소문자를 구분하지 않는다`() {
        assertEquals(listOf("MSFT"), UsStockRepository.search(stocks, "마이크로").map { it.symbol })
        assertEquals(listOf("QQQ"), UsStockRepository.search(stocks, "qqq").map { it.symbol })
    }

    @Test
    fun `일치하지 않으면 빈 목록`() {
        assertTrue(UsStockRepository.search(stocks, "zzzz").isEmpty())
    }

    @Test
    fun `결과는 최대 100건`() {
        val many = (1..300).map { UsStockLocal("A$it", "종목$it", "NASDAQ", "STOCK") }
        assertEquals(100, UsStockRepository.search(many, "A").size)
    }
}
