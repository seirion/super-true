package com.trueedu.tong.di

import android.content.Context
import androidx.room.Room
import com.trueedu.tong.db.AppDatabase
import com.trueedu.tong.db.BrokerAccountDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "tong.db")
            .build()

    @Provides
    @Singleton
    fun provideBrokerAccountDao(db: AppDatabase): BrokerAccountDao = db.brokerAccountDao()
}
