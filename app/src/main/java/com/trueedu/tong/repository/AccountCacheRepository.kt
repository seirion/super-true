package com.trueedu.tong.repository

import com.trueedu.tong.db.AccountSummaryCacheDao
import com.trueedu.tong.model.CachedAccountSummary
import com.trueedu.tong.model.CachedHolding
import com.trueedu.tong.model.account.AccountSummary
import com.trueedu.tong.model.account.HoldingStock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountCacheRepository @Inject constructor(
    private val dao: AccountSummaryCacheDao,
) {
    suspend fun load(accountId: Long): AccountSummary? {
        val summary = dao.getSummary(accountId) ?: return null
        val holdings = dao.getHoldings(accountId)
        return AccountSummary(
            accountId = summary.accountId,
            totalAsset = summary.totalAsset,
            deposit = summary.deposit,
            depositD1 = summary.depositD1,
            depositD2 = summary.depositD2,
            totalProfitAmount = summary.totalProfitAmount,
            totalProfitRate = summary.totalProfitRate,
            holdings = holdings.map {
                HoldingStock(
                    code = it.code,
                    name = it.name,
                    quantity = it.quantity,
                    avgPrice = it.avgPrice,
                    currentPrice = it.currentPrice,
                    evaluationAmount = it.evaluationAmount,
                    profitAmount = it.profitAmount,
                    profitRate = it.profitRate,
                )
            },
        )
    }

    suspend fun save(summary: AccountSummary) {
        dao.insertSummary(
            CachedAccountSummary(
                accountId = summary.accountId,
                totalAsset = summary.totalAsset,
                deposit = summary.deposit,
                depositD1 = summary.depositD1,
                depositD2 = summary.depositD2,
                totalProfitAmount = summary.totalProfitAmount,
                totalProfitRate = summary.totalProfitRate,
            )
        )
        dao.deleteHoldings(summary.accountId)
        dao.insertHoldings(
            summary.holdings.map {
                CachedHolding(
                    accountId = summary.accountId,
                    code = it.code,
                    name = it.name,
                    quantity = it.quantity,
                    avgPrice = it.avgPrice,
                    currentPrice = it.currentPrice,
                    evaluationAmount = it.evaluationAmount,
                    profitAmount = it.profitAmount,
                    profitRate = it.profitRate,
                )
            }
        )
    }

    suspend fun clear(accountId: Long) {
        dao.deleteSummary(accountId)
        dao.deleteHoldings(accountId)
    }
}
