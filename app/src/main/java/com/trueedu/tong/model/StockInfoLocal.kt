package com.trueedu.tong.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockInfoLocal(
    @PrimaryKey
    val code: String,
    val nameKr: String,
    val attributes: String,
    val kospi: Boolean,
)
