package com.trueedu.tong.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.trueedu.tong.model.BrokerAccount
import com.trueedu.tong.model.CachedAccountSummary
import com.trueedu.tong.model.CachedHolding

@Database(
    entities = [
        BrokerAccount::class,
        CachedAccountSummary::class,
        CachedHolding::class,
    ],
    version = 3,
)
@TypeConverters(BrokerTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brokerAccountDao(): BrokerAccountDao
    abstract fun accountSummaryCacheDao(): AccountSummaryCacheDao
}
