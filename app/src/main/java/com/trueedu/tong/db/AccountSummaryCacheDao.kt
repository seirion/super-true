package com.trueedu.tong.db

import androidx.room.*
import com.trueedu.tong.model.CachedAccountSummary
import com.trueedu.tong.model.CachedHolding

@Dao
interface AccountSummaryCacheDao {
    @Query("SELECT * FROM cached_account_summaries WHERE accountId = :accountId")
    suspend fun getSummary(accountId: Long): CachedAccountSummary?

    @Query("SELECT * FROM cached_holdings WHERE accountId = :accountId")
    suspend fun getHoldings(accountId: Long): List<CachedHolding>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: CachedAccountSummary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldings(holdings: List<CachedHolding>)

    @Query("DELETE FROM cached_holdings WHERE accountId = :accountId")
    suspend fun deleteHoldings(accountId: Long)

    // 계좌 삭제 시 캐시도 정리
    @Query("DELETE FROM cached_account_summaries WHERE accountId = :accountId")
    suspend fun deleteSummary(accountId: Long)
}
