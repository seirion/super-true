package com.trueedu.tong.repository

import com.trueedu.tong.db.WatchlistDao
import com.trueedu.tong.model.WatchlistItem
import kotlinx.coroutines.flow.Flow
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
}
