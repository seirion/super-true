package com.trueedu.tong.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trueedu.tong.db.AccountSummaryCacheDao
import com.trueedu.tong.db.AppDatabase
import com.trueedu.tong.db.BrokerAccountDao
import com.trueedu.tong.db.StockInfoLocalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `stocks` (
                `code` TEXT NOT NULL,
                `nameKr` TEXT NOT NULL,
                `attributes` TEXT NOT NULL,
                `kospi` INTEGER NOT NULL,
                PRIMARY KEY(`code`)
            )
            """.trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "tong.db")
            .addMigrations(MIGRATION_3_4)
            .build()

    @Provides
    @Singleton
    fun provideBrokerAccountDao(db: AppDatabase): BrokerAccountDao = db.brokerAccountDao()

    @Provides
    @Singleton
    fun provideAccountSummaryCacheDao(db: AppDatabase): AccountSummaryCacheDao =
        db.accountSummaryCacheDao()

    @Provides
    @Singleton
    fun provideStockInfoLocalDao(db: AppDatabase): StockInfoLocalDao =
        db.stockInfoLocalDao()
}
