package com.trueedu.tong.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.CachedAccountSummary
import com.trueedu.tong.model.CachedHolding
import com.trueedu.tong.model.StockInfoLocal
import com.trueedu.tong.model.WatchlistItem

@Database(
    entities = [
        BrokerAccount::class,
        CachedAccountSummary::class,
        CachedHolding::class,
        StockInfoLocal::class,
        WatchlistItem::class,
    ],
    version = 6,
)
@TypeConverters(BrokerTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brokerAccountDao(): BrokerAccountDao
    abstract fun accountSummaryCacheDao(): AccountSummaryCacheDao
    abstract fun stockInfoLocalDao(): StockInfoLocalDao
    abstract fun watchlistDao(): WatchlistDao
}
