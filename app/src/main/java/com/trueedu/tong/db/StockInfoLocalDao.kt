package com.trueedu.tong.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trueedu.tong.model.StockInfoLocal

@Dao
interface StockInfoLocalDao {
    @Insert
    suspend fun insert(stock: StockInfoLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<StockInfoLocal>)

    @Query("SELECT * FROM stocks WHERE code = :code")
    suspend fun getStockByCode(code: String): StockInfoLocal?

    @Query("SELECT * FROM stocks")
    suspend fun getAllStocks(): List<StockInfoLocal>

    @Query("DELETE FROM stocks")
    suspend fun deleteAllStocks()
}
