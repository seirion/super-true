package com.trueedu.tong.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.trueedu.tong.model.BrokerAccount

@Database(entities = [BrokerAccount::class], version = 1)
@TypeConverters(BrokerTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brokerAccountDao(): BrokerAccountDao
}
