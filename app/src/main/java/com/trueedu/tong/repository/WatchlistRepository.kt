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
    suspend fun add(code: String, nameKr: String) = watchlistDao.insertOrIgnore(WatchlistItem(code, nameKr))
    suspend fun remove(code: String) = watchlistDao.delete(code)
    suspend fun contains(code: String): Boolean = watchlistDao.contains(code)

    /**
     * 주어진 code 순서대로 sortOrder(0..n)를 재할당해 저장한다.
     * 드래그&드랍 편집 완료 시 한 번에 호출.
     */
    suspend fun saveOrder(codesInOrder: List<String>) {
        val byCode = watchlistDao.getAll().first().associateBy { it.code }
        val reindexed = codesInOrder.mapIndexedNotNull { index, code ->
            byCode[code]?.copy(sortOrder = index)
        }
        if (reindexed.isNotEmpty()) {
            watchlistDao.update(reindexed)
        }
    }
}
