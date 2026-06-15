package com.trueedu.tong.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val code: String,
    val nameKr: String,
    val addedAt: Long = System.currentTimeMillis(),
)
