package com.trueedu.tong.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trueedu.tong.model.BrokerAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface BrokerAccountDao {
    @Query("SELECT * FROM broker_accounts ORDER BY createdAt ASC")
    fun getAll(): Flow<List<BrokerAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: BrokerAccount): Long

    @Delete
    suspend fun delete(account: BrokerAccount)

    @Update
    suspend fun update(account: BrokerAccount)

    @Query("UPDATE broker_accounts SET isSelected = 0")
    suspend fun clearSelection()

    @Query("UPDATE broker_accounts SET isSelected = 1 WHERE id = :id")
    suspend fun select(id: Long)
}
