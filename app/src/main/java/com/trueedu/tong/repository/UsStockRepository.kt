package com.trueedu.tong.repository

import android.content.Context
import androidx.room.withTransaction
import com.trueedu.tong.db.AppDatabase
import com.trueedu.tong.db.UsStockLocalDao
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.UsStockLocal
import com.trueedu.tong.repository.remote.toss.TossMarketRepository
import com.trueedu.tong.utils.logD
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 미국 종목 검색용 유니버스 (토스 /stocks/all) 로컬 캐시.
 * 데이터가 크지 않고 저변동이라 [REFRESH_INTERVAL_MS] (7일)마다만 다시 받는다.
 */
@Singleton
class UsStockRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: UsStockLocalDao,
    private val tossMarket: TossMarketRepository,
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("us_stock_universe", Context.MODE_PRIVATE)

    companion object {
        const val REFRESH_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val MAX_RESULTS = 100

        /**
         * 티커/한글명 검색. 티커 일치 → 티커 접두 → 이름 포함 순으로 정렬한다.
         * 검색어가 비어 있으면 빈 목록 (전체를 나열하지 않음).
         */
        fun search(all: List<UsStockLocal>, query: String): List<UsStockLocal> {
            val q = query.trim()
            if (q.isEmpty()) return emptyList()
            fun rank(s: UsStockLocal): Int = when {
                s.symbol.equals(q, ignoreCase = true) -> 0
                s.symbol.startsWith(q, ignoreCase = true) -> 1
                s.nameKr.startsWith(q, ignoreCase = true) -> 2
                s.symbol.contains(q, ignoreCase = true) -> 3
                s.nameKr.contains(q, ignoreCase = true) -> 4
                else -> Int.MAX_VALUE
            }
            return all.map { it to rank(it) }
                .filter { it.second != Int.MAX_VALUE }
                .sortedWith(compareBy({ it.second }, { it.first.symbol }))
                .take(MAX_RESULTS)
                .map { it.first }
        }
    }

    suspend fun getAll(): List<UsStockLocal> = dao.getAll()

    /**
     * 캐시가 없거나 오래됐으면 토스에서 다시 받아 교체한다. 받기에 실패하면 기존 캐시를 그대로 둔다.
     * @return 사용 가능한 캐시가 있으면 true
     */
    suspend fun refreshIfStale(account: BrokerAccount): Boolean {
        val empty = dao.count() == 0
        val age = System.currentTimeMillis() - prefs.getLong(KEY_FETCHED_AT, 0L)
        if (!empty && age < REFRESH_INTERVAL_MS) return true

        val fresh = tossMarket.fetchUsUniverse(account) ?: return !empty
        if (fresh.isEmpty()) return !empty
        database.withTransaction {
            dao.deleteAll()
            dao.insertAll(fresh)
        }
        prefs.edit().putLong(KEY_FETCHED_AT, System.currentTimeMillis()).apply()
        logD("미국 종목 유니버스 갱신: ${fresh.size}건")
        return true
    }
}
