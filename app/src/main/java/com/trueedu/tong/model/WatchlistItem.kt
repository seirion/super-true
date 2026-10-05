package com.trueedu.tong.model

import androidx.room.Entity

/**
 * 관심종목. 국내 코드와 미국 티커가 겹칠 수 있어 (code, market) 을 기본키로 한다.
 * @param market [MARKET_KR] 국내, [MARKET_US] 미국
 */
@Entity(tableName = "watchlist", primaryKeys = ["code", "market"])
data class WatchlistItem(
    val code: String,
    val nameKr: String,
    val addedAt: Long = System.currentTimeMillis(),
    // 사용자 지정 순서. 작을수록 위에 노출되며, 같은 값끼리는 addedAt 최신순.
    val sortOrder: Int = 0,
    val market: String = MARKET_KR,
) {
    companion object {
        const val MARKET_KR = "KR"
        const val MARKET_US = "US"
    }
}
