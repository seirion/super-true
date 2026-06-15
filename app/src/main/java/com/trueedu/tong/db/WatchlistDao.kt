package com.trueedu.tong.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trueedu.tong.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(item: WatchlistItem)

    @Query("DELETE FROM watchlist WHERE code = :code")
    suspend fun delete(code: String)

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WatchlistItem>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE code = :code)")
    suspend fun contains(code: String): Boolean
}
