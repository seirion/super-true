package com.trueedu.tong.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_account_summaries")
data class CachedAccountSummary(
    @PrimaryKey
    val accountId: Long,         // BrokerAccount.id
    val totalAsset: Double,
    val deposit: Double,
    val depositD1: Double?,
    val depositD2: Double?,
    val totalProfitAmount: Double,
    val totalProfitRate: Double,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_holdings")
data class CachedHolding(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,         // FK to BrokerAccount.id
    val code: String,
    val name: String,
    val quantity: Long,
    val avgPrice: Double,
    val currentPrice: Double?,
    val evaluationAmount: Double,
    val profitAmount: Double,
    val profitRate: Double,
)
