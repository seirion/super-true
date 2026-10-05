package com.trueedu.tong.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 미국 종목 검색용 로컬 캐시 (토스 GET /api/v1/stocks/all).
 * @param market NASDAQ / NYSE / AMEX
 * @param securityType STOCK / FOREIGN_STOCK / DEPOSITARY_RECEIPT / REIT / ETF / FOREIGN_ETF
 */
@Entity(tableName = "us_stocks")
data class UsStockLocal(
    @PrimaryKey
    val symbol: String,
    val nameKr: String,
    val market: String,
    val securityType: String,
)
