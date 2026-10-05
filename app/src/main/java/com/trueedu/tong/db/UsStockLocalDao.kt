package com.trueedu.tong.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trueedu.tong.model.UsStockLocal

@Dao
interface UsStockLocalDao {
    @Query("SELECT * FROM us_stocks ORDER BY symbol ASC")
    suspend fun getAll(): List<UsStockLocal>

    @Query("SELECT COUNT(*) FROM us_stocks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<UsStockLocal>)

    @Query("DELETE FROM us_stocks")
    suspend fun deleteAll()
}
