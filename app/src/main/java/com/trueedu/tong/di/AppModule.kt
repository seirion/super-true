package com.trueedu.tong.di

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.trueedu.tong.analytics.TrueAnalytics
import com.trueedu.tong.repository.local.Local
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModuleProvider {

    @Provides
    @Singleton
    fun providesContentResolver(
        @ApplicationContext context: Context
    ): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @AppVersion
    fun providesAppVersion(
        @ApplicationContext context: Context
    ): String? {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName
        } catch (e: Exception) {
            null
        }
    }

    @Provides
    @AppVersionCode
    fun providesAppVersionCode(
        @ApplicationContext context: Context
    ): Long {
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return pInfo.longVersionCode
    }

    @Provides
    @Singleton
    fun providesLocal(
        @ApplicationContext context: Context
    ): Local {
        return Local(context.getSharedPreferences("local", Context.MODE_PRIVATE))
    }

    @Provides
    @Singleton
    fun providesTrueAnalytics(
        @ApplicationContext context: Context,
    ): TrueAnalytics {
        return TrueAnalytics(context as Application)
    }
}
