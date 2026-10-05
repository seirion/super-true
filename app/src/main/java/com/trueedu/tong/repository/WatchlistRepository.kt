package com.trueedu.tong.repository

import com.trueedu.tong.db.WatchlistDao
import com.trueedu.tong.model.WatchlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
) {
    fun getAll(): Flow<List<WatchlistItem>> = watchlistDao.getAll()

    suspend fun add(code: String, nameKr: String, market: String = WatchlistItem.MARKET_KR) =
        watchlistDao.insertOrIgnore(WatchlistItem(code = code, nameKr = nameKr, market = market))

    suspend fun remove(code: String, market: String = WatchlistItem.MARKET_KR) =
        watchlistDao.delete(code, market)

    suspend fun contains(code: String, market: String = WatchlistItem.MARKET_KR): Boolean =
        watchlistDao.contains(code, market)

    /**
     * 해당 시장의 종목을 주어진 code 순서대로 sortOrder(0..n)를 재할당해 저장한다.
     * 드래그&드랍 편집 완료 시 한 번에 호출.
     */
    suspend fun saveOrder(market: String, codesInOrder: List<String>) {
        val byCode = watchlistDao.getAll().first()
            .filter { it.market == market }
            .associateBy { it.code }
        val reindexed = codesInOrder.mapIndexedNotNull { index, code ->
            byCode[code]?.copy(sortOrder = index)
        }
        if (reindexed.isNotEmpty()) {
            watchlistDao.update(reindexed)
        }
    }
}
