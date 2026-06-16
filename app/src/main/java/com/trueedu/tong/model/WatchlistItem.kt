package com.trueedu.tong.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val code: String,
    val nameKr: String,
    val addedAt: Long = System.currentTimeMillis(),
    // 사용자 지정 순서. 작을수록 위에 노출되며, 같은 값끼리는 addedAt 최신순.
    val sortOrder: Int = 0,
)
